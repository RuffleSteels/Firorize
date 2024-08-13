package com.oscimate.firorize.mixin.fire_overlays.client;

import com.oscimate.firorize.RenderFireColorAccessor;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ClientPlayNetworkHandler.class)
public class OscimateClientPlayNetworkHandler {
    @Shadow private ClientWorld world;
    @Inject(method = "onEntityDamage", at = @At("HEAD"))
    public void entitySetsOnSoulFire(EntityDamageS2CPacket packet, CallbackInfo ci) {
        Entity targetEntity = world.getEntityById(packet.entityId());
        Entity sourceEntity = world.getEntityById(packet.sourceDirectId());
        if (targetEntity != null && sourceEntity != null) {
            if ((sourceEntity instanceof ZombieEntity || sourceEntity instanceof ArrowEntity) && sourceEntity.doesRenderOnFire()) {
                ((RenderFireColorAccessor)targetEntity).firorize$setRenderFireColor(((RenderFireColorAccessor) sourceEntity).firorize$getRenderFireColor());
            }
        } if (targetEntity != null) {
            if (packet.createDamageSource(world).isOf(DamageTypes.LIGHTNING_BOLT)) {
                ((RenderFireColorAccessor)targetEntity).firorize$setRenderFireColor(new int[]{2});
            }
        }
    }
}
