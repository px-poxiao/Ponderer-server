package com.ponderer.addon.mixin;

import com.nododiiiii.ponderer.ponder.SceneRuntime;
import com.ponderer.addon.PondererAddonConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

@Pseudo
@Mixin(targets = "net.createmod.ponder.foundation.ui.PonderUI", remap = false)
public abstract class PonderUiEditButtonMixin extends Screen {

    @Unique
    private Button pondererAddon$editButton;

    protected PonderUiEditButtonMixin() {
        super(CommonComponents.EMPTY);
    }

    @Inject(method = "init", at = @At("TAIL"), remap = false, require = 0)
    private void pondererAddon$addEditButton(CallbackInfo ci) {
        if (!PondererAddonConfig.isEditButtonEnabled()) {
            return;
        }

        ResourceLocation activeId = pondererAddon$getActiveSceneId();
        if (activeId == null || SceneRuntime.findBySceneId(activeId) == null) {
            return;
        }

        int y = this.height - 54;
        Button editButton = Button.builder(Component.literal("Edit"), button -> {
            ResourceLocation currentId = pondererAddon$getActiveSceneId();
            if (currentId == null) return;
            var match = SceneRuntime.findBySceneId(currentId);
            if (match != null) {
                pondererAddon$openEditor(match.scene(), match.sceneIndex());
            }
        }).bounds(this.width - 86, y, 48, 20).build();

        pondererAddon$editButton = editButton;
        addRenderableWidget(editButton);
    }

    @Unique
    private ResourceLocation pondererAddon$getActiveSceneId() {
        try {
            Method getActiveScene = ((Object) this).getClass().getMethod("getActiveScene");
            Object scene = getActiveScene.invoke(this);
            if (scene == null) return null;
            Method getId = scene.getClass().getMethod("getId");
            Object id = getId.invoke(scene);
            return id instanceof ResourceLocation resourceLocation ? resourceLocation : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    @Unique
    private void pondererAddon$openEditor(Object scene, int sceneIndex) {
        try {
            Class<?> dslSceneClass = Class.forName("com.nododiiiii.ponderer.ponder.DslScene");
            Class<?> editorClass = Class.forName("com.nododiiiii.ponderer.ui.SceneEditorScreen");
            Object editor = editorClass.getConstructor(dslSceneClass, int.class).newInstance(scene, sceneIndex);
            if (editor instanceof Screen screen) {
                Minecraft.getInstance().setScreen(screen);
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
