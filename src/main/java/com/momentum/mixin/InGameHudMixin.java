package com.momentum.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    // --- Crosshair Trackers ---
    @Unique private float momentum$spread = 0.0f;
    @Unique private double momentum$lastX = 0.0;
    @Unique private double momentum$lastZ = 0.0;
    @Unique private long momentum$lastTime = System.currentTimeMillis();
    @Unique private double momentum$currentSpeed = 0.0;

    // --- Status HUD Trackers ---
    @Unique private float momentum$delayedHealth = 20.0f;
    @Unique private float momentum$lastHealth = 20.0f;
    @Unique private float momentum$shakeMagnitude = 0.0f;
    @Unique private long momentum$lastStatusFrame = System.currentTimeMillis();

    // ==========================================
    // 1. DYNAMIC CROSSHAIR
    // ==========================================
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    private void momentum$renderDynamicCrosshair(DrawContext context, @Coerce Object tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        ci.cancel(); 

        if (!client.options.getPerspective().isFirstPerson()) return;

        int centerX = client.getWindow().getScaledWidth() / 2;
        int centerY = client.getWindow().getScaledHeight() / 2;

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

        float targetSpread = 0.0f;
        if (momentum$currentSpeed > 0.5) {
            targetSpread = (float) Math.min(momentum$currentSpeed * 2.5, 16.0);
        }

        momentum$spread += (targetSpread - momentum$spread) * 0.3f;
        int spread = Math.round(momentum$spread);

        int shadow = 0xAA000000;
        int glow = 0xFF00FFAA; 

        context.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, shadow);
        context.fill(centerX, centerY, centerX + 1, centerY + 1, 0xFFFFFFFF);

        if (spread > 2) {
            context.fill(centerX - spread - 1, centerY - 1, centerX - spread + 2, centerY + 2, shadow);
            context.fill(centerX - spread, centerY, centerX - spread + 1, centerY + 1, glow);
            context.fill(centerX + spread - 1, centerY - 1, centerX + spread + 2, centerY + 2, shadow);
            context.fill(centerX + spread, centerY, centerX + spread + 1, centerY + 1, glow);
            context.fill(centerX - 1, centerY + spread - 1, centerX + 2, centerY + spread + 2, shadow);
            context.fill(centerX, centerY + spread, centerX + 1, centerY + spread + 1, glow);
        }
        
        float cooldown = client.player.getAttackCooldownProgress(1.0F); 
        if (cooldown < 1.0F) {
            int barWidth = 14;
            int fillWidth = (int) (barWidth * cooldown);
            int yOffset = 10 + spread; 
            context.fill(centerX - barWidth / 2 - 1, centerY + yOffset - 1, centerX + barWidth / 2 + 1, centerY + yOffset + 2, shadow);
            if (fillWidth > 0) context.fill(centerX - barWidth / 2, centerY + yOffset, centerX - barWidth / 2 + fillWidth, centerY + yOffset + 1, glow);
        }
    }

    // ==========================================
    // 2. TERMINAL STATUS HUD
    // ==========================================
    // FIX: Removed require=0 so it matches perfectly, and updated signature to (DrawContext, CallbackInfo)
