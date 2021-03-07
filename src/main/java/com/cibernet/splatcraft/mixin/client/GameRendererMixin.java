package com.cibernet.splatcraft.mixin.client;

import com.cibernet.splatcraft.client.config.SplatcraftConfig;
import com.cibernet.splatcraft.client.config.enums.PreventBobView;
import com.cibernet.splatcraft.component.PlayerDataComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void bobView(MatrixStack matrixStack, float f, CallbackInfo ci) {
        if (this.client.player != null) {
            PlayerDataComponent data = PlayerDataComponent.getComponent(this.client.player);
            PreventBobView preventBobView = SplatcraftConfig.UI.preventBobViewWhenSquid.getEnum();
            if (preventBobView != PreventBobView.OFF && data.isSquid()) {
                if (preventBobView != PreventBobView.SUBMERGED || data.isSubmerged()) {
                    ci.cancel();
                }
            }
        }
    }
}
