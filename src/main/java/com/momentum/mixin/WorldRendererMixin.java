package com.momentum.mixin;

import com.momentum.MomentumConfig;
import com.momentum.animation.BlockAnimationManager;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    
    @Inject(method = "render", at = @At("RETURN"), require = 0)
    private void momentum$renderDynamicBlocks(CallbackInfo ci) {
        if (MomentumConfig.INSTANCE.enableBlockAnimations) {
            BlockAnimationManager.renderAnimations();
        }
    }
}