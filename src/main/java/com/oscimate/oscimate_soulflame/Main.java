package com.oscimate.oscimate_soulflame;

import com.google.common.base.Suppliers;
import com.oscimate.oscimate_soulflame.config.ConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.FireBlock;
import net.minecraft.block.SoulFireBlock;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.function.Supplier;


@Environment(EnvType.CLIENT)
public class Main implements ClientModInitializer {



    public static final String MODID = "oscimate_soulflame";
    public static final ConfigManager CONFIG_MANAGER = new ConfigManager();
    public static final Supplier<Sprite> BLANK_FIRE = Suppliers.memoize(() -> new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("oscimate_soulflame:block/blank_fire_1")).getSprite());



    public int getColorInt(int r, int g, int b) {
        return r << 16 | g << 8 | b;
    }


    @Override
    public void onInitializeClient() {

        ModelLoadingRegistry.INSTANCE.registerResourceProvider(rm -> new TutorialModelProvider());

//        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.FIRE, CustomRenderLayer.getCustomTint());
//
//        ColorProviderRegistry.BLOCK.register(((state, world, pos, tintIndex) -> {
//            int value = Color.ORANGE.hashCode();
//
//            if(MinecraftClient.getInstance().world.getBiome(pos).getKey().orElseThrow().equals(BiomeKeys.BASALT_DELTAS)) {
//                value = this.getColorInt(39, 40, 48);
//            } else if (MinecraftClient.getInstance().world.getBiome(pos).getKey().orElseThrow().equals(BiomeKeys.CRIMSON_FOREST)) {
//                value = this.getColorInt(255, 0, 0);
//            } else if (MinecraftClient.getInstance().world.getBiome(pos).getKey().orElseThrow().equals(BiomeKeys.WARPED_FOREST)) {
//                value = this.getColorInt(0, 255, 0);
//            }
//
//            return value;
//        }), Blocks.FIRE);

        if(!this.CONFIG_MANAGER.fileExists()) {
            this.CONFIG_MANAGER.save();
        }
        System.out.println(this.CONFIG_MANAGER.getStartupConfig());
        ClientTickEvents.START_CLIENT_TICK.register((client) -> {
            if (client.world != null) {
                client.world.getEntities().forEach(entity -> {
                    Box box = entity.getBoundingBox();
                    BlockPos blockPos = new BlockPos((int) (box.minX + 0.001), (int) (box.minY + 0.001), (int) (box.minZ + 0.001));
                    BlockPos blockPos2 = new BlockPos((int) (box.maxX - 0.001), (int) (box.maxY - 0.001), (int) (box.maxZ - 0.001));
                    if (entity.getWorld().isRegionLoaded(blockPos, blockPos2)) {
                        BlockPos.Mutable mutable = new BlockPos.Mutable();
                        Boolean isSoulFire = false;
                        outerLoop:
                        for (int i = blockPos.getX(); i <= blockPos2.getX(); ++i) {
                            for (int j = blockPos.getY(); j <= blockPos2.getY(); ++j) {
                                for (int k = blockPos.getZ(); k <= blockPos2.getZ(); ++k) {
                                    mutable.set(i, j, k);
                                    BlockState blockState = entity.getWorld().getBlockState(mutable);
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