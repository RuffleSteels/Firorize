package com.santapexie.santapexie_soulflame.mixin.fire_overlays.client;

import com.santapexie.santapexie_soulflame.Main;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(at = @At("HEAD"), method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V")
    private void autoswitch$disconnectEvent(Screen screen, CallbackInfo ci) {
        Main.onFireEntityList.clear();
    }
}
