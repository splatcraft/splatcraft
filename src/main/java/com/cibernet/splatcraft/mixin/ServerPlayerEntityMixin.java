package com.cibernet.splatcraft.mixin;

import com.cibernet.splatcraft.component.PlayerDataComponent;
import com.cibernet.splatcraft.item.AttackInputDetectable;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
    @Inject(method = "swingHand", at = @At("TAIL"))
    private void swingHand(Hand hand, CallbackInfo ci) {
        ServerPlayerEntity player = ServerPlayerEntity.class.cast(this);
        if (!player.isSpectator() && !PlayerDataComponent.isSquid(player)) {
            ItemStack stack = player.getStackInHand(hand);
            Item item = stack.getItem();
            if (item instanceof AttackInputDetectable) {
                ((AttackInputDetectable) item).onAttack(player, stack);
            }
        }
    }
}