package com.oscimate.oscimate_soulflame.mixin.fire_overlays.client;

import com.oscimate.oscimate_soulflame.FireLogic;
import com.oscimate.oscimate_soulflame.Main;
import com.oscimate.oscimate_soulflame.OnSoulFireAccessor;
import net.minecraft.block.FireBlock;
import net.minecraft.block.SoulFireBlock;
import net.minecraft.block.SoulSandBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.fluid.LavaFluid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow World world;
    @Shadow abstract BlockPos getBlockPos();

    @Inject(method = "baseTick", at = @At("HEAD"))
    public void checkBlockUnderneath(CallbackInfo ci) {
        if(world.isClient) {
            if (world.getBlockState(getBlockPos()).getBlock() instanceof SoulFireBlock) {
                ((OnSoulFireAccessor) ((Entity) (Object) this)).setRenderSoulFire(true);
            }
            if (world.getBlockState(getBlockPos()).getBlock() instanceof FireBlock) {
                ((OnSoulFireAccessor) ((Entity) (Object) this)).setRenderSoulFire(false);
            }
            if (((Entity) (Object) this).isInLava()) {
                ((OnSoulFireAccessor) ((Entity) (Object) this)).setRenderSoulFire(false);
            }
            if (Main.CONFIG_MANAGER.getCurrentFireLogic() == FireLogic.CONSISTENT) {
                if (!(world.getBlockState(getBlockPos()).getBlock() instanceof SoulFireBlock)) {
                    ((OnSoulFireAccessor) ((Entity) (Object) this)).setRenderSoulFire(false);
                }
            }
        }
    }
}
