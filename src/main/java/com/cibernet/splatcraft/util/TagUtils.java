package com.cibernet.splatcraft.util;

import com.cibernet.splatcraft.Splatcraft;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtHelper;

public class TagUtils {
    public static CompoundTag getBlockEntityTagOrRoot(ItemStack stack) {
        return stack.getItem() instanceof BlockItem ? stack.getOrCreateSubTag("BlockEntityTag") : stack.getOrCreateTag();
    }
    public static CompoundTag getBlockEntityTagOrRoot(CompoundTag tag) {
        return tag.contains("BlockEntityTag") ? tag.getCompound("BlockEntityTag") : tag;
    }

    public static CompoundTag getOrCreateSplatcraftTag(CompoundTag tag) {
        if (tag != null) {
            CompoundTag root = TagUtils.getBlockEntityTagOrRoot(tag);
            CompoundTag splatcraft = root.getCompound(Splatcraft.MOD_ID);
            if (splatcraft != null) {
                return splatcraft;
            }
        }

        return new CompoundTag();
    }
    public static CompoundTag getOrCreateSplatcraftTag(ItemStack stack) {
        return TagUtils.getOrCreateSplatcraftTag(stack.getTag());
    }

    public static BlockState getBlockStateFromInkedBlockItem(CompoundTag tag) {
        return NbtHelper.toBlockState(TagUtils.getOrCreateSplatcraftTag(tag).getCompound("SavedState"));
    }
    public static BlockState getBlockStateFromInkedBlockItem(ItemStack stack) {
        return TagUtils.getBlockStateFromInkedBlockItem(stack.getTag());
    }
}
