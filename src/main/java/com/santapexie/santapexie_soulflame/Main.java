package com.santapexie.santapexie_soulflame;


import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SoulFireBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.fluid.LavaFluid;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.*;
import net.minecraft.world.CollisionView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.BiPredicate;


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

                System.out.println(onFireEntityList);
                if(!client.player.isOnFire()) {
                    shouldBeRenderingPlayer = false;
                }

            client.world.getEntities().forEach(entity -> {
                if (!entity.isPlayer() && !onFireEntityList.contains(entity) && entity instanceof MobEntity) {
                    if (client.world.getBlockState(entity.getBlockPos().south()).getBlock() == Blocks.SOUL_FIRE || client.world.getBlockState(entity.getBlockPos().north()).getBlock() == Blocks.SOUL_FIRE || client.world.getBlockState(entity.getBlockPos().east()).getBlock() == Blocks.SOUL_FIRE || client.world.getBlockState(entity.getBlockPos().west()).getBlock() == Blocks.SOUL_FIRE || client.world.getBlockState(entity.getBlockPos().up()).getBlock() == Blocks.SOUL_FIRE || client.world.getBlockState(entity.getBlockPos()).getBlock() == Blocks.SOUL_FIRE) {
                        onFireEntityList.add(entity);
                    }
                }
                if (!entity.isPlayer() && onFireEntityList.contains(entity) && entity instanceof MobEntity && client.world.getBlockState(entity.getBlockPos().south()).getBlock() == Blocks.FIRE || client.world.getBlockState(entity.getBlockPos().north()).getBlock() == Blocks.FIRE || client.world.getBlockState(entity.getBlockPos().east()).getBlock() == Blocks.FIRE || client.world.getBlockState(entity.getBlockPos().west()).getBlock() == Blocks.FIRE || client.world.getBlockState(entity.getBlockPos().up()).getBlock() == Blocks.FIRE || client.world.getBlockState(entity.getBlockPos()).getBlock() == Blocks.FIRE) {
                    onFireEntityList.remove(entity);
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
