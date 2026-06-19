package com.ponderer.addon.mixin;

import com.ponderer.addon.PondererStorageContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;

@Pseudo
@Mixin(targets = "com.nododiiiii.ponderer.ai.AiSceneGenerator", remap = false)
public class AiSceneGeneratorStorageMixin {

    @Inject(method = "getPromptsDir", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void pondererAddon$getPromptsDir(CallbackInfoReturnable<Path> cir) {
        cir.setReturnValue(PondererStorageContext.promptsDir());
    }

    @Inject(method = "getLogsDir", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void pondererAddon$getLogsDir(CallbackInfoReturnable<Path> cir) {
        cir.setReturnValue(PondererStorageContext.logsDir());
    }
}
