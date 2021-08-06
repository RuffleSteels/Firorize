package com.santapexie.santapexie_soulflame.mixin.fire_overlays.client;

import com.santapexie.santapexie_soulflame.OnSoulFireAccessor;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public class ClientEntityMixin implements OnSoulFireAccessor {
    @Unique
    private boolean renderSoulFire;

    @Override
    public boolean isRenderSoulFire() {
        return renderSoulFire;
    }

    @Override
    public void setRenderSoulFire(boolean renderSoulFire) {
        this.renderSoulFire = renderSoulFire;
    }
}
