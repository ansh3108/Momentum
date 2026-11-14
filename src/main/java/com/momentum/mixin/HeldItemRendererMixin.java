package com.momentum.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {

    @Unique private float momentum$swayYaw = 0.0f;
    @Unique private float momentum$swayPitch = 0.0f;
    @Unique private float momentum$impactPos = 0.0f;
    @Unique private float momentum$impactVel = 0.0f;
    @Unique private float momentum$flinchPos = 0.0f;

    @Unique private float momentum$lastYaw = 0.0f;
    @Unique private float momentum$lastPitch = 0.0f;
    @Unique private int momentum$lastHurtTime = 0;
    @Unique private float momentum$flinchDirection = 1.0f;
    @Unique private long momentum$lastFrameTime = System.currentTimeMillis();

    @Inject(method = "renderItem", at = @At("HEAD"))
    private void momentum$applyHeavyPhysics(LivingEntity entity, ItemStack stack, ItemDisplayContext context, MatrixStack matrices, @Coerce Object renderQueue, int light, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.player == null || client.isPaused() || entity != client.player) return;
        if (context != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND && context != ItemDisplayContext.FIRST_PERSON_LEFT_HAND) return;

        ClientPlayerEntity player = client.player;
        long now = System.currentTimeMillis();
        
        float dt = (now - momentum$lastFrameTime) / 1000.0f;

        if (dt > 0.005f) { 
            if (dt > 0.05f) dt = 0.05f; 
            momentum$lastFrameTime = now;

            // --- 1. Camera Sway ---
            float currentYaw = (float) player.getYaw();
            float currentPitch = (float) player.getPitch();
            float yawDelta = currentYaw - momentum$lastYaw;
            float pitchDelta = currentPitch - momentum$lastPitch;

            while (yawDelta >= 180.0f) yawDelta -= 360.0f;
            while (yawDelta < -180.0f) yawDelta += 360.0f;

            momentum$lastYaw = currentYaw;
            momentum$lastPitch = currentPitch;

            momentum$swayYaw += (yawDelta * 2.5f - momentum$swayYaw) * (dt * 12.0f);
            momentum$swayPitch += (pitchDelta * 2.5f - momentum$swayPitch) * (dt * 12.0f);

            // --- 2. Fall & Landing Impact ---
            double yVelocity = player.getVelocity().y;
            double fallDist = player.fallDistance;
            float targetImpact = 0.0f;
            
            // Trigger exactly when falling past 1.0 block
            if (!player.isOnGround() && yVelocity < -0.1 && fallDist > 1.0) {
                float fallModifier = (float) Math.min((fallDist - 1.0) * 2.0, 1.0);
                targetImpact = (float) Math.min(Math.abs(yVelocity) * 6.0, 6.0) * fallModifier;
            }

            float force = (targetImpact - momentum$impactPos) * 200.0f;
            momentum$impactVel += force * dt;
            momentum$impactVel *= (1.0f - 10.0f * dt); 
            
            if (momentum$impactVel > 60.0f) momentum$impactVel = 60.0f;
            if (momentum$impactVel < -60.0f) momentum$impactVel = -60.0f;
            
            momentum$impactPos += momentum$impactVel * dt;

            if (momentum$impactPos < -5.0f) {
                momentum$impactPos = -5.0f;
                momentum$impactVel *= -0.5f; 
            }
            if (momentum$impactPos > 6.0f) {
                momentum$impactPos = 6.0f;
                momentum$impactVel = 0.0f;
            }

            // --- 3. Damage Flinch ---
            int hurtTime = player.hurtTime;
            float targetFlinch = 0.0f;

            if (hurtTime > 0) {
                if (hurtTime == player.maxHurtTime && momentum$lastHurtTime != hurtTime) {
                    momentum$flinchDirection = player.getRandom().nextBoolean() ? 1.0f : -1.0f;
                }
                targetFlinch = (float) ((hurtTime / 10.0) * 25.0 * momentum$flinchDirection);
            }
            momentum$lastHurtTime = hurtTime;
            
            momentum$flinchPos += (targetFlinch - momentum$flinchPos) * (dt * 15.0f);
        }

        if (Float.isNaN(momentum$impactPos)) momentum$impactPos = 0.0f;
        if (Float.isNaN(momentum$swayYaw)) momentum$swayYaw = 0.0f;
        if (Float.isNaN(momentum$swayPitch)) momentum$swayPitch = 0.0f;
        if (Float.isNaN(momentum$flinchPos)) momentum$flinchPos = 0.0f;

        float handModifier = (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) ? -1.0f : 1.0f;

        // --- APPLY MATRICES ---
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(momentum$swayYaw * 1.5f));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(momentum$swayPitch * 1.5f));

        matrices.translate(0.0f, momentum$impactPos * 0.04f, 0.0f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(momentum$impactPos * 4.0f));

        matrices.translate(momentum$flinchPos * 0.015f * handModifier, 0.0f, 0.0f);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(momentum$flinchPos * 2.5f * handModifier));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(momentum$flinchPos * 1.5f * handModifier));
    }
}