package com.devpapo.cosmosapi.cosmo;

import java.util.Locale;

public enum TimeRewardUnit {
    MINUTE(60_000L),
    DAY(86_400_000L),
    WEEK(604_800_000L),
    MONTH(2_592_000_000L),
    YEAR(31_536_000_000L);

    private final long milliseconds;

    TimeRewardUnit(long milliseconds) {
        this.milliseconds = milliseconds;
    }

    public long toMilliseconds(long amount) {
        return Math.multiplyExact(amount, milliseconds);
    }

    public static TimeRewardUnit fromInput(String input) {
        if (input == null) {
            return null;
        }
        switch (input.toLowerCase(Locale.ROOT)) {
            case "minute":
            case "minutes":
            case "minuto":
            case "minutos":
                return MINUTE;
            case "day":
            case "days":
            case "dia":
            case "dias":
            case "día":
            case "días":
                return DAY;
            case "week":
            case "weeks":
            case "semana":
            case "semanas":
                return WEEK;
            case "month":
            case "months":
            case "mes":
            case "meses":
                return MONTH;
            case "year":
            case "years":
            case "ano":
            case "anos":
            case "año":
            case "años":
                return YEAR;
            default:
                return null;
        }
    }
}