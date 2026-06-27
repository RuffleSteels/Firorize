package com.oscimate.firorize;

import com.oscimate.firorize.config.ConfigManager;
import com.oscimate.firorize.config.ConfigScreen;
import com.oscimate.firorize.config.render.BlockSceneRenderer;
import com.oscimate.firorize.mixin.fire_overlays.client.FireBlockInvoker;
import com.oscimate.firorize.test.TestModel;
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.phys.AABB;
import org.apache.commons.collections4.map.ListOrderedMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Environment(EnvType.CLIENT)
public class Main implements ClientModInitializer {
    public static final String MODID = "firorize";
    public static final ConfigManager CONFIG_MANAGER = new ConfigManager();
    public static List<TagKey<Block>> blockTagList = new ArrayList<>();
    public static List<ResourceKey<Biome>> biomeKeyList = new ArrayList<>();
    public static boolean inConfig = false;
    private static int[] getNextResolution(int width, int height) {
        double widthScale = Math.ceil((double) width / 1920);
        double heightScale = Math.ceil((double) height / 1080);

        double scale = Math.max(widthScale, heightScale);

        int nextWidth = (int) (1920 * scale);
        int nextHeight = (int) (1080 * scale);

        return new int[]{nextWidth, nextHeight};
    }
    public static void setScale(int width, int height, Minecraft client) {
        int[] stuffs = getNextResolution(client.getWindow().getWidth(), client.getWindow().getHeight());

        int widthh = client.getWindow().getWidth();
        int heightt = client.getWindow().getHeight();

        if (Math.round((float) widthh / 16) < Math.round((float) heightt / 9)) {
            double factor = (double) widthh / stuffs[0] * 2  * ((double) stuffs[0] /1920);
            double nearestInt = Math.round(factor);
            double difference = Math.abs(factor - nearestInt);
            if (difference <= 0.2) factor = nearestInt;

            client.getWindow().setGuiScale((int) factor);
        } else{
            double factor = (double)2*heightt/ stuffs[1] * ((double) stuffs[0] /1920);
            double nearestInt = Math.round(factor);
            double difference = Math.abs(factor - nearestInt);
            if (difference <= 0.2) factor = nearestInt;

            client.getWindow().setGuiScale((int) factor);
        }
    }
    @SuppressWarnings("deprecation") // builtInRegistryHolder().tags() is the supported per-block tag stream
    public static void settingFireColor(Entity entity) {
        AABB box = entity.getBoundingBox();
        int i = Mth.floor(box.minX);
        int j = Mth.ceil(box.maxX);
        int k = Mth.floor(box.minY);
        int l = Mth.ceil(box.maxY);
        int m = Mth.floor(box.minZ);
        int n = Mth.ceil(box.maxZ);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int p = i; p < j; ++p) {
            for (int q = k; q < l; ++q) {
                for (int r = m; r < n; ++r) {
                    mutable.set(p, q, r);
                    Block block = entity.level().getBlockState(mutable).getBlock();
                    if (!((float)q + 1f >= box.minY)) continue;
                    if (block instanceof BaseFireBlock) {
                        final Block blockUnder;
                        if (block instanceof FireBlock) {
                            if (entity.level().getBlockState(mutable).getValue(FireBlock.NORTH)) {
                                blockUnder = entity.level().getBlockState(mutable.north()).getBlock();
                            } else if (entity.level().getBlockState(mutable).getValue(FireBlock.EAST)) {
                                blockUnder = entity.level().getBlockState(mutable.east()).getBlock();
                            } else if (entity.level().getBlockState(mutable).getValue(FireBlock.SOUTH)) {
                                blockUnder = entity.level().getBlockState(mutable.south()).getBlock();
                            } else if (entity.level().getBlockState(mutable).getValue(FireBlock.WEST)) {
                                blockUnder = entity.level().getBlockState(mutable.west()).getBlock();
                            } else if (entity.level().getBlockState(mutable).getValue(FireBlock.UP)) {
                                blockUnder = entity.level().getBlockState(mutable.above()).getBlock();
                            } else {
                                blockUnder = entity.level().getBlockState(mutable.below()).getBlock();
                            }
                        } else {
                            blockUnder = entity.level().getBlockState(mutable.below()).getBlock();
                        }

                        if (!blockUnder.equals(Blocks.AIR)) {
                            ArrayList<ListOrderedMap<String, int[]>> list = CONFIG_MANAGER.getCurrentBlockFireColors().getLeft();
                            if ((blockUnder.builtInRegistryHolder().tags().anyMatch(tag -> Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(1).containsKey(tag.location().toString())) ||
                                    Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(2).containsKey(entity.level().getBiome(mutable).unwrapKey().get().identifier().toString()) ||
                                    list.get(0).containsKey(BuiltInRegistries.BLOCK.getKey(blockUnder).toString()))) {

                                ((RenderFireColorAccessor) entity).firorize$setRenderFireColor(new int[]{2});

                                for (int ii = 0; ii < 3; ii++) {
                                    int order = Main.CONFIG_MANAGER.getPriorityOrder().get(ii);

                                    if (order == 0) {
                                        if (list.get(0).containsKey(BuiltInRegistries.BLOCK.getKey(blockUnder).toString())) {
                                            ((RenderFireColorAccessor) entity).firorize$setRenderFireColor(Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(0).get(BuiltInRegistries.BLOCK.getKey(blockUnder).toString()));
                                            return;
                                        }
                                    } else if (order == 1) {
                                        if (blockUnder.builtInRegistryHolder().tags().anyMatch(tag -> Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(1).containsKey(tag.location().toString()))) {
                                            ListOrderedMap<String, int[]> map = Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(1);
                                            // Match against the block's real tags by id string directly. Cross-version
                                            // profiles may hold tags absent in this version; those simply never match
                                            // a real tag here, so they're ignored without resolving them.
                                            String matchedTag = map.keyList().stream().filter(tag -> blockUnder.builtInRegistryHolder().tags().anyMatch(tagg -> tagg.location().toString().equals(tag))).findFirst().orElse(null);
                                            if (matchedTag != null) {
                                                ((RenderFireColorAccessor) entity).firorize$setRenderFireColor(list.get(1).get(matchedTag).clone());
                                                return;
                                            }
                                        }
                                    } else if (order == 2) {
                                        if (Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(2).containsKey(entity.level().getBiome(mutable).unwrapKey().get().identifier().toString())) {
                                            ((RenderFireColorAccessor) entity).firorize$setRenderFireColor(list.get(2).get(String.valueOf(entity.level().getBiome(mutable).unwrapKey().get().identifier().toString())).clone());
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
        // Ensure the entity always has a non-null colour: some paths above (e.g. a fire block
        // whose block-under is air, or no priority match) can leave it unset, which would NPE the
        // render redirects that dereference firorize$getRenderFireColor()[0].
        if (((RenderFireColorAccessor) entity).firorize$getRenderFireColor() == null) {
            ((RenderFireColorAccessor) entity).firorize$setRenderFireColor(Main.CONFIG_MANAGER.getCurrentBlockFireColors().getRight().clone());
        }
    }

    public static final KeyMapping configKeybind = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("firorze.key.openConfig", InputConstants.Type.KEYSYM, InputConstants.KEY_I, KeyMapping.Category.MISC)
    );

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level != null && client.player != null) {
                if (client.screen == null) {
                    while (configKeybind.consumeClick()) {
                        client.setScreen(new ConfigScreen());
                    }
                }
            }
        });
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
            biomeKeyList = registries.lookupOrThrow(Registries.BIOME).registryKeySet().stream().toList();
            blockTagList = registries.lookupOrThrow(Registries.BLOCK).getTags().filter(named -> named.stream().map(entry2 -> entry2.value()).anyMatch(block -> block.defaultBlockState().isFaceSturdy(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, Direction.UP) || ((FireBlockInvoker)Blocks.FIRE).getBurnChances().containsKey(block))).map(named -> named.key()).toList();
        });
        ModelLoadingPlugin.register(pluginContext -> {
            // 1.21.4+ wraps whole block-state models; the context exposes the BlockState (no more
            // per-sub-model resource ids), so dispatch on the block instead of model paths.
            pluginContext.modifyBlockModelAfterBake().register(ModelModifier.WRAP_PHASE, (model, context) -> {
                Block block = context.state().getBlock();
                if (block == Blocks.FIRE) {
                    return new TestModel(model, false);
                }
                if (block == Blocks.SOUL_FIRE) {
                    return new TestModel(model, true);
                }
                return model;
            });
        });

        PictureInPictureRendererRegistry.register(ctx -> new BlockSceneRenderer(ctx.bufferSource()));

        if(!CONFIG_MANAGER.fileExists()) {
            CONFIG_MANAGER.save();
        }
        CONFIG_MANAGER.getStartupConfig();
    }
}