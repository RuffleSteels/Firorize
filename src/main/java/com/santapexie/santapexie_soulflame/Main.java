package com.santapexie.santapexie_soulflame;


import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import java.util.ArrayList;
import java.util.List;


@Environment(EnvType.CLIENT)
public class Main implements ClientModInitializer {

    public static boolean shouldBeRenderingPlayer;
    public static List<Entity> onFireEntityList = new ArrayList<>();

    @Override
    public void onInitializeClient() {
         ClientTickEvents.START_CLIENT_TICK.register((client) -> {
            if(client.world != null) {
                onFireEntityList.removeIf(entity -> !entity.isPlayer() && !entity.isAlive());
                onFireEntityList.removeIf(entity -> !entity.isPlayer() && !entity.isOnFire());

                //System.out.println(onFireEntityList);
                if(!client.player.isOnFire()) {
                    shouldBeRenderingPlayer = false;
                }
            client.world.getEntities().forEach(entity -> {
                if (entity.isOnFire() && !onFireEntityList.contains(entity) && entity instanceof MobEntity) {
                    if (client.world.getBlockState(entity.getBlockPos().up()).getBlock() == Blocks.SOUL_FIRE || client.world.getBlockState(entity.getBlockPos()).getBlock() == Blocks.SOUL_FIRE) {
                        onFireEntityList.add(entity);
                    }
                } else {
                if (!entity.isPlayer() && !onFireEntityList.contains(entity) && entity instanceof MobEntity) {
                    if (client.world.getBlockState(entity.getBlockPos().south()).getBlock() == Blocks.SOUL_FIRE || client.world.getBlockState(entity.getBlockPos().north()).getBlock() == Blocks.SOUL_FIRE || client.world.getBlockState(entity.getBlockPos().east()).getBlock() == Blocks.SOUL_FIRE || client.world.getBlockState(entity.getBlockPos().west()).getBlock() == Blocks.SOUL_FIRE || client.world.getBlockState(entity.getBlockPos().up()).getBlock() == Blocks.SOUL_FIRE || client.world.getBlockState(entity.getBlockPos()).getBlock() == Blocks.SOUL_FIRE) {
                        onFireEntityList.add(entity);
                    }
                }
            }
                if (entity.isOnFire() && onFireEntityList.contains(entity) && entity instanceof MobEntity) {
                    if (client.world.getBlockState(entity.getBlockPos().up()).getBlock() == Blocks.FIRE || client.world.getBlockState(entity.getBlockPos()).getBlock() == Blocks.FIRE) {
                        onFireEntityList.remove(entity);
                    }
                }
                if (!entity.isPlayer() && onFireEntityList.contains(entity) && entity instanceof MobEntity && entity.isInLava()) {
                    onFireEntityList.remove(entity);
                }
                if(entity.isPlayer()) {
                    if (entity.isInLava()) {
                        shouldBeRenderingPlayer = false;
                    }
                }
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
