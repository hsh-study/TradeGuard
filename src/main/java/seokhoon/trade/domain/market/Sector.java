package seokhoon.trade.domain.market;

import java.time.Instant;
import java.util.Objects;

public record Sector(
        Long id,
        String sectorCode,
        String sectorName,
        SectorType sectorType,
        Instant createdAt,
        Instant updatedAt
) {
    public Sector {
        requireText(sectorCode, "sectorCode");
        requireText(sectorName, "sectorName");
        Objects.requireNonNull(sectorType, "sectorType");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
