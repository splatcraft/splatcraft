package com.cibernet.splatcraft.item.weapon;

import com.cibernet.splatcraft.entity.InkProjectileEntity;
import com.cibernet.splatcraft.handler.PlayerPoseHandler;
import com.cibernet.splatcraft.inkcolor.ColorUtil;
import com.cibernet.splatcraft.inkcolor.InkColor;
import com.cibernet.splatcraft.inkcolor.InkDamage;
import com.cibernet.splatcraft.inkcolor.InkType;
import com.cibernet.splatcraft.item.AttackInputDetectable;
import com.cibernet.splatcraft.item.weapon.component.RollerComponent;
import com.cibernet.splatcraft.util.InkBlockUtil;
import com.google.common.collect.ImmutableList;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

import java.util.Random;

public class RollerItem extends AbstractWeaponItem implements AttackInputDetectable {
    public final RollerComponent component;

    public RollerItem(Item.Settings settings, float mobility, RollerComponent component) {
        super(settings, mobility);
        this.component = component;
    }

    public RollerItem(RollerItem parent) {
        this(parent.settings, parent.mobility, parent.component.copy());
    }
    public RollerItem(RollerItem parent, RollerComponent component) {
        this(parent.settings, parent.mobility, component);
    }

    @Override
    protected ImmutableList<WeaponStat> createWeaponStats() {
        return ImmutableList.of(
            new WeaponStat("range", (int) ((this.component.fling.speed /*+ this.component.swing.speed*/) * 50)),
            new WeaponStat("ink_speed", (int) (/*this.component.dash.speed /*/ 2f * 100)),
            new WeaponStat("handling", (int) ((20/* - (this.component.fling.time + this.component.swing.time)*/ / 2f) * 5))
        );
    }

    @Override
    public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner) {
        return !miner.isCreative();
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if (!player.isSneaking()) {
            player.getItemCooldownManager().getCooldownProgress(this, 0);
        }

        return super.use(world, player, hand);
    }

    @Override
    public float getInkConsumption(float fling) {
        return fling == 0 ? this.component.consumption : this.component.fling.consumption;
    }

    public boolean hasInk(PlayerEntity player, ItemStack weapon, boolean fling) {
        return this.hasInk(player, weapon, fling ? 1 : 0);
    }

    public void reduceInk(PlayerEntity player, boolean fling) {
        this.reduceInk(player, fling ? 1 : 0);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (user instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) user;
            InkType isGlowing = InkType.from(player);

            if (this.hasInk(player, stack, false)) {
                InkColor color = ColorUtil.getInkColor(stack);
                int downReach = player.getY() % 1 < 0.5 ? 1 : 0;
                Vec3d fwd = getFwd(0, player.yaw).normalize();
                fwd = new Vec3d(Math.round(fwd.x), Math.round(fwd.y), Math.round(fwd.z));

                BlockPos pos = new BlockPos(Math.floor(player.getX()) + 0.5, player.getY() - downReach, Math.floor(player.getZ()) + 0.5);

                for (int i = 0; i < this.component.radius; i++) {
                    for (int rollDepth = 0; rollDepth < 2; rollDepth++) {
                        double xOff;
                        double zOff = i == 0 ? 0 : Math.round(fwd.x) * Math.ceil(i / 2.0);

                        if (i % 2 == 0) {
                            zOff *= -1;
                        }

                        Direction horizontalFacing = player.getHorizontalFacing();
                        boolean horizontalFacingIsSouth = horizontalFacing.equals(Direction.SOUTH);
                        if (horizontalFacing.equals(Direction.NORTH) || horizontalFacingIsSouth) {
                            zOff = rollDepth - (horizontalFacingIsSouth ? horizontalFacing.getDirection().offset() : 0);
                            xOff = i * (horizontalFacing.equals(Direction.SOUTH) ? -1 : 1);
                        } else {
                            boolean horizontalFacingIsEast = horizontalFacing.equals(Direction.EAST);
                            xOff = rollDepth + (horizontalFacingIsEast ? -1 : 0);
                        }

                        BlockPos inkPos = pos.add(fwd.x * 2 + xOff, -1, fwd.z * 2 + zOff);

                        int h = 0;
                        for (; h <= downReach; h++) {
                            if (InkBlockUtil.canInkPassthrough(world, inkPos.up())) {
                                break;
                            } else {
                                inkPos = inkPos.up();
                            }
                        }

                        if (InkBlockUtil.inkBlockAsPlayer(player, world, inkPos, color, isGlowing)) {
                            Random random = world.random;
                            double min = -0.5d;
                            double max = 0.5d;

                            for (int pCount = 0; pCount < 4; pCount++) {
                                double x = inkPos.getX() + 0.5d + (min + random.nextDouble() * (max - min));
                                double y = inkPos.getY() + 1.0d + (0.0d + random.nextDouble() * (0.02d - 0.0d));
                                double z = inkPos.getZ() + 0.5d + (min + random.nextDouble() * (max - min));
                                ColorUtil.addInkSplashParticle(world, color, new Vec3d(x, y, z));
                            }
                            this.reduceInk(player, false);

                            if (player.getVelocity().getX() != 0 || player.getVelocity().getZ() != 0) {
                                player.setSprinting(true);
                            }
                        }

                        for (LivingEntity target : world.getEntitiesIncludingUngeneratedChunks(LivingEntity.class, new Box(inkPos.up()))) {
                            if (!target.equals(player)) {
                                if (InkDamage.roll(world, target, this.component.damage, color, player, false)) {
                                    this.reduceInk(player, false);
                                }
                            }
                        }

                        if (h > downReach) {
                            break;
                        }
                    }
                }
            } else {
                sendNoInkMessage(player);
            }
        }
    }

    @Override
    public boolean onAttack(ServerPlayerEntity player, ItemStack stack) {
        if (this.component.fling != null) {
            if (this.hasInk(player, stack, true)) {
                this.reduceInk(player, true);

                InkProjectileEntity proj = new InkProjectileEntity(player.world, player, stack, InkType.from(player), this.component.fling.size, this.component.fling.damage);
                proj.setProperties(player, player.pitch, player.yaw, 0.0f, 1.5f, 1.0f);
                player.world.spawnEntity(proj);
            } else {
                sendNoInkMessage(player);
            }
        }

        return false;
    }

    private Vec3d getFwd(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * 0.017453292f - (float)Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292f - (float)Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292f);
        float f3 = MathHelper.sin(-pitch * 0.017453292f);
        return new Vec3d(f1 * f2, f3, f * f2);
    }

    @Override
    public PlayerPoseHandler.WeaponPose getPose() {
        return PlayerPoseHandler.WeaponPose.ROLL;
    }
}
