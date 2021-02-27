package com.cibernet.splatcraft.client.model;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.CompositeEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

public class InkSquidEntityModel extends CompositeEntityModel<LivingEntity> {
    private final ModelPart body;
    private final ModelPart leftTentacle;
    private final ModelPart rightTentacle;

    public InkSquidEntityModel() {
        textureWidth = 32;
        textureHeight = 32;

        body = new ModelPart(this);
        body.setPivot(0.0F, 22.5F, 3.0F);
        body.setTextureOffset(4, 2).addCuboid(-3.5F, -1.5F, -12.0F, 7.0F, 3.0F, 3.0F, 0.0F, false);
        body.setTextureOffset(0, 0).addCuboid(-4.5F, -2.0F, -9.0F, 9.0F, 4.0F, 5.0F, 0.0F, false);
        body.setTextureOffset(0, 9).addCuboid(-3.5F, -1.5F, -4.0F, 7.0F, 3.0F, 4.0F, 0.0F, false);

        leftTentacle = new ModelPart(this);
        leftTentacle.setPivot(2.5F, 22.5F, 2.0F);
        leftTentacle.setTextureOffset(14, 23).addCuboid(-1.0F, -1.0F, 1.0F, 2.0F, 2.0F, 3.0F, 0.0F, true);
        leftTentacle.setTextureOffset(14, 17).addCuboid(-2.0F, -1.0F, 4.0F, 3.0F, 2.0F, 4.0F, 0.0F, true);

        rightTentacle = new ModelPart(this);
        rightTentacle.setPivot(-2.5F, 22.5F, 2.0F);
        rightTentacle.setTextureOffset(14, 23).addCuboid(-1.0F, -1.0F, 1.0F, 2.0F, 2.0F, 3.0F, 0.0F, false);
        rightTentacle.setTextureOffset(14, 17).addCuboid(-1.0F, -1.0F, 4.0F, 3.0F, 2.0F, 4.0F, 0.0F, false);
    }

    @Override
    public void setAngles(LivingEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {}

    @Override
    public void animateModel(LivingEntity entity, float limbAngle, float limbDistance, float tickDelta) {
        boolean isSwimming = entity.isSwimming();

        if (!entity.hasVehicle()) {
            float angle = isSwimming ? (float) -((entity.pitch * Math.PI) / 180F) : (float) (entity.getY() - entity.prevY) * 1.1f;
            this.body.pitch = (float) -Math.min(Math.PI / 2, Math.max(-Math.PI / 2, angle));
        }

        if (entity.isOnGround() || isSwimming) {
            this.leftTentacle.yaw = MathHelper.cos(limbAngle * 0.6662F + (float) Math.PI) * 1.4F * limbDistance / (isSwimming ? 2.2f : 1.5f);
            this.rightTentacle.yaw = MathHelper.cos(limbAngle * 0.6662F) * 1.4F * limbDistance / (isSwimming ? 2.2f : 1.5f);
        } else {
            if (Math.abs(Math.round(rightTentacle.yaw * 100)) != 0) {
                this.rightTentacle.yaw -= rightTentacle.yaw / 8.0F;
            }
            if (Math.abs(Math.round(leftTentacle.yaw * 100)) != 0) {
                this.leftTentacle.yaw -= leftTentacle.yaw / 8.0F;
            }
        }
    }

    @Override
    public Iterable<ModelPart> getParts() {
        return ImmutableList.of(body, leftTentacle, rightTentacle);
    }

    /*public void setRotationAngle(ModelPart bone, float x, float y, float z) {
        bone.pitch = x;
        bone.yaw = y;
        bone.roll = z;
    }*/
}