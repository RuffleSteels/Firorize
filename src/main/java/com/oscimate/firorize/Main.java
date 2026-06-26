package com.oscimate.firorize;

import com.oscimate.firorize.config.ConfigManager;
import com.oscimate.firorize.config.ConfigScreen;
import com.oscimate.firorize.config.render.BlockSceneRenderer;
import com.oscimate.firorize.mixin.fire_overlays.client.FireBlockInvoker;
import com.oscimate.firorize.test.TestModel;
import net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FireBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
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
    public static List<TagKey<Block>> blockTagList = new ArrayList<>();
    public static List<RegistryKey<Biome>> biomeKeyList = new ArrayList<>();
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

            client.getWindow().setScaleFactor((int) factor);
        } else{
            double factor = (double)2*heightt/ stuffs[1] * ((double) stuffs[0] /1920);
            double nearestInt = Math.round(factor);
            double difference = Math.abs(factor - nearestInt);
            if (difference <= 0.2) factor = nearestInt;

            client.getWindow().setScaleFactor((int) factor);
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
                    Block block = entity.getEntityWorld().getBlockState(mutable).getBlock();
                    if (!((float)q + 1f >= box.minY)) continue;
                    if (block instanceof AbstractFireBlock) {
                        final Block blockUnder;
                        if (block instanceof FireBlock) {
                            if (entity.getEntityWorld().getBlockState(mutable).get(FireBlock.NORTH)) {
                                blockUnder = entity.getEntityWorld().getBlockState(mutable.north()).getBlock();
                            } else if (entity.getEntityWorld().getBlockState(mutable).get(FireBlock.EAST)) {
                                blockUnder = entity.getEntityWorld().getBlockState(mutable.east()).getBlock();
                            } else if (entity.getEntityWorld().getBlockState(mutable).get(FireBlock.SOUTH)) {
                                blockUnder = entity.getEntityWorld().getBlockState(mutable.south()).getBlock();
                            } else if (entity.getEntityWorld().getBlockState(mutable).get(FireBlock.WEST)) {
                                blockUnder = entity.getEntityWorld().getBlockState(mutable.west()).getBlock();
                            } else if (entity.getEntityWorld().getBlockState(mutable).get(FireBlock.UP)) {
                                blockUnder = entity.getEntityWorld().getBlockState(mutable.up()).getBlock();
                            } else {
                                blockUnder = entity.getEntityWorld().getBlockState(mutable.down()).getBlock();
                            }
                        } else {
                            blockUnder = entity.getEntityWorld().getBlockState(mutable.down()).getBlock();
                        }

                        if (!blockUnder.equals(Blocks.AIR)) {
                            ArrayList<ListOrderedMap<String, int[]>> list = CONFIG_MANAGER.getCurrentBlockFireColors().getLeft();
                            if ((blockUnder.getDefaultState().streamTags().anyMatch(tag -> Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(1).containsKey(tag.id().toString())) ||
                                    Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(2).containsKey(entity.getEntityWorld().getBiome(mutable).getKey().get().getValue().toString()) ||
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
                                            // Match against the block's real tags by id string directly. Cross-version
                                            // profiles may hold tags absent in this version; those simply never match
                                            // a real tag here, so they're ignored without resolving them.
                                            String matchedTag = map.keyList().stream().filter(tag -> blockUnder.getDefaultState().streamTags().anyMatch(tagg -> tagg.id().toString().equals(tag))).findFirst().orElse(null);
                                            if (matchedTag != null) {
                                                ((RenderFireColorAccessor) entity).firorize$setRenderFireColor(list.get(1).get(matchedTag).clone());
                                                return;
                                            }
                                        }
                                    } else if (order == 2) {
                                        if (Main.CONFIG_MANAGER.getCurrentBlockFireColors().getLeft().get(2).containsKey(entity.getEntityWorld().getBiome(mutable).getKey().get().getValue().toString())) {
                                            ((RenderFireColorAccessor) entity).firorize$setRenderFireColor(list.get(2).get(String.valueOf(entity.getEntityWorld().getBiome(mutable).getKey().get().getValue().toString())).clone());
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

    public static final KeyBinding configKeybind = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("firorze.key.openConfig", InputUtil.Type.KEYSYM, InputUtil.GLFW_KEY_I, KeyBinding.Category.MISC)
    );

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != null && client.player != null) {
                if (client.currentScreen == null) {
                    while (configKeybind.isPressed()) {
                        client.setScreen(new ConfigScreen());
                    }
                }
            }
        });
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
            biomeKeyList = registries.getOrThrow(RegistryKeys.BIOME).getKeys().stream().toList();
            blockTagList = registries.getOrThrow(RegistryKeys.BLOCK).streamTags().filter(named -> named.stream().map(entry2 -> entry2.value()).anyMatch(block -> block.getDefaultState().isSideSolidFullSquare(EmptyBlockView.INSTANCE, BlockPos.ORIGIN, Direction.UP) || ((FireBlockInvoker)Blocks.FIRE).getBurnChances().containsKey(block))).map(named -> named.getTag()).toList();
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

        SpecialGuiElementRegistry.register(ctx -> new BlockSceneRenderer(ctx.vertexConsumers()));

        if(!CONFIG_MANAGER.fileExists()) {
            CONFIG_MANAGER.save();
        }
        CONFIG_MANAGER.getStartupConfig();
    }
}