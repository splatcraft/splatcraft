package com.cibernet.splatcraft.mixin.client;

import com.cibernet.splatcraft.item.inkable.InkableArmorItem;
import com.cibernet.splatcraft.item.weapon.AbstractWeaponItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(ItemStack.class)
public abstract class ClientItemStackMixin {
    @Shadow public abstract Item getItem();

    @Environment(EnvType.CLIENT)
    @Inject(method = "getHideFlags", at = @At("HEAD"), cancellable = true)
    private void getHideFlags(CallbackInfoReturnable<Integer> cir) {
        Item item = this.getItem();
        if (item instanceof AbstractWeaponItem || item instanceof InkableArmorItem) {
            cir.setReturnValue(2);
        }
    }
}