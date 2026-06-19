package com.ponderer.addon.mixin;

import com.ponderer.addon.PondererStorageContext;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftStorageContextMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void pondererAddon$refreshStorageContext(CallbackInfo ci) {
        PondererStorageContext.reloadPondererIfContextChanged();
    }
}
