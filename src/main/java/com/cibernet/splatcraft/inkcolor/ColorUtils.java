package com.cibernet.splatcraft.inkcolor;

import com.cibernet.splatcraft.Splatcraft;
import com.cibernet.splatcraft.block.AbstractInkableBlock;
import com.cibernet.splatcraft.block.entity.AbstractInkableBlockEntity;
import com.cibernet.splatcraft.component.PlayerDataComponent;
import com.cibernet.splatcraft.component.SplatcraftComponents;
import com.cibernet.splatcraft.entity.InkableEntity;
import com.cibernet.splatcraft.network.SplatcraftNetworkingConstants;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public class ColorUtils {
    public static final int DEFAULT = 0x00FFFFFF;
    public static final int DEFAULT_COLOR_LOCK_FRIENDLY = 0xDEA801;
    public static final int DEFAULT_COLOR_LOCK_HOSTILE = 0x4717A9;

    public static final InkColor[] STARTER_COLORS = { InkColors.ORANGE, InkColors.BLUE, InkColors.GREEN, InkColors.PINK };

    public static InkColor getEntityColor(Entity entity) {
        if (entity instanceof PlayerEntity) {
            return getInkColor((PlayerEntity) entity);
        } else if (entity instanceof InkableEntity) {
            return ((InkableEntity) entity).getInkColor();
        } else {
            return InkColors.NONE;
        }
    }

    public static InkColor getInkColor(PlayerEntity player) {
        try {
            return PlayerDataComponent.getInkColor(player);
        } catch (NullPointerException e) {
            return InkColors.NONE;
        }
    }
    public static boolean setInkColor(PlayerEntity player, InkColor color) {
        PlayerDataComponent data = SplatcraftComponents.PLAYER_DATA.get(player);
        if (!data.getInkColor().equals(color)) {
            data.setInkColor(color);

            if (!player.world.isClient) {
                ServerPlayNetworking.send((ServerPlayerEntity) player, SplatcraftNetworkingConstants.SYNC_INK_COLOR_CHANGE_FOR_COLOR_LOCK_PACKET_ID, PacketByteBufs.empty());
            }

            return true;
        } else {
            return false;
        }
    }

    public static InkColor getInkColor(Entity entity) {
        if (entity instanceof InkableEntity) {
            return ((InkableEntity) entity).getInkColor();
        } else {
            return InkColors.NONE;
        }
    }
    public static boolean setInkColor(Entity entity, InkColor color) {
        if (entity instanceof InkableEntity) {
            return ((InkableEntity) entity).setInkColor(color);
        } else if (entity instanceof PlayerEntity) {
            return ColorUtils.setInkColor((PlayerEntity) entity, color);
        }

        return false;
    }

    public static InkColor getInkColor(ItemStack stack) {
        CompoundTag tag = ColorUtils.getBlockEntityTagOrRoot(stack);
        if (tag == null) {
            return InkColors.NONE;
        } else {
            CompoundTag splatcraft = tag.getCompound(Splatcraft.MOD_ID);
            if (splatcraft == null) {
                return InkColors.NONE;
            } else {
                String inkColor = splatcraft.getString("InkColor");
                return inkColor == null ? InkColors.NONE : InkColor.getFromId(inkColor);
            }
        }
    }
    public static ItemStack setInkColor(ItemStack stack, InkColor color, boolean setColorLocked) {
        CompoundTag tag = ColorUtils.getBlockEntityTagOrRoot(stack);
        CompoundTag splatcraft = ColorUtils.getOrCreateSplatcraftTag(tag);
        splatcraft.putString("InkColor", color.toString());
        tag.put(Splatcraft.MOD_ID, splatcraft);

        if (setColorLocked) {
            ColorUtils.setColorLocked(stack, true);
        }

        return stack;
    }
    public static ItemStack setInkColor(ItemStack stack, InkColor color) {
        return ColorUtils.setInkColor(stack, color, false);
    }

    public static InkColor getInkColor(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return InkColors.NONE;
        } else if (blockEntity instanceof AbstractInkableBlockEntity) {
            return ((AbstractInkableBlockEntity) blockEntity).getInkColor();
        }

        Block block = blockEntity.getCachedState().getBlock();
        if (block instanceof AbstractInkableBlock) {
            return ((AbstractInkableBlock) block).getColor(Objects.requireNonNull(blockEntity.getWorld()), blockEntity.getPos());
        }

        return InkColors.NONE;
    }
    public static boolean setInkColor(BlockEntity blockEntity, InkColor color) {
        if (blockEntity instanceof AbstractInkableBlockEntity) {
            return ((AbstractInkableBlockEntity) blockEntity).setInkColor(color);
        }

        return false;
    }

    public static float[] getColorsFromInt(int color) {
        float r = ((color & 16711680) >> 16) / 255.0f;
        float g = ((color & '\uff00') >> 8) / 255.0f;
        float b = (color & 255) / 255.0f;

        return new float[]{ r, g, b };
    }

    public static Text getFormattedColorName(InkColor inkColor, boolean colorless) {
        TranslatableText text = new TranslatableText(inkColor.getTranslationKey());
        if (!colorless) {
            text.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(inkColor.color)));
        }

        return text;
    }
    public static TranslatableText getTranslatableTextWithColor(String key, InkColor color, boolean colorless) {
        return new TranslatableText(key, getFormattedColorName(color, colorless));
    }
    public static TranslatableText getTranslatableTextWithColor(ItemStack stack, boolean colorless) {
        return getTranslatableTextWithColor(((TranslatableText)stack.getItem().getName()).getKey(), ColorUtils.getInkColor(stack), colorless);
    }

    public static void appendTooltip(ItemStack stack, List<Text> tooltip) {
        InkColor inkColor = ColorUtils.getInkColor(stack);
        if (!inkColor.equals(InkColors.NONE) || ColorUtils.isColorLocked(stack)) {
            tooltip.add(ColorUtils.getFormattedColorName(ColorUtils.getInkColor(stack), false));
        } else {
            tooltip.add(new TranslatableText("item." + Splatcraft.MOD_ID + ".tooltip.colorless").formatted(Formatting.GRAY));
        }
    }

    public static ItemStack setColorLocked(ItemStack stack, boolean isLocked) {
        CompoundTag tag = ColorUtils.getBlockEntityTagOrRoot(stack);
        CompoundTag splatcraft = ColorUtils.getOrCreateSplatcraftTag(tag);
        splatcraft.putBoolean("ColorLocked", isLocked);
        tag.put(Splatcraft.MOD_ID, splatcraft);

        return stack;
    }
    public static boolean isColorLocked(ItemStack stack) {
        CompoundTag tag = ColorUtils.getBlockEntityTagOrRoot(stack);
        CompoundTag splatcraft = ColorUtils.getOrCreateSplatcraftTag(tag);
        return splatcraft.getBoolean("ColorLocked");
    }

    public static CompoundTag getBlockEntityTagOrRoot(ItemStack stack) {
        return stack.getItem() instanceof BlockItem ? stack.getOrCreateSubTag("BlockEntityTag") : stack.getOrCreateTag();
    }

    public static boolean colorEquals(PlayerEntity player, ItemStack stack) {
        return ColorUtils.getInkColor(player).matches(ColorUtils.getInkColor(stack).color);
    }

    public static void addInkSplashParticle(World world, InkColor inkColor, Vec3d pos, float scale) {
        if (!world.isClient) {
            PacketByteBuf buf = PacketByteBufs.create();

            buf.writeIdentifier(inkColor.id);
            buf.writeFloat(scale);

            // write spawn pos
            buf.writeDouble(pos.getX());
            buf.writeDouble(pos.getY());
            buf.writeDouble(pos.getZ());

            for (ServerPlayerEntity player : PlayerLookup.tracking((ServerWorld) world, new BlockPos(pos))) {
                ServerPlayNetworking.send(player, SplatcraftNetworkingConstants.PLAY_BLOCK_INKING_EFFECTS_PACKET_ID, buf);
            }
        }
    }
    public static void addInkSplashParticle(World world, InkColor inkColor, Vec3d pos) {
        ColorUtils.addInkSplashParticle(world, inkColor, pos, 1.0f);
    }
    public static void addInkSplashParticle(World world, BlockPos sourcePos, Vec3d spawnPos, float scale) {
        BlockEntity blockEntity = world.getBlockEntity(sourcePos);
        if (blockEntity instanceof AbstractInkableBlockEntity) {
            ColorUtils.addInkSplashParticle(world, ((AbstractInkableBlockEntity) blockEntity).getInkColor(), spawnPos, scale);
        }
    }
    public static void addInkSplashParticle(World world, BlockPos sourcePos, Vec3d spawnPos) {
        ColorUtils.addInkSplashParticle(world, sourcePos, spawnPos, 1.0f);
    }

    public static void playSquidTravelEffects(Entity entity, InkColor inkColor, float scale) {
        if (!entity.world.isClient) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeUuid(entity.getUuid());

            buf.writeDouble(entity.getX());
            buf.writeDouble(entity.getY());
            buf.writeDouble(entity.getZ());

            buf.writeIdentifier(inkColor.id);
            buf.writeFloat(scale);

            for (ServerPlayerEntity serverPlayer : PlayerLookup.tracking((ServerWorld) entity.world, entity.getBlockPos())) {
                ServerPlayNetworking.send(serverPlayer, SplatcraftNetworkingConstants.PLAY_SQUID_TRAVEL_EFFECTS_PACKET_ID, buf);
            }
        }
    }

    public static InkColor getRandomStarterColor(Random random) {
        return STARTER_COLORS[random.nextInt(STARTER_COLORS.length)];
    }

    public static CompoundTag getOrCreateSplatcraftTag(CompoundTag tag) {
        if (tag != null) {
            CompoundTag splatcraft = tag.getCompound(Splatcraft.MOD_ID);
            if (splatcraft != null) {
                return splatcraft;
            }
        }

        return new CompoundTag();
    }
}
