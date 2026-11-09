package com.momentum.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    // Smooth UI interpolation
    @Unique private float momentum$spread = 0.0f;
    
    // Manual coordinate tracking prevents the speed from ever getting "stuck"
    @Unique private double momentum$lastX = 0.0;
    @Unique private double momentum$lastZ = 0.0;
    @Unique private long momentum$lastTime = System.currentTimeMillis();
    @Unique private double momentum$currentSpeed = 0.0;

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    private void momentum$renderDynamicCrosshair(DrawContext context, @Coerce Object tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        ci.cancel(); 

        // Hide in 3rd person
        if (!client.options.getPerspective().isFirstPerson()) {
            return;
        }

        int centerX = client.getWindow().getScaledWidth() / 2;
        int centerY = client.getWindow().getScaledHeight() / 2;

        // 1. Calculate true speed
        double currentX = client.player.getX();
        double currentZ = client.player.getZ();
        long now = System.currentTimeMillis();
        double dt = (now - momentum$lastTime) / 1000.0;

        if (dt >= 0.05) { 
            if (dt > 1.0) {
                momentum$currentSpeed = 0.0;
            } else {
                double dx = currentX - momentum$lastX;
                double dz = currentZ - momentum$lastZ;
                momentum$currentSpeed = Math.sqrt(dx * dx + dz * dz) / dt;
            }
            momentum$lastX = currentX;
            momentum$lastZ = currentZ;
            momentum$lastTime = now;
        }

        // 2. Deadzone target mapping (Requires > 0.5 speed to trigger so it fully collapses)
        float targetSpread = 0.0f;
        if (momentum$currentSpeed > 0.5) {
            targetSpread = (float) Math.min(momentum$currentSpeed * 2.5, 16.0);
        }

        // 3. Apply stable Lerp
        momentum$spread += (targetSpread - momentum$spread) * 0.3f;
        int spread = Math.round(momentum$spread);

        int shadow = 0xAA000000;
        int glow = 0xFF00FFAA; 

        // 4. Center Dot (Always visible)
        context.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, shadow);
        context.fill(centerX, centerY, centerX + 1, centerY + 1, 0xFFFFFFFF);

        // 5. Draw the 3 outer dots only if they have expanded past the center dot
        if (spread > 2) {
            // Left Dot
            context.fill(centerX - spread - 1, centerY - 1, centerX - spread + 2, centerY + 2, shadow);
            context.fill(centerX - spread, centerY, centerX - spread + 1, centerY + 1, glow);
            
            // Right Dot
            context.fill(centerX + spread - 1, centerY - 1, centerX + spread + 2, centerY + 2, shadow);
            context.fill(centerX + spread, centerY, centerX + spread + 1, centerY + 1, glow);
            
            // Bottom Dot
            context.fill(centerX - 1, centerY + spread - 1, centerX + 2, centerY + spread + 2, shadow);
            context.fill(centerX, centerY + spread, centerX + 1, centerY + spread + 1, glow);
        }
        
        // 6. Dynamic Attack Cooldown Indicator
        float cooldown = client.player.getAttackCooldownProgress(1.0F); 
        if (cooldown < 1.0F) {
            int barWidth = 14;
            int fillWidth = (int) (barWidth * cooldown);
            // Push the cooldown bar down smoothly as the bottom dot expands
            int yOffset = 10 + spread; 
            
            context.fill(centerX - barWidth / 2 - 1, centerY + yOffset - 1, centerX + barWidth / 2 + 1, centerY + yOffset + 2, shadow);
            if (fillWidth > 0) {
                context.fill(centerX - barWidth / 2, centerY + yOffset, centerX - barWidth / 2 + fillWidth, centerY + yOffset + 1, glow);
            }
        }
    }
}