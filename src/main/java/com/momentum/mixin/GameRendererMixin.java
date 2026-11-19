package com.momentum.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Shadow
    private void setPostProcessor(Identifier id) {}

    @Inject(method = "onCameraEntitySet", at = @At("TAIL"))
    private void momentum$loadAtmosphere(Entity entity, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (entity != null && client.world != null) {
            this.setPostProcessor(Identifier.of("momentum", "atmosphere"));
        }
    }
}