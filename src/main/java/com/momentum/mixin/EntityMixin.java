package com.momentum.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {

    // Intercept the final coordinate calculation before ANY renderer sees it
    @Inject(method = "getLerpedPos", at = @At("RETURN"), cancellable = true)
    private void momentum$spoofItemPosition(float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        
        // 1. Only intercept Item Entities
        if (!((Object) this instanceof ItemEntity item)) return;

        // 2. Ensure we only run on the visual client world to avoid server conflicts
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        if (client.world.getEntityById(item.getId()) != item) return;

        PlayerEntity player = client.player;
        if (player.isSpectator() || player.isSneaking()) return;

        // 3. Grab the original vanilla position
        Vec3d pos = cir.getReturnValue();

        double dx = player.getX() - pos.x;
        double dy = (player.getY() + 1.0) - pos.y; // Aim for the chest
        double dz = player.getZ() - pos.z;
        double distSq = dx * dx + dy * dy + dz * dz;

        double offsetX = 0.0;
        double offsetY = 0.0;
        double offsetZ = 0.0;

        // 4. Calculate Magnetic Vacuum Pull
        if (item.age > 25 && distSq > 0.001 && distSq < 36.0) {
            double dist = Math.sqrt(distSq);
            double pull = Math.pow(1.0 - (dist / 6.0), 3.0);
            
            offsetX = dx * pull;
            offsetY = dy * pull;
            offsetZ = dz * pull;
        }

        // 5. Calculate Initial Pop Animation
        if (item.age < 12) {
            float ageF = item.age + tickDelta;
            double popForce = Math.max(0.0, 1.0 - (ageF / 12.0));
            offsetY += Math.sin(ageF * 0.5) * popForce * 0.6;
        }

        // 6. Firewall against NaN corruption
        if (Double.isNaN(offsetX) || Double.isInfinite(offsetX)) offsetX = 0.0;
        if (Double.isNaN(offsetY) || Double.isInfinite(offsetY)) offsetY = 0.0;
        if (Double.isNaN(offsetZ) || Double.isInfinite(offsetZ)) offsetZ = 0.0;

        // 7. Inject the spoofed coordinates back into the rendering pipeline!
        if (offsetX != 0.0 || offsetY != 0.0 || offsetZ != 0.0) {
            cir.setReturnValue(pos.add(offsetX, offsetY, offsetZ));
        }
    }
}