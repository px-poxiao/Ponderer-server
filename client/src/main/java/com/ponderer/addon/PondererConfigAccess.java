package com.ponderer.addon;

public final class PondererConfigAccess {

    private PondererConfigAccess() {}

    public static String getApiKey() {
        return getStringValue("AI_API_KEY");
    }

    public static String getProvider() {
        return getStringValue("AI_PROVIDER");
    }

    private static String getStringValue(String fieldName) {
        try {
            Class<?> configClass = Class.forName("com.nododiiiii.ponderer.Config");
            Object configValue = configClass.getField(fieldName).get(null);
            Object value = configValue.getClass().getMethod("get").invoke(configValue);
            return value == null ? "" : value.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }
}
