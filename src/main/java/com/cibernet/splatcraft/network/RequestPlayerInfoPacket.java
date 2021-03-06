package com.cibernet.splatcraft.network;

import com.cibernet.splatcraft.network.base.PlayToServerPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;

import java.util.UUID;

public class RequestPlayerInfoPacket extends PlayToServerPacket
{
    UUID target;

    public RequestPlayerInfoPacket(PlayerEntity target)
    {
        this.target = target.getUniqueID();
    }

    private RequestPlayerInfoPacket(UUID target)
    {
        this.target = target;
    }

    public static RequestPlayerInfoPacket decode(PacketBuffer buffer)
    {
        return new RequestPlayerInfoPacket(buffer.readUniqueId());
    }

    @Override
    public void encode(PacketBuffer buffer)
    {
        buffer.writeUniqueId(target);
    }

    @Override
    public void execute(PlayerEntity player)
    {
        ServerPlayerEntity target = (ServerPlayerEntity) player.world.getPlayerByUuid(this.target);
        if (target != null)
        {
            SplatcraftPacketHandler.sendToPlayer(new UpdatePlayerInfoPacket(target), (ServerPlayerEntity) player);
        }
    }

}
