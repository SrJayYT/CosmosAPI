package com.devpapo.cosmosapi.cosmo;

import java.util.Locale;

public enum CosmosTrigger {
    TIME,
    PLAYER_KILL,
    PLAYER_DEATH,
    BLOCK_BREAK,
    BLOCK_PLACE,
    MOB_KILL,
    TAMED_ANIMAL_DEATH;

    public static CosmosTrigger fromInput(String input) {
        try {
            return valueOf(input.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}