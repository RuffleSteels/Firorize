package com.santapexie.santapexie_soulflame;


import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;


@Environment(EnvType.CLIENT)
public class Main implements ClientModInitializer {

    public static boolean shouldBeRenderingPlayer;

    @Override
    public void onInitializeClient() {

         ClientTickEvents.START_CLIENT_TICK.register((client) -> {
            if(client.world != null) {
                if(!client.player.isOnFire()) {
                    shouldBeRenderingPlayer = false;
                }
            client.world.getEntities().forEach(entity -> {
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
