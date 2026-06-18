package com.ponderer.addon.mixin;

import com.ponderer.addon.PondererAddonConfig;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.createmod.ponder.foundation.ui.PonderUI", remap = false, priority = 900)
public abstract class PondererCanEditMixin {

    @Inject(method = "canEdit", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void pondererAddon$allowSurvivalEdit(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (PondererAddonConfig.isEditButtonEnabled() && player != null) {
            cir.setReturnValue(true);
        }
    }
}
