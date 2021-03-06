package com.cibernet.splatcraft.items.weapons;

import com.cibernet.splatcraft.SplatcraftConfig;
import com.cibernet.splatcraft.blocks.InkedBlock;
import com.cibernet.splatcraft.blocks.InkwellBlock;
import com.cibernet.splatcraft.data.capabilities.playerinfo.PlayerInfoCapability;
import com.cibernet.splatcraft.handlers.PlayerPosingHandler;
import com.cibernet.splatcraft.items.InkTankItem;
import com.cibernet.splatcraft.registries.SplatcraftGameRules;
import com.cibernet.splatcraft.registries.SplatcraftItemGroups;
import com.cibernet.splatcraft.registries.SplatcraftItems;
import com.cibernet.splatcraft.registries.SplatcraftSounds;
import com.cibernet.splatcraft.tileentities.InkColorTileEntity;
import com.cibernet.splatcraft.util.ClientUtils;
import com.cibernet.splatcraft.util.ColorUtils;
import com.cibernet.splatcraft.util.PlayerCooldown;
import com.cibernet.splatcraft.util.WeaponStat;
import net.minecraft.block.BlockState;
import net.minecraft.block.CauldronBlock;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.stats.Stats;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.*;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class WeaponBaseItem extends Item
{
    public static final int USE_DURATION = 72000;
    protected final List<WeaponStat> stats = new ArrayList<>();
    protected boolean secret = false;

    public WeaponBaseItem()
    {
        super(new Properties().maxStackSize(1).group(SplatcraftItemGroups.GROUP_WEAPONS));
        SplatcraftItems.inkColoredItems.add(this);
        SplatcraftItems.weapons.add(this);
    }

    public static float getInkAmount(LivingEntity player, ItemStack weapon)
    {
        if (!SplatcraftGameRules.getBooleanRuleValue(player.world, SplatcraftGameRules.REQUIRE_INK_TANK))
        {
            return Float.MAX_VALUE;
        }

        ItemStack tank = player.getItemStackFromSlot(EquipmentSlotType.CHEST);
        if (!(tank.getItem() instanceof InkTankItem))
        {
            return 0;
        }

        return InkTankItem.getInkAmount(tank, weapon);
    }

    public static boolean hasInk(LivingEntity player, ItemStack weapon)
    {
        return getInkAmount(player, weapon) > 0;
    }

    public static void reduceInk(LivingEntity player, float amount)
    {
        ItemStack tank = player.getItemStackFromSlot(EquipmentSlotType.CHEST);
        if (!SplatcraftGameRules.getBooleanRuleValue(player.world, SplatcraftGameRules.REQUIRE_INK_TANK))
        {
            return;
        }
        if (!(tank.getItem() instanceof InkTankItem))
        {
            return;
        }

        InkTankItem.setInkAmount(tank, InkTankItem.getInkAmount(tank) - amount);
    }

    public static void sendNoInkMessage(LivingEntity entity)
    {
        sendNoInkMessage(entity, SplatcraftSounds.noInkMain);
    }

    public static void sendNoInkMessage(LivingEntity entity, SoundEvent sound)
    {
        if (entity instanceof PlayerEntity)
        {
            ((PlayerEntity) entity).sendStatusMessage(new TranslationTextComponent("status.no_ink").mergeStyle(TextFormatting.RED), true);
            if (sound != null)
            {
                entity.world.playSound(null, entity.getPosX(), entity.getPosY(), entity.getPosZ(), sound, SoundCategory.PLAYERS, 0.8F,
                        ((entity.world.rand.nextFloat() - entity.world.rand.nextFloat()) * 0.1F + 1.0F) * 0.95F);
            }
        }

    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag)
    {
        super.addInformation(stack, world, tooltip, flag);

        if (ColorUtils.isColorLocked(stack))
        {
            tooltip.add(ColorUtils.getFormatedColorName(ColorUtils.getInkColor(stack), true));
        } else
        {
            tooltip.add(new StringTextComponent(""));
        }

        for (WeaponStat stat : stats)
        {
            tooltip.add(stat.getTextComponent(stack, world).setStyle(Style.EMPTY.setFormatting(TextFormatting.DARK_GREEN)));
        }
    }

    public void addStat(WeaponStat stat)
    {
        stats.add(stat);
    }

    @Override
    public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> list)
    {
        if (!secret)
        {
            super.fillItemGroup(group, list);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected)
    {
        super.inventoryTick(stack, world, entity, itemSlot, isSelected);

        if (entity instanceof PlayerEntity && !ColorUtils.isColorLocked(stack) && ColorUtils.getInkColor(stack) != ColorUtils.getPlayerColor((PlayerEntity) entity)
                && PlayerInfoCapability.hasCapability((LivingEntity) entity))
            ColorUtils.setInkColor(stack, ColorUtils.getPlayerColor((PlayerEntity) entity));
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity)
    {
        BlockPos pos = entity.getPosition().down();

        if (entity.world.getBlockState(pos).getBlock() instanceof InkwellBlock)
        {
            InkColorTileEntity te = (InkColorTileEntity) entity.world.getTileEntity(pos);

            if (ColorUtils.getInkColor(stack) != ColorUtils.getInkColor(te))
            {
                ColorUtils.setInkColor(entity.getItem(), ColorUtils.getInkColor(te));
                ColorUtils.setColorLocked(entity.getItem(), true);
            }
        }
        else if(InkedBlock.causesClear(entity.world.getBlockState(pos)) && ColorUtils.isColorLocked(stack))
        {
            ColorUtils.setInkColor(stack, 0xFFFFFF);
            ColorUtils.setColorLocked(stack, false);
        }

        return false;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack)
    {
        try
        {
            return ClientUtils.getDurabilityForDisplay(stack);
        } catch (NoClassDefFoundError e)
        {
            return 1;
        }
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack)
    {
        return !SplatcraftConfig.Client.vanillaInkDurability.get() ? ColorUtils.getInkColor(stack) : super.getRGBDurabilityForDisplay(stack);
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack)
    {
        try
        {
            return ClientUtils.showDurabilityBar(stack);
        } catch (NoClassDefFoundError e)
        {
            return false;
        }
    }

    @Override
    public int getUseDuration(ItemStack stack)
    {
        return USE_DURATION;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand)
    {
        if(!(player.isActualySwimming() && !player.isInWater()))
            player.setActiveHand(hand);
        return super.onItemRightClick(world, player, hand);
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext context)
    {
        BlockState state = context.getWorld().getBlockState(context.getPos());
        if (ColorUtils.isColorLocked(context.getItem()) && state.getBlock() instanceof CauldronBlock && context.getPlayer() != null && !context.getPlayer().isSneaking())
        {
            int i = state.get(CauldronBlock.LEVEL);

            if (i > 0)
            {
                World world = context.getWorld();
                PlayerEntity player = context.getPlayer();
                ColorUtils.setColorLocked(context.getItem(), false);

                context.getPlayer().addStat(Stats.USE_CAULDRON);

                if (!player.abilities.isCreativeMode)
                {world.setBlockState(context.getPos(), state.with(CauldronBlock.LEVEL, MathHelper.clamp(i - 1, 0, 3)), 2);
                    world.updateComparatorOutputLevel(context.getPos(), state.getBlock());
                }

                return ActionResultType.SUCCESS;
            }

        }

        return super.onItemUse(context);
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, LivingEntity entity, int timeLeft)
    {
        entity.resetActiveHand();
        super.onPlayerStoppedUsing(stack, world, entity, timeLeft);
    }

    public void weaponUseTick(World world, LivingEntity entity, ItemStack stack, int timeLeft)
    {

    }

    public void onPlayerCooldownEnd(World world, PlayerEntity player, ItemStack stack, PlayerCooldown cooldown)
    {

    }

    public boolean hasSpeedModifier(LivingEntity entity, ItemStack stack)
    {
        return getSpeedModifier(entity, stack) != null;
    }

    public AttributeModifier getSpeedModifier(LivingEntity entity, ItemStack stack)
    {
        return null;
    }

    public PlayerPosingHandler.WeaponPose getPose()
    {
        return PlayerPosingHandler.WeaponPose.NONE;
    }
}
