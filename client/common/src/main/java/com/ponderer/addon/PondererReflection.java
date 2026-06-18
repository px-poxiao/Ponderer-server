package com.ponderer.addon;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class PondererReflection {

    public static final String CLIENT_COMMANDS = "com.nododiiiii.ponderer.ponder.PondererClientCommands";
    public static final String SCENE_RUNTIME = "com.nododiiiii.ponderer.ponder.SceneRuntime";
    public static final String SCENE_EDITOR_SCREEN = "com.nododiiiii.ponderer.ui.SceneEditorScreen";

    private PondererReflection() {}

    public static Object invokeStatic(String className, String methodName, Object... args) throws ReflectiveOperationException {
        Class<?> owner = Class.forName(className);
        Method method = findMethod(owner, methodName, true, args);
        method.setAccessible(true);
        return method.invoke(null, args);
    }

    public static Object invokeInstance(Object target, String methodName, Object... args) throws ReflectiveOperationException {
        Method method = findMethod(target.getClass(), methodName, false, args);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    public static Object findSceneById(ResourceLocation sceneId) throws ReflectiveOperationException {
        return invokeStatic(SCENE_RUNTIME, "findBySceneId", sceneId);
    }

    public static Object scene(Object match) throws ReflectiveOperationException {
        return invokeInstance(match, "scene");
    }

    public static int sceneIndex(Object match) throws ReflectiveOperationException {
        Object value = invokeInstance(match, "sceneIndex");
        return value instanceof Number number ? number.intValue() : 0;
    }

    public static boolean canModifyScene(Object scene) {
        try {
            Object value = invokeStatic(SCENE_EDITOR_SCREEN, "canModifyScene", scene);
            return !(value instanceof Boolean allowed) || allowed;
        } catch (ReflectiveOperationException ignored) {
            return true;
        }
    }

    public static Screen createEditor(Object scene, int sceneIndex) throws ReflectiveOperationException {
        Class<?> editorClass = Class.forName(SCENE_EDITOR_SCREEN);
        Constructor<?> constructor = findConstructor(editorClass, scene, sceneIndex);
        constructor.setAccessible(true);
        Object editor = constructor.newInstance(scene, sceneIndex);
        return editor instanceof Screen screen ? screen : null;
    }

    private static Method findMethod(Class<?> owner, String name, boolean staticMethod, Object... args)
            throws NoSuchMethodException {
        Class<?> current = owner;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (!method.getName().equals(name)) continue;
                if (Modifier.isStatic(method.getModifiers()) != staticMethod) continue;
                if (matches(method.getParameterTypes(), args)) return method;
            }
            current = current.getSuperclass();
        }

        for (Method method : owner.getMethods()) {
            if (!method.getName().equals(name)) continue;
            if (Modifier.isStatic(method.getModifiers()) != staticMethod) continue;
            if (matches(method.getParameterTypes(), args)) return method;
        }

        throw new NoSuchMethodException(owner.getName() + "." + name + "/" + args.length);
    }

    private static Constructor<?> findConstructor(Class<?> owner, Object... args) throws NoSuchMethodException {
        for (Constructor<?> constructor : owner.getDeclaredConstructors()) {
            if (matches(constructor.getParameterTypes(), args)) return constructor;
        }
        throw new NoSuchMethodException(owner.getName() + " constructor/" + args.length);
    }

    private static boolean matches(Class<?>[] parameterTypes, Object[] args) {
        if (parameterTypes.length != args.length) return false;
        for (int i = 0; i < parameterTypes.length; i++) {
            Object arg = args[i];
            if (arg == null) continue;
            Class<?> parameterType = wrap(parameterTypes[i]);
            if (!parameterType.isInstance(arg)) return false;
        }
        return true;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == char.class) return Character.class;
        return Void.class;
    }
}
