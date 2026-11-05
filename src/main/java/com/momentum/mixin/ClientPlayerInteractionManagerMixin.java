package com.momentum.mixin;

import com.momentum.MomentumConfig;
import com.momentum.animation.BlockAnimationManager;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(method = "interactBlock", at = @At("RETURN"))
    private void momentum$onBlockPlace(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (MomentumConfig.INSTANCE.enableBlockAnimations && cir.getReturnValue().isAccepted()) {
            // Offset the position to target the newly placed block, not the block we clicked on
            BlockAnimationManager.playPlaceAnimation(hitResult.getBlockPos().offset(hitResult.getSide()));
        }
    }
}