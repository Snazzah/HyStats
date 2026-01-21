package com.snazzah.hystats;

import com.hypixel.hytale.common.util.FormatUtil;

import javax.annotation.Nonnull;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

public record CustomStatConfig(String identifier, String displayName, DisplayFormat format) {
    public enum DisplayFormat {
        NUMBER,
        TIME,
        DISTANCE
    }

    public CustomStatConfig(@Nonnull String identifier, @Nonnull String displayName, @Nonnull DisplayFormat format) {
        this.identifier = identifier;
        this.displayName = displayName;
        this.format = format;
    }

    @Nonnull
    public String formatValue(long value) {
        switch (format) {
            case TIME:
                // Use Hytale's FormatUtil for time formatting (value in seconds)
                return FormatUtil.timeUnitToString(value, TimeUnit.SECONDS);
            case DISTANCE:
                // Assume value is in centimeters, convert to meters with 1 decimal place
                DecimalFormat df = new DecimalFormat("#,##0.0");
                return df.format(value / 100.0) + "m";
            case NUMBER:
            default:
                // Format as whole number with comma separators
                DecimalFormat nf = new DecimalFormat("#,###");
                return nf.format(value);
        }
    }

    @Override
    @Nonnull
    public String identifier() {
        return identifier;
    }

    @Override
    @Nonnull
    public String displayName() {
        return displayName;
    }

    @Override
    @Nonnull
    public DisplayFormat format() {
        return format;
    }
}
