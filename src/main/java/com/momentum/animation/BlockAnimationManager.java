package com.momentum.animation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class BlockAnimationManager {
    private static final java.util.concurrent.ConcurrentHashMap<BlockPos, Long> ANIMATIONS = new java.util.concurrent.ConcurrentHashMap<>();
    public static final long DURATION = 200L;

    public static void playPlaceAnimation(BlockPos pos) {
        ANIMATIONS.put(pos, System.currentTimeMillis());
    }

    public static void renderAnimations() {
        if (ANIMATIONS.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        long now = System.currentTimeMillis();
        ANIMATIONS.entrySet().removeIf(entry -> (now - entry.getValue()) > DURATION);

        if (ANIMATIONS.isEmpty()) return;

        // Completely bypass the Camera class to fix all IDE mapping errors
        Vec3d cameraPos = client.player.getCameraPosVec(1.0F);
        float pitch = client.player.getPitch();
        float yaw = client.player.getYaw();
        
        MatrixStack matrices = new MatrixStack();
        
        // Reconstruct the View Matrix using the player's rotation
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(yaw + 180.0F));

        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();

        for (java.util.Map.Entry<BlockPos, Long> entry : ANIMATIONS.entrySet()) {
            BlockPos pos = entry.getKey();
            long elapsed = now - entry.getValue();
            float progress = (float) elapsed / DURATION;
            
            float scale = 1.005f + (float) (Math.sin(progress * Math.PI) * 0.15f * (1.0f - progress));

            matrices.push();
            matrices.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
            
            matrices.translate(0.5, 0.5, 0.5);
            matrices.scale(scale, scale, scale);
            matrices.translate(-0.5, -0.5, -0.5);

            client.getBlockRenderManager().renderBlockAsEntity(
                    client.world.getBlockState(pos),
                    matrices,
                    immediate,
                    15728880,
                    OverlayTexture.DEFAULT_UV
            );
            matrices.pop();
        }
        
        immediate.draw();
    }
}