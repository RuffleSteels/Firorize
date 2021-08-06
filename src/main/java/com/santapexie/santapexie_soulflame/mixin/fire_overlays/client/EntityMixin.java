package com.santapexie.santapexie_soulflame.mixin.fire_overlays.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.santapexie.santapexie_soulflame.SoulFireEntityAccessor;

import net.minecraft.entity.Entity;

@Mixin(Entity.class)
public abstract class EntityMixin implements SoulFireEntityAccessor {

	@Unique
	private boolean onSoulFire;

	@Override
	public boolean isOnSoulFire() {
		return onSoulFire;
	}

	@Override
	public void setOnSoulFire(boolean onSoulFire) {
		this.onSoulFire = onSoulFire;
	}
	
}