@Inject(method = "renderStatusBars", at = @At("HEAD"), cancellable = true)
    private void momentum$renderTerminalStatus(DrawContext context, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || player.isSpectator()) return;
        
        ci.cancel(); 
        
        long now = System.currentTimeMillis();
        float dt = (now - momentum$lastStatusFrame) / 1000.0f;
        
        if (dt > 0.005f) {
            if (dt > 0.05f) dt = 0.05f;
            momentum$lastStatusFrame = now;
            
            float trueHealth = player.getHealth();
            if (trueHealth < momentum$lastHealth) {
                momentum$shakeMagnitude += (momentum$lastHealth - trueHealth) * 2.5f; 
            }
            momentum$lastHealth = trueHealth;
            
            if (momentum$delayedHealth > trueHealth) {
                momentum$delayedHealth -= (momentum$delayedHealth - trueHealth) * (dt * 3.0f);
                if (momentum$delayedHealth - trueHealth < 0.1f) momentum$delayedHealth = trueHealth;
            } else {
                momentum$delayedHealth = trueHealth; 
            }
            
            momentum$shakeMagnitude *= (1.0f - 12.0f * dt);
            if (momentum$shakeMagnitude < 0.1f) momentum$shakeMagnitude = 0.0f;
        }

        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        int offsetX = 0;
        int offsetY = 0;
        if (momentum$shakeMagnitude > 0) {
            offsetX = Math.round((player.getRandom().nextFloat() - 0.5f) * momentum$shakeMagnitude);
            offsetY = Math.round((player.getRandom().nextFloat() - 0.5f) * momentum$shakeMagnitude);
        }

        // Lowered baseY from height - 45 to height - 36 to sit flush above the XP text
        int leftX = width / 2 - 110 + offsetX;
        int rightX = width / 2 + 10 + offsetX;
        int baseY = height - 36 + offsetY;

        float maxHealth = Math.max(player.getMaxHealth(), 1.0f);
        float trueHealth = player.getHealth();
        int armor = player.getArmor();
        int food = player.getHungerManager().getFoodLevel();

        int bgTrack = 0xAA0D0D12; 
        int hpColor = 0xFF14F195; 
        int dmgColor = 0xFFFF2244; 
        int arColor = 0xFF00C8FF; 
        int fdColor = 0xFFFF9900; 

        boolean critical = (trueHealth <= maxHealth * 0.3f);
        int pulseAlpha = critical ? (int) ((Math.sin(now / 150.0) * 0.4 + 0.3) * 255) : 0;
        int critBg = (pulseAlpha << 24) | 0xFF0000; 

        // ==========================================
        // LEFT SIDE: HEALTH & ARMOR
        // ==========================================
        int panelTop = (armor > 0) ? baseY - 10 : baseY; 
        
        context.fill(leftX - 4, panelTop - 2, leftX, baseY + 6, hpColor); 
        context.fill(leftX, panelTop - 2, leftX + 102, baseY + 6, bgTrack); 
        if (critical) context.fill(leftX, panelTop - 2, leftX + 102, baseY + 6, critBg); 

        if (armor > 0) {
            context.fill(leftX + 1, baseY - 8, leftX + 101, baseY - 4, 0x55000000); 
            int armorW = (int) ((armor / 20.0f) * 100.0f);
            context.fill(leftX + 1, baseY - 8, leftX + 1 + armorW, baseY - 4, arColor);
        }

        context.fill(leftX + 1, baseY, leftX + 101, baseY + 4, 0x55000000); 
        int delayedW = (int) ((momentum$delayedHealth / maxHealth) * 100.0f);
        context.fill(leftX + 1, baseY, leftX + 1 + delayedW, baseY + 4, dmgColor); 
        int trueW = (int) ((trueHealth / maxHealth) * 100.0f);
        context.fill(leftX + 1, baseY, leftX + 1 + trueW, baseY + 4, hpColor); 
        
        String hpText = "HP " + (int) Math.ceil(trueHealth);
        context.drawText(client.textRenderer, hpText, leftX, panelTop - 11, hpColor, true);

        for (int i = 1; i <= 9; i++) {
            context.fill(leftX + (i * 10), baseY + 4, leftX + (i * 10) + 1, baseY + 6, 0x55FFFFFF);
        }

        // ==========================================
        // RIGHT SIDE: ENERGY (FOOD)
        // ==========================================
        context.fill(rightX + 102, baseY - 2, rightX + 106, baseY + 6, fdColor); 
        context.fill(rightX, baseY - 2, rightX + 102, baseY + 6, bgTrack); 

        context.fill(rightX + 1, baseY, rightX + 101, baseY + 4, 0x55000000); 
        int foodW = (int) ((food / 20.0f) * 100.0f);
        context.fill(rightX + 101 - foodW, baseY, rightX + 101, baseY + 4, fdColor); 

        String fdText = "EN " + food;
        int fdTextWidth = client.textRenderer.getWidth(fdText);
        context.drawText(client.textRenderer, fdText, rightX + 102 - fdTextWidth, baseY - 11, fdColor, true);
        
        for (int i = 1; i <= 9; i++) {
            context.fill(rightX + (i * 10), baseY + 4, rightX + (i * 10) + 1, baseY + 6, 0x55FFFFFF);
        }
    }}