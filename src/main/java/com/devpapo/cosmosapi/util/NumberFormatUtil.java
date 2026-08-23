package com.devpapo.cosmosapi.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class NumberFormatUtil {
    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1_000L);
    private static final String[] SUFFIXES = {"", "k", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc"};

    private NumberFormatUtil() {
    }

    public static String format(long value) {
        BigDecimal number = BigDecimal.valueOf(value);
        boolean negative = number.signum() < 0;
        BigDecimal absolute = number.abs();
        int suffixIndex = 0;

        while (absolute.compareTo(THOUSAND) >= 0 && suffixIndex < SUFFIXES.length - 1) {
            absolute = absolute.divide(THOUSAND);
            suffixIndex++;
        }

        int decimalPlaces = absolute.compareTo(BigDecimal.TEN) < 0 ? 2 : absolute.compareTo(BigDecimal.valueOf(100L)) < 0 ? 1 : 0;
        String formatted = absolute.setScale(decimalPlaces, RoundingMode.DOWN).stripTrailingZeros().toPlainString().replace('.', ',');
        return (negative ? "-" : "") + formatted + SUFFIXES[suffixIndex];
    }
}