package com.oscimate.oscimate_soulflame;

import com.google.common.base.Suppliers;
import com.oscimate.oscimate_soulflame.config.ConfigManager;
import com.oscimate.oscimate_soulflame.mixin.fire_overlays.client.BlockTagAccessor;
import com.oscimate.oscimate_soulflame.test.TestModel;
import it.unimi.dsi.fastutil.Hash;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.blockview.v2.FabricBlockView;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.CustomizeBuffetLevelScreen;
import net.minecraft.client.network.ClientDynamicRegistryType;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.math.*;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import org.apache.commons.collections4.map.ListOrderedMap;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

import static com.oscimate.oscimate_soulflame.CustomRenderLayer.getCustomTint;


@Environment(EnvType.CLIENT)
public class Main implements ClientModInitializer {
    public static final String MODID = "oscimate_soulflame";
    public static final ConfigManager CONFIG_MANAGER = new ConfigManager();
    public static double currentFireHeight = 0.0;
    public static final Supplier<Sprite> BLANK_FIRE_0 = Suppliers.memoize(() -> new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("oscimate_soulflame:block/blank_fire_0")).getSprite());
    public static final Supplier<Sprite> BLANK_FIRE_0_OVERLAY = Suppliers.memoize(() -> new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("oscimate_soulflame:block/blank_fire_overlay_0")).getSprite());
    public static final Supplier<Sprite> SOUL_FIRE_1 = Suppliers.memoize(() -> new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, new Identifier("block/soul_fire_1")).getSprite());
    public static final Supplier<Sprite> SOUL_FIRE_0 = Suppliers.memoize(() -> new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, new Identifier("block/soul_fire_0")).getSprite());
    public static final Supplier<Sprite> BLANK_FIRE_1 = Suppliers.memoize(() -> new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("oscimate_soulflame:block/blank_fire_1")).getSprite());
    public static final Supplier<Sprite> BLANK_FIRE_1_OVERLAY = Suppliers.memoize(() -> new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("oscimate_soulflame:block/blank_fire_overlay_1")).getSprite());
    public static final Supplier<Sprite> ARROW_RIGHT = Suppliers.memoize(() -> new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("oscimate_soulflame:block/arrow_right")).getSprite());
    public static final Supplier<Sprite> ARROW_LEFT = Suppliers.memoize(() -> new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("oscimate_soulflame:block/arrow_left")).getSprite());
    public static final Supplier<Sprite> UNDO = Suppliers.memoize(() -> new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("oscimate_soulflame:block/undo")).getSprite());
    public static List<TagKey<Block>> blockTagList = null;
    public static List<RegistryKey<Biome>> biomeKeyList = null;

    public static void settingFireColor(Entity entity) {
        Box box = entity.getBoundingBox();
        int i = MathHelper.floor(box.minX);
        int j = MathHelper.ceil(box.maxX);
        int k = MathHelper.floor(box.minY);
        int l = MathHelper.ceil(box.maxY);
        int m = MathHelper.floor(box.minZ);
        int n = MathHelper.ceil(box.maxZ);
        boolean bl2 = false;
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int p = i; p < j; ++p) {
            for (int q = k; q < l; ++q) {
                for (int r = m; r < n; ++r) {
                    double e;
                    mutable.set(p, q, r);
                    Block block = entity.getWorld().getBlockState(mutable).getBlock();
                    Block blockUnder = entity.getWorld().getBlockState(mutable.down()).getBlock();
                    if (!((float)q + 1f >= box.minY)) continue;
                    if (!entity.isInLava()) {
                        if (block instanceof FireBlock) {
                            if (Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(0).containsKey(blockUnder.getTranslationKey())) {
                                ((RenderFireColorAccessor) entity).setRenderFireColor(Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(0).get(blockUnder.getTranslationKey()));
                            } else if (blockUnder.getDefaultState().streamTags().anyMatch(tag -> Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(1).containsKey(tag.id().toString()))) {
                                ListOrderedMap<String, int[]> map = Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(1);
                                List<TagKey<Block>> tags = map.keyList().stream().filter(tag -> blockUnder.getDefaultState().streamTags().map(tagg -> tagg.id().toString()).toList().contains(tag)).map(BlockTagAccessor::callOf).toList();
                                int[] colors = map.get(tags.get(0).id().toString()).clone();
                                ((RenderFireColorAccessor) entity).setRenderFireColor(colors);
                            }

                        } else {
                            if (block instanceof SoulFireBlock) {
                                ((RenderFireColorAccessor) entity).setRenderFireColor(new int[]{1});
                            } else if (block instanceof FireBlock){
                                ((RenderFireColorAccessor) entity).setRenderFireColor(new int[]{2});
                            } else {
                                if(((RenderFireColorAccessor) entity).getRenderFireColor() == null) {
                                    ((RenderFireColorAccessor) entity).setRenderFireColor(new int[]{2});
                                }
                            }
                        }
                    } else {
                        ((RenderFireColorAccessor) entity).setRenderFireColor(new int[]{2});
                    }
                }
            }
        }
    }
    public int getColorInt(int r, int g, int b) {
        return r << 16 | g << 8 | b;
    }
    @Override
    public void onInitializeClient() {
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
            biomeKeyList = registries.get(RegistryKeys.BIOME).getKeys().stream().toList();
            blockTagList = registries.get(RegistryKeys.BLOCK).streamTags().filter(tag -> Registries.BLOCK.getEntryList(tag).get().stream().map(entry2 -> entry2.value()).filter(block -> block.getDefaultState().isSideSolidFullSquare(EmptyBlockView.INSTANCE, BlockPos.ORIGIN, Direction.UP)).toList().size() > 0).toList();
        });
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.FIRE, getCustomTint());
        ModelLoadingPlugin.register(pluginContext -> {
            pluginContext.modifyModelAfterBake().register(ModelModifier.WRAP_PHASE, (model, context) -> {
                if (context.id().getPath().contains("block/fire_side") || context.id().getPath().contains("block/fire_floor") || context.id().getPath().contains("block/fire_up") ) {
                    return new TestModel(model);
                }
                return model;
            });
        });
        ColorProviderRegistry.BLOCK.register(((state, world, pos, tintIndex) -> {
            if (world.hasBiomes()) {
                ArrayList<ListOrderedMap<String, int[]>> list = CONFIG_MANAGER.getCurrentBlockFireColors();

                Block blockUnder = world.getBlockState(pos.down()).getBlock();
                for (int i = 0; i < 3; i++) {
                    int order = Main.CONFIG_MANAGER.getPriorityOrder().get(i);
                    if (order == 0) {
                        if (list.get(0).containsKey(Registries.BLOCK.getId(blockUnder).toString())) {
                            int[] colors = list.get(0).get(Registries.BLOCK.getId(blockUnder).toString()).clone();
                            if (tintIndex == 1) {
                                return colors[0];
                            }
                            if (tintIndex == 2) {
                                return colors[1];
                            }
                        }
                    } else if (order == 1) {
                        if (blockUnder.getDefaultState().streamTags().anyMatch(tag -> Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(1).containsKey(tag.id().toString()))) {
                            ListOrderedMap<String, int[]> map = Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(1);
                            List<TagKey<Block>> tags = map.keyList().stream().filter(tag -> blockUnder.getDefaultState().streamTags().map(tagg -> tagg.id().toString()).toList().contains(tag)).map(BlockTagAccessor::callOf).toList();
                            int[] colors = list.get(1).get(tags.get(0).id().toString()).clone();

                            if (tintIndex == 1) {
                                return colors[0];
                            }
                            if (tintIndex == 2) {
                                return colors[1];
                            }

                        }
                    } else if (order == 2) {
                        if (Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(2).containsKey(world.getBiomeFabric(pos).getKey().get().getValue().toString())) {
                            int[] colors = list.get(2).get(world.getBiomeFabric(pos).getKey().get().getValue().toString()).clone();
                            if (tintIndex == 1) {
                                return colors[0];
                            }
                            if (tintIndex == 2) {
                                return colors[1];
                            }
                        }
                    }
                }

            }
            return this.getColorInt(0, 0, 0);
        }), Blocks.FIRE);

        if(!CONFIG_MANAGER.fileExists()) {
            CONFIG_MANAGER.save();
        }
        CONFIG_MANAGER.getStartupConfig();
    }
}