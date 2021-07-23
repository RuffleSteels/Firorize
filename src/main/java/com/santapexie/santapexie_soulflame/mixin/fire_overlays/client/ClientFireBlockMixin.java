package com.santapexie.santapexie_soulflame.mixin.fire_overlays.client;

import com.santapexie.santapexie_soulflame.Main;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.FireBlock;
import net.minecraft.block.SoulFireBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Environment(EnvType.CLIENT)
@Mixin(FireBlock.class)
public class ClientFireBlockMixin extends AbstractFireBlock {
    public ClientFireBlockMixin(Settings settings, float damage) {
        super(settings, damage);
    }

    @Shadow
    @Override
    protected boolean isFlammable(BlockState state) {
        return true;
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if(entity instanceof PlayerEntity) {
            Main.shouldBeRenderingPlayer = false;
        }
        if (entity instanceof MobEntity && Main.onFireEntityList.contains(entity)) {
            Main.onFireEntityList.remove(entity);
        }
        super.onEntityCollision(state, world, pos, entity);
    }
}
