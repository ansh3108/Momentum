package com.momentum.mixin;

import com.momentum.Momentum;
import com.momentum.MomentumConfig;
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

        // The updated line checking both the config toggle and the player's health
        if (MomentumConfig.INSTANCE.enableAdrenaline && healthPercentage <= 0.3f) {
            int width = context.getScaledWindowWidth();
            int height = context.getScaledWindowHeight();

            Momentum.terrorStrength = 1.0f - (healthPercentage / 0.3f);
            
            float pulsePhase = (System.currentTimeMillis() % 1000L) / 1000.0f * (float)Math.PI * 2.0f;
            float pulseIntensity = (float) (Math.sin(pulsePhase) * 0.5f + 0.5f);
            
            float alpha = Momentum.terrorStrength * (0.2f + pulseIntensity * 0.4f);
            int alphaInt = (int) (alpha * 255.0f);
            
            int color = (alphaInt << 24) | 0x880000; 
            
            context.fill(0, 0, width, height, color);
        } else {
            Momentum.terrorStrength = 0.0f;
        }
    }
}