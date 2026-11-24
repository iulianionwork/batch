package spring.batch.example.data;

import java.time.LocalDate;

public record TradeDbRecord(
        String direction,
        LocalDate date,
        String country,
        String commodity,
        String transportMode,
        String measure,
        Long value,
        Long cumulative
) {
}
