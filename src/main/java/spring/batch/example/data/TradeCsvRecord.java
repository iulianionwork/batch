package spring.batch.example.data;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;

public record TradeCsvRecord(
        @NotNull String direction,
        @NotNull Integer year,
        @NotNull LocalDate date,
        @NotNull String weekday,
        @NotNull String country,
        @NotNull String commodity,
        @NotNull String transportMode,
        @NotNull String measure,
        @NotNull Long value,
        @NotNull Long cumulative
) {
}