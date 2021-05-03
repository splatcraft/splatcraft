package com.cibernet.splatcraft.client.network;

import com.cibernet.splatcraft.Splatcraft;
import com.cibernet.splatcraft.client.signal.Signal;
import com.cibernet.splatcraft.client.signal.SignalRegistryManager;
import com.cibernet.splatcraft.client.signal.SignalRendererManager;
import com.cibernet.splatcraft.component.LazyPlayerDataComponent;
import com.cibernet.splatcraft.entity.InkableEntity;
import com.cibernet.splatcraft.inkcolor.ColorUtil;
import com.cibernet.splatcraft.inkcolor.InkColor;
import com.cibernet.splatcraft.inkcolor.InkColorSynchroniser;
import com.cibernet.splatcraft.inkcolor.InkColors;
import com.cibernet.splatcraft.network.SplatcraftNetworkingConstants;
import com.cibernet.splatcraft.particle.InkSplashParticleEffect;
import com.cibernet.splatcraft.particle.InkSquidSoulParticleEffect;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.apache.logging.log4j.Level;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class SplatcraftClientNetworking {
    public static void registerReceivers() {
        /*
         * INK COLORS
         */

        ClientPlayNetworking.registerGlobalReceiver(SplatcraftNetworkingConstants.PLAY_BLOCK_INKING_EFFECTS_PACKET_ID, (client, handler, buf, responseSender) -> {
            Identifier inkColorId = buf.readIdentifier();
            float scale = buf.readFloat();
            double posX = buf.readDouble();
            double posY = buf.readDouble();
            double posZ = buf.readDouble();
            double velocityX = buf.readDouble();
            double velocityY = buf.readDouble();
            double velocityZ = buf.readDouble();

            client.execute(() -> {
                if (client.world != null) {
                    SplatcraftClientNetworking.playBlockInkingEffects(client.world, InkColors.getNonNull(inkColorId), scale, new Vec3d(posX, posY, posZ), new Vec3d(velocityX, velocityY, velocityZ));
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(SplatcraftNetworkingConstants.SYNC_INK_COLOR_CHANGE_FOR_COLOR_LOCK_PACKET_ID, (client, handler, buf, responseSender) -> {
            if (client.world != null) {
                client.execute(() -> {
                    for (BlockEntity blockEntity : client.world.blockEntities) {
                        BlockPos pos = blockEntity.getPos();
                        client.world.addSyncedBlockEvent(pos, client.world.getBlockState(pos).getBlock(), 0, 0);
                    }
                });
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(SplatcraftNetworkingConstants.SYNC_INK_COLORS_REGISTRY_PACKET_ID, (client, handler, buf, responseSender) -> {
            InkColorSynchroniser.rebuildIfNeeded(InkColorSynchroniser.getCachedData());

            CompoundTag tag = buf.readCompoundTag();
            client.execute(() -> {
                if (tag != null) {
                    ListTag inkColors = tag.getList("InkColors", 10);
                    HashMap<Identifier, InkColor> all = new LinkedHashMap<>();
                    inkColors.forEach(inkColor -> {
                        CompoundTag inkColorTag = (CompoundTag) inkColor;
                        Identifier id = Identifier.tryParse(inkColorTag.getString("id"));
                        all.put(id, new InkColor(id, inkColorTag.getInt("Color")));
                    });

                    InkColors.setAll(all);
                    Splatcraft.log("Synchronised ink colors with server");
                } else {
                    Splatcraft.log(Level.ERROR, "Received ink color list was null!");
                }
            });
        });

        /*
         *  PLAYER
         */

        ClientPlayNetworking.registerGlobalReceiver(SplatcraftNetworkingConstants.PLAY_SQUID_TRAVEL_EFFECTS_PACKET_ID, (client, handler, buf, responseSender) -> {
            ClientWorld world = MinecraftClient.getInstance().world;
            if (world != null) {
                UUID uuid = buf.readUuid();
                double x = buf.readDouble();
                double y = buf.readDouble();
                double z = buf.readDouble();
                Identifier inkColorId = buf.readIdentifier();
                float scale = buf.readFloat();

                client.execute(() -> {
                    PlayerEntity player = world.getPlayerByUuid(uuid);
                    if (player != null) {
                        Vec3d vec3d = new Vec3d(x, y, z);
                        InkColor inkColor = InkColors.getNonNull(inkColorId);
                        playSquidTravelEffects(player, inkColor, scale, vec3d);
                    }
                });
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(SplatcraftNetworkingConstants.PLAY_PLAYER_TOGGLE_SQUID_EFFECTS_PACKET_ID, (client, handler, buf, responseSender) -> {
            if (client.world != null) {
                UUID uuid = buf.readUuid();
                Identifier inkColorId = buf.readIdentifier();

                client.execute(() -> {
                    PlayerEntity player = client.world.getPlayerByUuid(uuid);
                    InkColor inkColor = InkColors.getNonNull(inkColorId);
                    if (player != null && client.world != null) {
                        SplatcraftClientNetworking.playPlayerToggleSquidEffects(player, client.world, inkColor);
                    }
                });
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(SplatcraftNetworkingConstants.PLAY_INK_DEATH_EFFECTS, (client, handler, buf, responseSender) -> {
            if (client.world != null) {
                int id = buf.readInt();

                client.execute(() -> {
                    Entity entity = client.world.getEntityById(id);
                    if ((entity instanceof PlayerEntity || entity instanceof InkableEntity) && !entity.isInvisibleTo(client.player)) {
                        client.worldRenderer.addParticle(new InkSquidSoulParticleEffect(entity), true, entity.getX(), entity.getEyeY() + 0.5d, entity.getZ(), 0.0d, 1.0d, 0.0d);
                    }
                });
            }
        });

        /*
         *  SIGNALS
         */

        ClientPlayNetworking.registerGlobalReceiver(SplatcraftNetworkingConstants.PLAY_PLAYER_SIGNAL_PACKET_ID, (client, handler, buf, responseSender) -> {
            UUID uuid = buf.readUuid();
            Signal signal = SignalRegistryManager.get(buf.readIdentifier());
            if (signal != null && client.world != null) {
                client.execute(() -> SignalRendererManager.playSignal((AbstractClientPlayerEntity) client.world.getPlayerByUuid(uuid), signal));
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(SplatcraftNetworkingConstants.RESET_PLAYER_SIGNAL_PACKET_ID, (client, handler, buf, responseSender) -> {
            UUID uuid = buf.readUuid();
            if (client.world != null) {
                client.execute(() -> SignalRendererManager.reset(client.world.getPlayerByUuid(uuid)));
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(SplatcraftNetworkingConstants.DISALLOWED_PLAYER_SIGNAL_PACKET_ID, (client, handler, buf, responseSender) -> {
            UUID uuid = buf.readUuid();
            Identifier id = buf.readIdentifier();

            if (client.world != null) {
                client.execute(() -> {
                    PlayerEntity player = client.world.getPlayerByUuid(uuid);
                    if (player != null) {
                        player.sendMessage(new TranslatableText("text." + Splatcraft.MOD_ID + ".signal.disallowed", SignalRegistryManager.get(id).text).formatted(Formatting.RED), true);
                    }
                });
            }
        });
    }

    public static void playSquidTravelEffects(PlayerEntity player, InkColor inkColor, float scale, Vec3d pos) {
        Random random = player.getRandom();
        if (random.nextFloat() <= 0.482f) {
            LazyPlayerDataComponent lazyData = LazyPlayerDataComponent.getComponent(player);
            if (lazyData.isSquid()) {
                if (!lazyData.isSubmerged()) {
                    if (random.nextFloat() <= 0.9f) {
                        player.world.playSound(player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_HONEY_BLOCK_FALL, SoundCategory.PLAYERS, 0.15f, 1.0f, true);
                    }
                } else {
                    player.world.playSound(player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_PLAYER_SWIM, SoundCategory.PLAYERS, 0.05f, 2.0f, true);
                }
            }
        }

        player.world.addParticle(new InkSplashParticleEffect(ColorUtil.getColorsFromInt(inkColor.getColorOrLocked()), scale), pos.getX(), pos.getY(), pos.getZ(), 0.0d, 0.0d, 0.0d);
    }

    public static void playPlayerToggleSquidEffects(PlayerEntity player, World world, InkColor inkColor) {
        if (inkColor.matches(ColorUtil.getInkColor(player).color)) {
            for (int i = 0; i < MathHelper.nextInt(player.getRandom(), 5, 7); ++i) {
                world.addParticle(new InkSplashParticleEffect(ColorUtil.getColorsFromInt(inkColor.getColorOrLocked())), player.getParticleX(0.5d), player.getRandomBodyY() - 0.25d, player.getParticleZ(0.5d), 0.0d, 0.0d, 0.0d);
            }
        }
    }

    public static void playBlockInkingEffects(World world, InkColor inkColor, float scale, Vec3d pos, double velocityX, double velocityY, double velocityZ) {
        float[] color = ColorUtil.getColorsFromInt(inkColor.getColorOrLocked());
        world.addParticle(new InkSplashParticleEffect(color[0], color[1], color[2], scale), pos.getX(), pos.getY(), pos.getZ(), velocityX, velocityY, velocityZ);
    }
    public static void playBlockInkingEffects(World world, InkColor inkColor, float scale, Vec3d pos, Vec3d velocity) {
        SplatcraftClientNetworking.playBlockInkingEffects(world, inkColor, scale, pos, velocity.getX(), velocity.getY(), velocity.getZ());
    }

    public static void setAndSendSquidForm(ClientPlayerEntity player, boolean isSquid) {
        LazyPlayerDataComponent lazyData = LazyPlayerDataComponent.getComponent(player);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(isSquid);
        ClientPlayNetworking.send(SplatcraftNetworkingConstants.SET_SQUID_FORM_PACKET_ID, buf);

        lazyData.setIsSquid(isSquid);

        if (isSquid) {
            SignalRendererManager.reset(player);
            player.setSprinting(false);
        }
    }
}