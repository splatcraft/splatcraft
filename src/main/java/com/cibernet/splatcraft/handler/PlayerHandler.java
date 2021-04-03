package com.cibernet.splatcraft.handler;

import com.cibernet.splatcraft.block.InkwellBlock;
import com.cibernet.splatcraft.block.entity.AbstractInkableBlockEntity;
import com.cibernet.splatcraft.component.LazyPlayerDataComponent;
import com.cibernet.splatcraft.component.PlayerDataComponent;
import com.cibernet.splatcraft.entity.damage.SplatcraftDamageSources;
import com.cibernet.splatcraft.init.SplatcraftAttributes;
import com.cibernet.splatcraft.init.SplatcraftGameRules;
import com.cibernet.splatcraft.init.SplatcraftStats;
import com.cibernet.splatcraft.inkcolor.ColorUtils;
import com.cibernet.splatcraft.inkcolor.InkBlockUtils;
import com.cibernet.splatcraft.network.SplatcraftNetworkingConstants;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;

public class PlayerHandler {
    public static final EntityDimensions SQUID_FORM_DIMENSIONS = EntityDimensions.fixed(0.5f, 0.5f);
    public static final EntityDimensions SQUID_FORM_AIRBORNE_DIMENSIONS = EntityDimensions.fixed(0.5f, 1.0f);

    protected static final double MOVING_THRESHOLD = 0.035d;

    public static void onPlayerTick(PlayerEntity player) {
        PlayerDataComponent data = PlayerDataComponent.getComponent(player);
        LazyPlayerDataComponent lazyData = LazyPlayerDataComponent.getComponent(player);

        Vec3d vel = player.getVelocity();
        data.setMoving(Math.abs(vel.getX()) >= MOVING_THRESHOLD || Math.abs(vel.getZ()) >= MOVING_THRESHOLD);

        if (player.abilities.flying && SplatcraftGameRules.getBoolean(player.world, SplatcraftGameRules.FLYING_DISABLES_SQUID_FORM) && lazyData.isSquid()) {
            lazyData.setIsSquid(false);
            return;
        }

        if (SplatcraftGameRules.getBoolean(player.world, SplatcraftGameRules.WATER_DAMAGE) && player.isTouchingWater() && player.age % 10 == 0) {
            player.damage(SplatcraftDamageSources.WATER, 8.0f);
        }

        boolean wasSubmerged = lazyData.isSubmerged();
        boolean shouldBeSubmerged = lazyData.isSquid() ? InkBlockUtils.shouldBeSubmerged(player) : PlayerHandler.shouldBeInvisible(player);
        if (shouldBeSubmerged != wasSubmerged) {
            if (!player.world.isClient) {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeUuid(player.getUuid());
                buf.writeBoolean(shouldBeSubmerged);
                buf.writeIdentifier(ColorUtils.getInkColor(player.world.getBlockEntity(shouldBeSubmerged ? InkBlockUtils.getVelocityAffectingPos(player) : InkBlockUtils.getVelocityAffectingPos(player).down())).id);

                for (ServerPlayerEntity serverPlayer : PlayerLookup.tracking((ServerWorld) player.world, player.getBlockPos())) {
                    ServerPlayNetworking.send(serverPlayer, SplatcraftNetworkingConstants.PLAY_PLAYER_TOGGLE_SQUID_EFFECTS_PACKET_ID, buf);
                }
            }

            lazyData.setSubmerged(shouldBeSubmerged);
            player.setInvisible(shouldBeSubmerged);
        }

        if (lazyData.isSquid()) {
            if (player.isOnGround()) {
                player.setPose(EntityPose.FALL_FLYING);
            }

            player.stopUsingItem();
            player.setSneaking(false);
            player.setSwimming(false);
            player.setSprinting(false);
            player.incrementStat(SplatcraftStats.SQUID_TIME);

            if (InkBlockUtils.takeDamage(player) && player.age % 20 == 0 && player.world.getDifficulty() != Difficulty.PEACEFUL) {
                player.damage(SplatcraftDamageSources.ENEMY_INK, 2.0f);
            } else if (InkBlockUtils.canSwim(player)) {
                player.fallDistance = 0;
                if (player.age % 5 == 0 && !player.hasStatusEffect(StatusEffects.POISON) && !player.hasStatusEffect(StatusEffects.WITHER)) {
                    player.heal(0.5f);
                }
            }

            if (SplatcraftGameRules.getBoolean(player.world, SplatcraftGameRules.INKWELL_CHANGES_PLAYER_INK_COLOR) && player.world.getBlockState(player.getBlockPos().down()).getBlock() instanceof InkwellBlock) {
                AbstractInkableBlockEntity blockEntity = (AbstractInkableBlockEntity) player.world.getBlockEntity(player.getBlockPos().down());
                if (blockEntity != null) {
                    ColorUtils.setInkColor(player, blockEntity.getInkColor());
                }
            }
        }
    }

    protected static boolean shouldBeInvisible(PlayerEntity player) {
        return player.hasStatusEffect(StatusEffects.INVISIBILITY);
    }

    public static boolean canEnterSquidForm(PlayerEntity player) {
        return !player.hasVehicle();
    }

    public static boolean shouldCancelInteraction(PlayerEntity player) {
        return LazyPlayerDataComponent.isSquid(player);
    }
    public static ActionResult getEventActionResult(PlayerEntity player) {
        if (LazyPlayerDataComponent.isSquid(player)) {
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }
    public static <T> TypedActionResult<T> getEventActionResult(PlayerEntity player, T data) {
        if (LazyPlayerDataComponent.isSquid(player)) {
            return TypedActionResult.fail(data);
        }

        return TypedActionResult.pass(data);
    }

    public static float getMovementSpeed(PlayerEntity player, float movementSpeed) {
        return !InkBlockUtils.canSwim(player) ? -1.0f : (float) (movementSpeed * (1.0f + player.getAttributeValue(SplatcraftAttributes.INK_SWIM_SPEED) * 100));
    }
}
