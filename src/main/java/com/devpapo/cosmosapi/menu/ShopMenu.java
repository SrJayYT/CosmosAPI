package com.devpapo.cosmosapi.menu;

public final class ShopMenu {
    private final String id;
    private final String name;
    private final String type;
    private final int size;
    private final String displayName;

    public ShopMenu(String id, String name, String type, int size, String displayName) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.size = size;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public int getSize() {
        return size;
    }

    public String getDisplayName() {
        return displayName;
    }
}