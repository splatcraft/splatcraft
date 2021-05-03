package com.cibernet.splatcraft.client.renderer.entity.ink_squid;

import com.cibernet.splatcraft.client.model.entity.InkSquidEntityModel;
import com.cibernet.splatcraft.entity.InkSquidEntity;
import com.cibernet.splatcraft.init.SplatcraftEntities;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class InkSquidEntityRenderer extends LivingEntityRenderer<LivingEntity, InkSquidEntityModel> {
    @SuppressWarnings("unused")
    public InkSquidEntityRenderer(EntityRenderDispatcher dispatcher, @Nullable EntityRendererRegistry.Context ctx) {
        super(dispatcher, new InkSquidEntityModel(), 0.2f);
        this.addFeature(new InkSquidEntityColorFeatureRenderer(this));
    }

    @Override
    protected boolean hasLabel(LivingEntity entity) {
        return super.hasLabel(entity) && (entity.shouldRenderName() || entity.hasCustomName() && entity == this.dispatcher.targetedEntity);
    }

    @Override
    public Identifier getTexture(LivingEntity entity) {
        return SplatcraftEntities.texture(InkSquidEntity.id + "/" + InkSquidEntity.id + "_overlay");
    }
}