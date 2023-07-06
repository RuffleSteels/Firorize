package com.oscimate.oscimate_soulflame.mixin.fire_overlays.client;

import com.oscimate.oscimate_soulflame.FireLogic;
import com.oscimate.oscimate_soulflame.Main;
import com.oscimate.oscimate_soulflame.OnSoulFireAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.mixin.client.message.ClientPlayNetworkHandlerMixin;
import net.minecraft.block.Block;
import net.minecraft.block.FireBlock;
import net.minecraft.block.SoulFireBlock;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.ZombieEntityModel;
import net.minecraft.client.world.ClientEntityManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.mob.HuskEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

import static com.oscimate.oscimate_soulflame.Main.CONFIG_MANAGER;


@Mixin(ClientPlayNetworkHandler.class)
public class OscimateClientPlayNetworkHandler {

    @Shadow private ClientWorld world;

    @Inject(method = "onEntityDamage", at = @At("HEAD"))
    public void entitySetsOnSoulFire(EntityDamageS2CPacket packet, CallbackInfo ci) {
        Entity targetEntity = world.getEntityById(packet.entityId());
        Entity sourceEntity = world.getEntityById(packet.sourceDirectId());
        if (targetEntity != null && sourceEntity != null) {
            if ((sourceEntity instanceof ZombieEntity || sourceEntity instanceof ArrowEntity) && sourceEntity.doesRenderOnFire()) {
                ((OnSoulFireAccessor)targetEntity).setRenderSoulFire(((OnSoulFireAccessor) sourceEntity).isRenderSoulFire());
            }
        } if (targetEntity != null) {
            if (packet.createDamageSource(world).isOf(DamageTypes.LIGHTNING_BOLT)) {
                ((OnSoulFireAccessor)targetEntity).setRenderSoulFire(false);
            }
        }

    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void clientTickEvents(CallbackInfo ci) {
        if (world != null) {
            world.getEntities().forEach(entity -> {
                Box box = entity.getBoundingBox();
                BlockPos blockPos = new BlockPos(MathHelper.floor(box.minX + 0.001), MathHelper.floor(box.minY + 0.001), MathHelper.floor(box.minZ + 0.001));
                BlockPos blockPos2 = new BlockPos(MathHelper.floor(box.maxX - 0.001), MathHelper.floor(box.maxY - 0.001), MathHelper.floor(box.maxZ - 0.001));
                if (entity.getWorld().isRegionLoaded(blockPos, blockPos2)) {
                    BlockPos.Mutable mutable = new BlockPos.Mutable();
                    for (int i = blockPos.getX(); i <= blockPos2.getX(); ++i) {
                        for (int j = blockPos.getY(); j <= blockPos2.getY(); ++j) {
                            for (int k = blockPos.getZ(); k <= blockPos2.getZ(); ++k) {
                                mutable.set(i, j, k);
                                try {
                                    Block block = entity.getWorld().getBlockState(mutable).getBlock();
                                    if (CONFIG_MANAGER.getCurrentFireLogic() == FireLogic.PERSISTENT) {

                                        if (block instanceof SoulFireBlock) {
                                            ((OnSoulFireAccessor)entity).setRenderSoulFire(true);
                                        }
                                        if (block instanceof FireBlock) {
                                            ((OnSoulFireAccessor)entity).setRenderSoulFire(false);
                                        }
                                        if (entity.isInLava()) {
                                            ((OnSoulFireAccessor)entity).setRenderSoulFire(false);
                                        }
                                    }
                                    if (CONFIG_MANAGER.getCurrentFireLogic() == FireLogic.CONSISTENT) {
                                        if(entity.isInLava()) {
                                            ((OnSoulFireAccessor)entity).setRenderSoulFire(false);
                                        }
                                        ((OnSoulFireAccessor)entity).setRenderSoulFire(block instanceof SoulFireBlock);
                                    }
                                }
                                catch (Throwable throwable) {
                                    CrashReport crashReport = CrashReport.create(throwable, "Colliding entity with block");
                                    throw new CrashException(crashReport);
                                }
                            }
                        }
                    }
                }

            });
        }
    }

}
