package com.oscimate.oscimate_soulflame;

import com.google.common.base.Suppliers;
import com.oscimate.oscimate_soulflame.config.ConfigManager;

import com.oscimate.oscimate_soulflame.mixin.fire_overlays.client.FireBlockInvoker;
import com.oscimate.oscimate_soulflame.test.TestModel;
import com.oscimate.oscimate_soulflame.test.TestierModel;
import it.unimi.dsi.fastutil.Hash;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.blockview.v2.FabricBlockView;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.BakedModelManagerHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.client.rendering.v1.AtlasSourceTypeRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.impl.client.rendering.AtlasSourceTypeRegistryImpl;
import net.fabricmc.fabric.mixin.client.model.loading.ModelLoaderBakerImplMixin;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.CustomizeBuffetLevelScreen;
import net.minecraft.client.network.ClientDynamicRegistryType;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.resource.DefaultClientResourcePackProvider;
import net.minecraft.client.texture.*;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.trim.ArmorTrimPattern;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.resource.Resource;
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
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

import static com.oscimate.oscimate_soulflame.CustomRenderLayer.getCustomTint;


@Environment(EnvType.CLIENT)
public class Main implements ClientModInitializer {
    public static final String MODID = "oscimate_soulflame";

    public static final ConfigManager CONFIG_MANAGER = new ConfigManager();
    public static List<TagKey<Block>> blockTagList = null;
    public static List<RegistryKey<Biome>> biomeKeyList = null;
    public static boolean inConfig = false;


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
//                    Block blockUnder = entity.getWorld().getBlockState(mutable.down()).getBlock();
                    if (!((float)q + 1f >= box.minY)) continue;
                    if (!entity.isInLava()) {
                        if (block instanceof FireBlock) {
                            final Block blockUnder;
                            if (entity.getWorld().getBlockState(mutable).get(FireBlock.NORTH)) {
                                blockUnder = entity.getWorld().getBlockState(mutable.north()).getBlock();
                            } else if (entity.getWorld().getBlockState(mutable).get(FireBlock.EAST)) {
                                blockUnder = entity.getWorld().getBlockState(mutable.east()).getBlock();
                            } else if (entity.getWorld().getBlockState(mutable).get(FireBlock.SOUTH)) {
                                blockUnder = entity.getWorld().getBlockState(mutable.south()).getBlock();
                            } else if (entity.getWorld().getBlockState(mutable).get(FireBlock.WEST)) {
                                blockUnder = entity.getWorld().getBlockState(mutable.west()).getBlock();
                            } else if (entity.getWorld().getBlockState(mutable).get(FireBlock.UP)) {
                                blockUnder = entity.getWorld().getBlockState(mutable.up()).getBlock();
                            } else {
                                blockUnder = entity.getWorld().getBlockState(mutable.down()).getBlock();
                            }
                            ArrayList<ListOrderedMap<String, int[]>> list = CONFIG_MANAGER.getCurrentBlockFireColors();
                            ((RenderFireColorAccessor) entity).setRenderFireColor(new int[]{2});

                            for (int ii = 0; ii < 3; ii++) {
                                int order = Main.CONFIG_MANAGER.getPriorityOrder().get(ii);

                                if (order == 0) {

                                    if (list.get(0).containsKey(Registries.BLOCK.getId(blockUnder).toString())) {
                                        ((RenderFireColorAccessor) entity).setRenderFireColor(Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(0).get(Registries.BLOCK.getId(blockUnder).toString()));

                                        break;
                                    }
                                } else if (order == 1) {

                                    if (blockUnder.getDefaultState().streamTags().anyMatch(tag -> Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(1).containsKey(tag.id().toString()))) {
                                        ListOrderedMap<String, int[]> map = Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(1);
                                        List<TagKey<Block>> tags = map.keyList().stream().filter(tag -> blockUnder.getDefaultState().streamTags().map(tagg -> tagg.id().toString()).toList().contains(tag)).map(BlockTags::of).toList();

                                        ((RenderFireColorAccessor) entity).setRenderFireColor(list.get(1).get(tags.get(0).id().toString()).clone());
                                        break;
                                    }
                                } else if (order == 2) {
                                    if (entity.getWorld().getBiome(mutable) != null && Main.CONFIG_MANAGER.getCurrentBlockFireColors().get(2).containsKey(entity.getWorld().getBiome(mutable).getKey().get().getValue().toString())) {
                                        ((RenderFireColorAccessor) entity).setRenderFireColor(list.get(2).get(entity.getWorld().getBiome(mutable).getKey().get().getValue().toString()).clone());

                                        break;
                                    }
                                }
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

    int counter = 0;
    @Override
    public void onInitializeClient() {


//        ClientTickEvents.END_CLIENT_TICK.register(client -> {
//            if (counter == 0) {
//                if (client.world != null) {
//                    counter++;
//                    ARROW_RIGHT = new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("oscimate_soulflame:block/arrow_right")).getSprite();
//                    ARROW_LEFT = new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("oscimate_soulflame:block/arrow_left")).getSprite();
//                    BLANK_FIRE_0 = new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("oscimate_soulflame:block/blank_fire_0")).getSprite();
//                    BLANK_FIRE_0_OVERLAY = new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("oscimate_soulflame:block/blank_fire_overlay_0")).getSprite();
//                    SOUL_FIRE_1 = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, new Identifier("block/soul_fire_1")).getSprite();
//                    SOUL_FIRE_0 = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, new Identifier("block/soul_fire_0")).getSprite();
//                    UNDO = new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("oscimate_soulflame:block/undo")).getSprite();
//                }
//            }
//        });

        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
            biomeKeyList = registries.get(RegistryKeys.BIOME).getKeys().stream().toList();
            blockTagList = registries.get(RegistryKeys.BLOCK).streamTags().filter(tag -> Registries.BLOCK.getEntryList(tag).get().stream().map(entry2 -> entry2.value()).filter(block -> block.getDefaultState().isSideSolidFullSquare(EmptyBlockView.INSTANCE, BlockPos.ORIGIN, Direction.UP)).toList().size() > 0).toList();
        });
        ModelLoadingPlugin.register(pluginContext -> {
            pluginContext.modifyModelAfterBake().register(ModelModifier.WRAP_PHASE, (model, context) -> {
                if (context.id().getPath().contains("block/fire_side") || context.id().getPath().contains("block/fire_floor") || context.id().getPath().contains("block/fire_up") ) {
                    return new TestModel(model, Integer.parseInt(context.id().getPath().substring(context.id().getPath().length() - 1)));
                }
                if (context.id().getPath().contains("block/soul_fire_side") || context.id().getPath().contains("block/soul_fire_floor") || context.id().getPath().contains("block/soul_fire_up") ) {
                    return new TestierModel(model);
                }
                return model;
            });
        });

        if(!CONFIG_MANAGER.fileExists()) {
            CONFIG_MANAGER.save();
        }
        CONFIG_MANAGER.getStartupConfig();
    }
}