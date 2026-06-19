package com.ponderer.addon.mixin;

import com.ponderer.addon.PondererStorageContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;

@Pseudo
@Mixin(targets = "com.nododiiiii.ponderer.ponder.SceneStore", remap = false)
public class SceneStoreStorageMixin {

    @Inject(method = "getSceneDir", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void pondererAddon$getSceneDir(CallbackInfoReturnable<Path> cir) {
        cir.setReturnValue(PondererStorageContext.sceneDir());
    }

    @Inject(method = "getStructureDir", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void pondererAddon$getStructureDir(CallbackInfoReturnable<Path> cir) {
        cir.setReturnValue(PondererStorageContext.structureDir());
    }
}
