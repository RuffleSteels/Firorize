package com.oscimate.oscimate_soulflame.mixin.fire_overlays.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.ZombieEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(ZombieEntity.class)
public class ZombieAttackMixin {
    @Inject(method = "tryAttack", at = @At("HEAD"))
    public void checkForSoulFireZombieHit(Entity target, CallbackInfoReturnable<Boolean> cir) {
        System.out.println("He has hit " + target.getEntityName());
    }
}
