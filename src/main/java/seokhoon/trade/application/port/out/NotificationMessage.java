package seokhoon.trade.application.port.out;

import java.time.Instant;
import java.util.Objects;

public record NotificationMessage(
        String title,
        String body,
        Instant createdAt
) {
    public NotificationMessage {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("body must not be blank");
        }
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
