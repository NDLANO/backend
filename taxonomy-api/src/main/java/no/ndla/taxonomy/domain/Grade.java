/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum Grade {
    One(1),
    Two(2),
    Three(3),
    Four(4),
    Five(5);

    private final int value;

    Grade(int value) {
        this.value = value;
    }

    @JsonValue
    public int toInt() {
        return value;
    }

    public static Grade fromInt(Integer value) {
        return switch (value) {
            case 1 -> One;
            case 2 -> Two;
            case 3 -> Three;
            case 4 -> Four;
            case 5 -> Five;
            default -> throw new IllegalArgumentException("Unexpected grade value: " + value + ". Must be 1-5.");
        };
    }
}
