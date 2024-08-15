package com.oscimate.firorize;

import com.oscimate.firorize.config.ConfigManager;
import com.oscimate.firorize.mixin.fire_overlays.client.FireBlockInvoker;
import com.oscimate.firorize.test.TestModel;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FireBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.biome.Biome;
import org.apache.commons.collections4.map.ListOrderedMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Environment(EnvType.CLIENT)
public class Main implements ClientModInitializer {
    public static final String MODID = "firorize";
    public static final ConfigManager CONFIG_MANAGER = new ConfigManager();
    public static List<TagKey<Block>> blockTagList = null;
    public static List<RegistryKey<Biome>> biomeKeyList = null;
    public static boolean inConfig = false;
    private static int[] getNextResolution(int width, int height) {
        double widthScale = Math.ceil((double) width / 1920);
        double heightScale = Math.ceil((double) height / 1080);

        double scale = Math.max(widthScale, heightScale);

        int nextWidth = (int) (1920 * scale);
        int nextHeight = (int) (1080 * scale);

        return new int[]{nextWidth, nextHeight};
    }
    public static void setScale(int width, int height, MinecraftClient client) {
        int[] stuffs = getNextResolution(client.getWindow().getFramebufferWidth(), client.getWindow().getFramebufferHeight());

        int widthh = client.getWindow().getFramebufferWidth();
        int heightt = client.getWindow().getFramebufferHeight();

        if (Math.round((float) widthh / 16) < Math.round((float) heightt / 9)) {
            double factor = (double) widthh / stuffs[0] * 2  * ((double) stuffs[0] /1920);
            double nearestInt = Math.round(factor);
            double difference = Math.abs(factor - nearestInt);
            if (difference <= 0.2) factor = nearestInt;

            client.getWindow().setScaleFactor(factor);
        } else{
            double factor = (double)2*heightt/ stuffs[1] * ((double) stuffs[0] /1920);
            double nearestInt = Math.round(factor);
            double difference = Math.abs(factor - nearestInt);
            if (difference <= 0.2) factor = nearestInt;

            client.getWindow().setScaleFactor(factor);
        }
    }
    public static void settingFireColor(Entity entity) {
        Box box = entity.getBoundingBox();
        int i = MathHelper.floor(box.minX);
        int j = MathHelper.ceil(box.maxX);
        int k = MathHelper.floor(box.minY);
        int l = MathHelper.ceil(box.maxY);
        int m = MathHelper.floor(box.minZ);
        int n = MathHelper.ceil(box.maxZ);
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int p = i; p < j; ++p) {
            for (int q = k; q < l; ++q) {
                for (int r = m; r < n; ++r) {
                    mutable.set(p, q, r);
                    Block block = entity.getWorld().getBlockState(mutable).getBlock();
                    if (!((float)q + 1f >= box.minY)) continue;
                    if (block instanceof AbstractFireBlock) {
                        final Block blockUnder;
                        if (block instanceof FireBlock) {
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
                        } else {
                            blockUnder = entity.getWorld().getBlockState(mutable.down()).getBlock();
                        }

                        if (!blockUnder.equals(Blocks.AIR)) {
                            ArrayList<ListOrderedMap<String, int[]>> list = CONFIG_MANAGER.getCurrentBlockFireColors().getLeft();
                            if ((blockUnder.getDefaultState().streamTags().anyMatch(tag -> Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(1).containsKey(tag.id().toString())) ||
                                    Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(2).containsKey(entity.getWorld().getBiome(mutable).getKey().get().getValue().toString()) ||
                                    list.get(0).containsKey(Registries.BLOCK.getId(blockUnder).toString()))) {

                                ((RenderFireColorAccessor) entity).firorize$setRenderFireColor(new int[]{2});

                                for (int ii = 0; ii < 3; ii++) {
                                    int order = Main.CONFIG_MANAGER.getPriorityOrder().get(ii);

                                    if (order == 0) {
                                        if (list.get(0).containsKey(Registries.BLOCK.getId(blockUnder).toString())) {
                                            ((RenderFireColorAccessor) entity).firorize$setRenderFireColor(Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(0).get(Registries.BLOCK.getId(blockUnder).toString()));
                                            return;
                                        }
                                    } else if (order == 1) {
                                        if (blockUnder.getDefaultState().streamTags().anyMatch(tag -> Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(1).containsKey(tag.id().toString()))) {
                                            ListOrderedMap<String, int[]> map = Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(1);
                                            List<TagKey<Block>> tags = map.keyList().stream().filter(tag -> blockUnder.getDefaultState().streamTags().map(tagg -> tagg.id().toString()).toList().contains(tag)).map(tag -> Main.blockTagList.stream().filter(tagg -> tagg.id().toString().equals(tag)).findFirst().get()).toList();
                                            ((RenderFireColorAccessor) entity).firorize$setRenderFireColor(list.get(1).get(tags.get(0).id().toString()).clone());
                                            return;
                                        }
                                    } else if (order == 2) {
                                        if (Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(2).containsKey(entity.getWorld().getBiome(mutable).getKey().get().getValue().toString())) {
                                            ((RenderFireColorAccessor) entity).firorize$setRenderFireColor(list.get(2).get(String.valueOf(entity.getWorld().getBiome(mutable).getKey().get().getValue().toString())).clone());
                                            return;
                                        }
                                    }
                                }
                            } else {
                                ((RenderFireColorAccessor) entity).firorize$setRenderFireColor(Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight().clone());
                            }
                        }
                    } else {

                        if (entity.isInLava()) {
                            ((RenderFireColorAccessor) entity).firorize$setRenderFireColor(new int[]{2});
                        }
                        else if (((RenderFireColorAccessor) entity).firorize$getRenderFireColor() == null) {
                            ((RenderFireColorAccessor) entity).firorize$setRenderFireColor(Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight().clone());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onInitializeClient() {
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
            biomeKeyList = registries.get(RegistryKeys.BIOME).getKeys().stream().toList();
            blockTagList = registries.get(RegistryKeys.BLOCK).streamTags().filter(tag -> Registries.BLOCK.getEntryList(tag).get().stream().map(entry2 -> entry2.value()).filter(block -> block.getDefaultState().isSideSolidFullSquare(EmptyBlockView.INSTANCE, BlockPos.ORIGIN, Direction.UP) || ((FireBlockInvoker)Blocks.FIRE).getBurnChances().containsKey(block)).toList().size() > 0).toList();
        });
        ModelLoadingPlugin.register(pluginContext -> {
            pluginContext.modifyModelAfterBake().register(ModelModifier.WRAP_PHASE, (model, context) -> {
                if (context.topLevelId() == null) {
                    if (context.resourceId().getPath().contains("block/fire_side") || context.resourceId().getPath().contains("block/fire_floor") || context.resourceId().getPath().contains("block/fire_up") ) {
                        return new TestModel(model, Integer.parseInt(context.resourceId().getPath().substring(context.resourceId().getPath().length() - 1)), false, context.resourceId().getPath().split("_")[1]);
                    }
                    if (context.resourceId().getPath().contains("block/soul_fire_side") || context.resourceId().getPath().contains("block/soul_fire_floor") || context.resourceId().getPath().contains("block/soul_fire_up") ) {

                        if (Main.inConfig) {
                            return new TestModel(model, Integer.parseInt(context.resourceId().getPath().substring(context.resourceId().getPath().length() - 1)), true, context.resourceId().getPath().split("_")[2]);
                        } else {
                            return new TestModel(model, Integer.parseInt(context.resourceId().getPath().substring(context.resourceId().getPath().length() - 1)), true, context.resourceId().getPath().split("_")[2]);
                        }
                    }
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