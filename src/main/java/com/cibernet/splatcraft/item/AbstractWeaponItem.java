package com.cibernet.splatcraft.item;

import com.cibernet.splatcraft.Splatcraft;
import com.cibernet.splatcraft.block.entity.AbstractInkableBlockEntity;
import com.cibernet.splatcraft.component.PlayerDataComponent;
import com.cibernet.splatcraft.handler.PlayerPoseHandler;
import com.cibernet.splatcraft.init.*;
import com.cibernet.splatcraft.inkcolor.ColorUtils;
import com.cibernet.splatcraft.inkcolor.InkColor;
import com.cibernet.splatcraft.inkcolor.InkColors;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import java.util.List;

public abstract class AbstractWeaponItem extends Item implements MatchItem, EntityTickable, AttackInputDetectable {
    public float inkConsumption;

    public AbstractWeaponItem(float inkConsumption, Item.Settings settings) {
        super(settings);
        this.inkConsumption = inkConsumption;
    }

    @Override
    public void appendTooltip(ItemStack stack, World player, List<Text> tooltip, TooltipContext advanced) {
        super.appendTooltip(stack, player, tooltip, advanced);

        InkColor inkColor = ColorUtils.getInkColor(stack);
        if (inkColor != InkColors.NONE || ColorUtils.isColorLocked(stack)) {
            tooltip.add(ColorUtils.getFormattedColorName(ColorUtils.getInkColor(stack), true));
        } else {
            tooltip.add(new TranslatableText(Util.createTranslationKey("item", new Identifier(Splatcraft.MOD_ID, "ink_cloth_armor")) + ".tooltip.colorless"));
        }
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        return ActionResult.PASS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        user.setCurrentHand(hand);
        return TypedActionResult.success(stack, false);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (ColorUtils.isColorLocked(stack) || !(entity instanceof PlayerEntity)) {
            return;
        } else {
            PlayerDataComponent data = SplatcraftComponents.PLAYER_DATA.get(entity);
            ColorUtils.setInkColor(stack, data.getInkColor());
        }

        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (user.getMainHandStack().equals(stack)) {
            user.clearActiveItem();
        }
        super.onStoppedUsing(stack, world, user, remainingUseTicks);
    }

    @Override
    public void entityTick(ItemStack stack, ItemEntity entity) {
        BlockPos floorPos = new BlockPos(entity.getPos().subtract(0.0D, -1.0D, 0.0D));

        if (entity.world.getBlockState(floorPos).getBlock().equals(SplatcraftBlocks.INKWELL)) {
            BlockEntity blockEntity = entity.world.getBlockEntity(floorPos);
            if (blockEntity instanceof AbstractInkableBlockEntity) {
                AbstractInkableBlockEntity inkableBlockEntity = (AbstractInkableBlockEntity) blockEntity;
                if (ColorUtils.getInkColor(stack) != inkableBlockEntity.getInkColor() || !ColorUtils.isColorLocked(stack)) {
                    ColorUtils.setInkColor(stack, inkableBlockEntity.getInkColor());
                    ColorUtils.setColorLocked(stack, true);
                }
            }
        } else if (entity.world.getBlockState(floorPos.up()).getMaterial().equals(Material.WATER) && ColorUtils.isColorLocked(stack)) {
            ColorUtils.setInkColor(stack, InkColors.NONE);
            ColorUtils.setColorLocked(stack, false);
        }
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 72000;
    }

    public static float getInkAmount(LivingEntity player, ItemStack weapon) {
        if (!SplatcraftGameRules.getBoolean(player.world, SplatcraftGameRules.REQUIRE_INK_TANK)) {
            return Float.MAX_VALUE;
        }

        ItemStack tank = player.getEquippedStack(EquipmentSlot.CHEST);
        if (!(tank.getItem() instanceof InkTankArmorItem)) {
            return 0;
        }

        return InkTankArmorItem.getInkAmount(tank, weapon);
    }

    public static boolean hasInk(LivingEntity player, ItemStack weapon) {
        return getInkAmount(player, weapon) > ((AbstractWeaponItem) weapon.getItem()).inkConsumption;
    }

    public static void reduceInk(LivingEntity player, float amount) {
        ItemStack tank = player.getEquippedStack(EquipmentSlot.CHEST);
        if (!SplatcraftGameRules.getBoolean(player.world, SplatcraftGameRules.REQUIRE_INK_TANK) || !(tank.getItem() instanceof InkTankArmorItem)) {
            return;
        }

        InkTankArmorItem.setInkAmount(tank, InkTankArmorItem.getInkAmount(tank) - amount);
    }
    public void reduceInk(PlayerEntity player) {
        reduceInk(player, inkConsumption);
    }

    public static void sendNoInkMessage(LivingEntity entity) {
        sendNoInkMessage(entity, SplatcraftSoundEvents.NO_INK);
    }
    public static void sendNoInkMessage(LivingEntity entity, SoundEvent sound) {
        if (entity instanceof PlayerEntity) {
            ((PlayerEntity) entity).sendMessage(new TranslatableText(Util.createTranslationKey("item", Registry.ITEM.getId(SplatcraftItems.INK_TANK)) + ".noInk").formatted(Formatting.RED), true);
            if (sound != null) {
                entity.world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundCategory.PLAYERS, 0.8F, ((entity.world.random.nextFloat() - entity.world.random.nextFloat()) * 0.1F + 1.0F) * 0.95F);
            }
        }

    }

    public boolean doesNotAffectMovementWhenUsed() {
        return false;
    }

    public PlayerPoseHandler.WeaponPose getPose() {
        return PlayerPoseHandler.WeaponPose.NONE;
    }
}