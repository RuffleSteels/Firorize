package com.oscimate.oscimate_soulflame;

import com.oscimate.oscimate_soulflame.config.ConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.FireBlock;
import net.minecraft.block.SoulFireBlock;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class Main implements ClientModInitializer {

    public static final ConfigManager CONFIG_MANAGER = new ConfigManager();

    @Override
    public void onInitializeClient() {
        if (!CONFIG_MANAGER.fileExists()) {
            CONFIG_MANAGER.save();
        }

        System.out.println(CONFIG_MANAGER.getStartupConfig());
        ClientTickEvents.START_CLIENT_TICK.register((client) -> {
            if (client.world != null) {
                client.world.getEntities().forEach(entity -> {
                    Box box = entity.getBoundingBox();
                    BlockPos blockPos = new BlockPos(MathHelper.floor(box.minX + 0.001), MathHelper.floor(box.minY + 0.001), MathHelper.floor(box.minZ + 0.001));
                    BlockPos blockPos2 = new BlockPos(MathHelper.floor(box.maxX - 0.001), MathHelper.floor(box.maxY - 0.001), MathHelper.floor(box.maxZ - 0.001));
                    if (entity.getWorld().isRegionLoaded(blockPos, blockPos2)) {
                        BlockPos.Mutable mutable = new BlockPos.Mutable();
                        boolean isSoulFire = false;
                        outerLoop:
                        for (int i = blockPos.getX(); i <= blockPos2.getX(); ++i) {
                            for (int j = blockPos.getY(); j <= blockPos2.getY(); ++j) {
                                for (int k = blockPos.getZ(); k <= blockPos2.getZ(); ++k) {
                                    mutable.set(i, j, k);

                                    try {
                                        Block block = entity.getWorld().getBlockState(mutable).getBlock();
                                        if (Main.CONFIG_MANAGER.getCurrentFireLogic() == FireLogic.PERSISTENT) {
                                            if (block instanceof SoulFireBlock) {
                                                ((OnSoulFireAccessor) entity).setRenderSoulFire(true);
                                            }
                                            if (block instanceof FireBlock) {
                                                ((OnSoulFireAccessor) entity).setRenderSoulFire(false);
                                            }
                                            if (entity.isInLava()) {
                                                ((OnSoulFireAccessor) entity).setRenderSoulFire(false);
                                            }
                                        }
                                        if (Main.CONFIG_MANAGER.getCurrentFireLogic() == FireLogic.CONSISTENT) {
                                            if (entity.isInLava()) {
                                                break outerLoop;
                                            }
                                            if (block instanceof SoulFireBlock) {
                                                isSoulFire = true;
                                                break outerLoop;
                                            }
                                        }
                                    }
                                    catch (Throwable throwable) {
                                        CrashReport crashReport = CrashReport.create(throwable, "Colliding entity with block");
                                        throw new CrashException(crashReport);
                                    }
                                }
                            }
                        }
                        if (CONFIG_MANAGER.getCurrentFireLogic() == FireLogic.CONSISTENT) {
                            ((OnSoulFireAccessor) entity).setRenderSoulFire(isSoulFire);
                        }
                    }

                });
            }
        });
    }
}