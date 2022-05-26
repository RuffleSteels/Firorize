package com.oscimate.oscimate_soulflame.mixin.fire_overlays.client;

import com.oscimate.oscimate_soulflame.FireLogic;
import com.oscimate.oscimate_soulflame.Main;
import com.oscimate.oscimate_soulflame.OnSoulFireAccessor;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.FireBlock;
import net.minecraft.block.SoulFireBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFireBlock.class)
public class AbstractFireBlockMixin {
    @Inject(method = "onEntityCollision", at = @At(value = "HEAD"))
    public void checkOnSoulFire(BlockState state, World world, BlockPos pos, Entity entity, CallbackInfo ci) {
        if(state.getBlock() instanceof SoulFireBlock) {
            ((OnSoulFireAccessor) entity).setRenderSoulFire(true);
        }
        if(state.getBlock() instanceof FireBlock) {
            ((OnSoulFireAccessor) entity).setRenderSoulFire(false);
        }
    }
}
