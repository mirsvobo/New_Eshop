package org.example.model;
public enum LayerType {
    LAZURE("Afromorsia"),
    ROOF_COLOR("Antracit");
    private final String defaultOptionName;
    LayerType(String defaultOptionName) {
        this.defaultOptionName = defaultOptionName;
    }
    public String getDefaultOptionName() {
        return defaultOptionName;
    }
    public boolean isDefaultOption(String optionName) {
        return optionName != null
                && defaultOptionName.equalsIgnoreCase(optionName.trim());
    }
}
