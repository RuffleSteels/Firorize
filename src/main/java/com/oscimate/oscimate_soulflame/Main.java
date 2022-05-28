package com.oscimate.oscimate_soulflame;

import com.oscimate.oscimate_soulflame.config.ConfigManager;
import com.oscimate.oscimate_soulflame.config.FireLogicConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.tag.BlockTags;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.checkerframework.checker.units.qual.C;

@Environment(EnvType.CLIENT)
public class Main implements ClientModInitializer {


    public static final ConfigManager CONFIG_MANAGER = new ConfigManager();


    @Override
    public void onInitializeClient() {
        if(!this.CONFIG_MANAGER.fileExists()) {
            this.CONFIG_MANAGER.save();
        }
        System.out.println(this.CONFIG_MANAGER.getStartupConfig());
        ClientTickEvents.START_CLIENT_TICK.register((client) -> {
            if (client.world != null) {
                client.world.getEntities().forEach(entity -> {
                    Box box = entity.getBoundingBox();
                    BlockPos blockPos = new BlockPos(box.minX + 0.001, box.minY + 0.001, box.minZ + 0.001);
                    BlockPos blockPos2 = new BlockPos(box.maxX - 0.001, box.maxY - 0.001, box.maxZ - 0.001);
                    if (entity.world.isRegionLoaded(blockPos, blockPos2)) {
                        BlockPos.Mutable mutable = new BlockPos.Mutable();
                        Boolean isSoulFire = false;
                        outerLoop:
                        for (int i = blockPos.getX(); i <= blockPos2.getX(); ++i) {
                            for (int j = blockPos.getY(); j <= blockPos2.getY(); ++j) {
                                for (int k = blockPos.getZ(); k <= blockPos2.getZ(); ++k) {
                                    mutable.set(i, j, k);
                                    BlockState blockState = entity.world.getBlockState(mutable);
                                    try {
                                        if (Main.CONFIG_MANAGER.getCurrentFireLogic() == FireLogic.PERSISTENT) {
                                            if (client.world.getBlockState(mutable).getBlock() instanceof SoulFireBlock) {
                                                ((OnSoulFireAccessor)entity).setRenderSoulFire(true);
                                            }
                                            if (client.world.getBlockState(mutable).getBlock() instanceof FireBlock) {
                                                ((OnSoulFireAccessor)entity).setRenderSoulFire(false);
                                            }
                                            if (entity.isInLava()) {
                                                ((OnSoulFireAccessor)entity).setRenderSoulFire(false);
                                            }
                                        }
                                        if (Main.CONFIG_MANAGER.getCurrentFireLogic() == FireLogic.CONSISTENT) {
                                            if(entity.isInLava()) {
                                                isSoulFire = false;
                                                break outerLoop;
                                            }
                                            if (client.world.getBlockState(mutable).getBlock() instanceof SoulFireBlock) {
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
                        if(CONFIG_MANAGER.getCurrentFireLogic() == FireLogic.CONSISTENT) {
                            ((OnSoulFireAccessor) entity).setRenderSoulFire(isSoulFire);
                        }
                    }

                });
            }
        });
    }
}