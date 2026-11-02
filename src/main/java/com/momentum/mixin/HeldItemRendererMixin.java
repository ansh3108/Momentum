package com.momentum.mixin;

import com.momentum.MomentumConfig;
import com.momentum.physics.SpringDamperSolver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {

    @Unique
    private final SpringDamperSolver momentum$yawSpring = new SpringDamperSolver(6.0f, 0.6f);

    @Unique
    private final SpringDamperSolver momentum$pitchSpring = new SpringDamperSolver(6.0f, 0.6f);

    @Unique
    private float momentum$lastYaw = 0.0f;

    @Unique
    private float momentum$lastPitch = 0.0f;

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void momentum$applySpringInertia(
            net.minecraft.client.network.AbstractClientPlayerEntity player,
            float tickDelta,
            float pitch,
            net.minecraft.util.Hand hand,
            float swingProgress,
            net.minecraft.item.ItemStack item,
            float equipProgress,
            MatrixStack matrices,
            net.minecraft.client.render.command.OrderedRenderCommandQueue vertexConsumers,
            int light,
            CallbackInfo ci) {
        if (!MomentumConfig.INSTANCE.enableViewmodelPhysics)
            return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.isPaused() || client.player == null)
            return;

        float currentYaw = client.player.getYaw(tickDelta);
        float currentPitch = client.player.getPitch(tickDelta);

        float yawDelta = currentYaw - momentum$lastYaw;
        float pitchDelta = currentPitch - momentum$lastPitch;

        // Prevent aggressive snapping when wrapping around 360 degrees
        if (Math.abs(yawDelta) > 180.0f) {
            yawDelta = 0.0f;
        }

        momentum$lastYaw = currentYaw;
        momentum$lastPitch = currentPitch;

        momentum$yawSpring.update(yawDelta, tickDelta);
        momentum$pitchSpring.update(pitchDelta, tickDelta);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(momentum$yawSpring.getPosition() * 1.2f));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(momentum$pitchSpring.getPosition() * 1.2f));
    }
}