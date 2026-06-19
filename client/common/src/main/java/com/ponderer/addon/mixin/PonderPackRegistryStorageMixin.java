package com.ponderer.addon.mixin;

import com.ponderer.addon.PondererStorageContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;

@Pseudo
@Mixin(targets = "com.nododiiiii.ponderer.ponder.PonderPackRegistry", remap = false)
public class PonderPackRegistryStorageMixin {

    @Shadow
    private static Path registryPath;

    @Inject(method = "load", at = @At("HEAD"), remap = false, require = 0)
    private static void pondererAddon$beforeLoad(CallbackInfo ci) {
        registryPath = PondererStorageContext.registryPath();
    }

    @Inject(method = "save", at = @At("HEAD"), remap = false, require = 0)
    private static void pondererAddon$beforeSave(CallbackInfo ci) {
        registryPath = PondererStorageContext.registryPath();
    }
}
