package com.oscimate.firorize.mixin.fire_overlays.client;

import com.oscimate.firorize.RenderFireColorAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ClientPacketListener.class)
public class OscimateClientPlayNetworkHandler {
    @Shadow private ClientLevel level;
    @Inject(method = "handleDamageEvent", at = @At("HEAD"))
    public void entitySetsOnSoulFire(ClientboundDamageEventPacket packet, CallbackInfo ci) {
        Entity targetEntity = level.getEntity(packet.entityId());
        Entity sourceEntity = level.getEntity(packet.sourceDirectId());
        if (targetEntity != null && sourceEntity != null) {
            if ((sourceEntity instanceof Zombie || sourceEntity instanceof Arrow) && sourceEntity.displayFireAnimation()) {
                int[] sourceColor = ((RenderFireColorAccessor) sourceEntity).firorize$getRenderFireColor();
                if (sourceColor != null) {
                    ((RenderFireColorAccessor) targetEntity).firorize$setRenderFireColor(sourceColor);
                }
            }
        } if (targetEntity != null) {
            if (packet.getSource(level).is(DamageTypes.LIGHTNING_BOLT)) {
                ((RenderFireColorAccessor)targetEntity).firorize$setRenderFireColor(new int[]{2});
            }
        }
    }
}
