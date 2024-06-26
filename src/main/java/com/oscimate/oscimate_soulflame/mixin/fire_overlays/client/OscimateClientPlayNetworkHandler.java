package com.oscimate.oscimate_soulflame.mixin.fire_overlays.client;

import com.oscimate.oscimate_soulflame.FireLogic;
import com.oscimate.oscimate_soulflame.Main;
import com.oscimate.oscimate_soulflame.RenderFireColorAccessor;
import net.minecraft.block.Block;
import net.minecraft.block.FireBlock;
import net.minecraft.block.SoulFireBlock;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
                ((RenderFireColorAccessor)targetEntity).setRenderFireColor(((RenderFireColorAccessor) sourceEntity).getRenderFireColor());
            }
        } if (targetEntity != null) {
            if (packet.createDamageSource(world).isOf(DamageTypes.LIGHTNING_BOLT)) {
                ((RenderFireColorAccessor)targetEntity).setRenderFireColor(new int[]{2});
            }
        }
    }
//    @Inject(method = "tick", at = @At("HEAD"))
//    public void clientTickEvents(CallbackInfo ci) {
//        if (world != null) {
//            world.getEntities().forEach(entity -> {
//                Box box = entity.getBoundingBox();
//                BlockPos blockPos = new BlockPos(MathHelper.floor(box.minX + 0.001), MathHelper.floor(box.minY + 0.001), MathHelper.floor(box.minZ + 0.001));
//                BlockPos blockPos2 = new BlockPos(MathHelper.floor(box.maxX - 0.001), MathHelper.floor(box.maxY - 0.001), MathHelper.floor(box.maxZ - 0.001));
//                if (entity.getWorld().isRegionLoaded(blockPos, blockPos2)) {
//                    BlockPos.Mutable mutable = new BlockPos.Mutable();
//                    for (int i = blockPos.getX(); i <= blockPos2.getX(); ++i) {
//                        for (int j = blockPos.getY(); j <= blockPos2.getY(); ++j) {
//                            for (int k = blockPos.getZ(); k <= blockPos2.getZ(); ++k) {
//                                mutable.set(i, j, k);
//                                try {
//                                    Block block = entity.getWorld().getBlockState(mutable).getBlock();
//                                    if (CONFIG_MANAGER.getCurrentFireLogic() == FireLogic.PERSISTENT) {
////                                        String blockUnder = entity.getWorld().getBlockState(mutable.down()).getBlock().getTranslationKey();
////                                        if (CONFIG_MANAGER.getCurrentBlockFireColors().containsKey(blockUnder) && block instanceof FireBlock) {
////                                            ((RenderFireColorAccessor)entity).setRenderFireColor(CONFIG_MANAGER.getCurrentBlockFireColors().get(blockUnder));
////                                        } else if (block instanceof SoulFireBlock) {
////                                            ((RenderFireColorAccessor)entity).setRenderFireColor(new int[]{1});
////                                        } else if (block instanceof FireBlock) {
////                                            ((RenderFireColorAccessor)entity).setRenderFireColor(new int[]{2});
////                                        }
////                                        if (entity.isInLava()) {
////                                            ((RenderFireColorAccessor)entity).setRenderFireColor(new int[]{2});
////                                        }
//                                    }
//                                    if (CONFIG_MANAGER.getCurrentFireLogic() == FireLogic.CONSISTENT) {
//                                        if(entity.isInLava()) {
//                                            ((RenderFireColorAccessor)entity).setRenderFireColor(new int[]{2});
//                                        }
//                                        if (block instanceof SoulFireBlock) {
//                                            ((RenderFireColorAccessor)entity).setRenderFireColor(new int[]{1});
//                                        } else {
//                                            ((RenderFireColorAccessor)entity).setRenderFireColor(new int[]{2});
//                                        }
//                                    }
//                                }
//                                catch (Throwable throwable) {
//                                    CrashReport crashReport = CrashReport.create(throwable, "Colliding entity with block");
//                                    throw new CrashException(crashReport);
//                                }
//                            }
//                        }
//                    }
//                }
//
//            });
//        }
//    }

}
