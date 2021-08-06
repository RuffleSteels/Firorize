package com.santapexie.santapexie_soulflame;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

@Environment(EnvType.CLIENT)
public class Main implements ClientModInitializer {

    public static boolean shouldBeRenderingPlayer;


    @Override
    public void onInitializeClient() {

         ClientTickEvents.START_CLIENT_TICK.register((client) -> {
            if(client.world != null) {

            client.world.getEntities().forEach(entity -> {

                if (entity.isInLava()) {
                	((SoulFireEntityAccessor) entity).setOnSoulFire(false);
                	return;
                }

                Box box = entity.getBoundingBox();
                BlockPos.Mutable mutable = new BlockPos.Mutable();
                BlockPos blockPos = new BlockPos(box.minX + 0.001D, box.minY + 0.001D, box.minZ + 0.001D);
                BlockPos blockPos2 = new BlockPos(box.maxX - 0.001D, box.maxY - 0.001D, box.maxZ - 0.001D);

                for(int i = blockPos.getX(); i <= blockPos2.getX(); ++i) {
                    for (int j = blockPos.getY(); j <= blockPos2.getY(); ++j) {
                        for (int k = blockPos.getZ(); k <= blockPos2.getZ(); ++k) {
                            mutable.set(i, j, k);
                            if (client.world.getBlockState(mutable).getBlock() == Blocks.SOUL_FIRE) {
                            	((SoulFireEntityAccessor) entity).setOnSoulFire(true);
                            	return;
                            }
                            if (client.world.getBlockState(mutable).getBlock() == Blocks.FIRE) {
                                ((SoulFireEntityAccessor) entity).setOnSoulFire(false);
                                return;
                            }
                        }
                    }
                }

                /** if (client.world.isRegionLoaded(blockPos, blockPos2)) {
                    BlockPos.Mutable mutable = new BlockPos.Mutable();

                    for(int i = blockPos.getX(); i <= blockPos2.getX(); ++i) {
                        for(int j = blockPos.getY(); j <= blockPos2.getY(); ++j) {
                            for(int k = blockPos.getZ(); k <= blockPos2.getZ(); ++k) {
                                mutable.set(i, j, k);
                                BlockState blockState = client.world.getBlockState(mutable);

                                try {
                                    blockState.onEntityCollision(client.world, mutable, entity);
                                    this.onBlockCollision(blockState);
                                } catch (Throwable var12) {
                                    CrashReport crashReport = CrashReport.create(var12, "Colliding entity with block");
                                    CrashReportSection crashReportSection = crashReport.addElement("Block being collided with");
                                    CrashReportSection.addBlockInfo(crashReportSection, this.world, mutable, blockState);
                                    throw new CrashException(crashReport);
                                }
                            }
                        }
                    }
                } **/

                /** if (entity.isOnFire() && !onFireEntityList.contains(entity) && entity instanceof MobEntity) {
                    if (client.world.getBlockState(entity.getBlockPos().up()).getBlock() == Blocks.SOUL_FIRE || client.world.getBlockState(entity.getBlockPos()).getBlock() == Blocks.SOUL_FIRE) {
                        onFireEntityList.add(entity);
                    }
                } else {
                if (!entity.isPlayer() && !onFireEntityList.contains(entity) && entity instanceof MobEntity) {
                    if (client.world.getBlockState(entity.getBlockPos().south()).getBlock() == Blocks.SOUL_FIRE || client.world.getBlockState(entity.getBlockPos().north()).getBlock() == Blocks.SOUL_FIRE || client.world.getBlockState(entity.getBlockPos().east()).getBlock() == Blocks.SOUL_FIRE || client.world.getBlockState(entity.getBlockPos().west()).getBlock() == Blocks.SOUL_FIRE || client.world.getBlockState(entity.getBlockPos().up()).getBlock() == Blocks.SOUL_FIRE || client.world.getBlockState(entity.getBlockPos()).getBlock() == Blocks.SOUL_FIRE) {
                        onFireEntityList.add(entity);
                    }
                }
            } **/
                /** if (entity.isOnFire() && onFireEntityList.contains(entity) && entity instanceof MobEntity) {
                    if (client.world.getBlockState(entity.getBlockPos().up()).getBlock() == Blocks.FIRE || client.world.getBlockState(entity.getBlockPos()).getBlock() == Blocks.FIRE) {
                        onFireEntityList.remove(entity);
                    }
                }**/
                /**
                if (client.world.getBlockState(entity.getBlockPos()).getBlock() == Blocks.SOUL_FIRE || client.world.getBlockState(entity.getBlockPos().up()).getBlock() == Blocks.SOUL_FIRE) {
                    shouldBeRendering = true;
                    System.out.println("Entity in soul fire");
                }
                 **/

            });
            }
        });
    }
}
