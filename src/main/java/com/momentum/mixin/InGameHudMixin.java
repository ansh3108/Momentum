package com.momentum.mixin;

import com.momentum.Momentum;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void momentum$renderAdrenaline(net.minecraft.client.gui.DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        float healthPercentage = client.player.getHealth() / client.player.getMaxHealth();

        if (healthPercentage <= 0.3f) {
            int width = context.getScaledWindowWidth();
            int height = context.getScaledWindowHeight();

            Momentum.terrorStrength = 1.0f - (healthPercentage / 0.3f);
            
            // Use system time for a smooth heartbeat pulse
            float pulsePhase = (System.currentTimeMillis() % 1000L) / 1000.0f * (float)Math.PI * 2.0f;
            float pulseIntensity = (float) (Math.sin(pulsePhase) * 0.5f + 0.5f);
            
            // Calculate dynamic alpha based on terror strength and pulse
            float alpha = Momentum.terrorStrength * (0.2f + pulseIntensity * 0.4f);
            int alphaInt = (int) (alpha * 255.0f);
            
            // 1.21.11 utilizes strict ARGB integer passing for UI colors. 
            // Bitshift format: (Alpha << 24) | (Red << 16) | (Green << 8) | Blue
            int color = (alphaInt << 24) | 0x880000; // Deep red tint
            
            // context.fill automatically handles modern blending without RenderSystem calls
            context.fill(0, 0, width, height, color);
        } else {
            Momentum.terrorStrength = 0.0f;
        }
    }
}