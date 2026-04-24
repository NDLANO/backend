/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import jakarta.persistence.AttributeConverter;

public class GradeConverter implements AttributeConverter<Grade, Integer> {
    @Override
    public Integer convertToDatabaseColumn(Grade grade) {
        if (grade == null) return null;
        return grade.toInt();
    }

    @Override
    public Grade convertToEntityAttribute(Integer integer) {
        if (integer == null) return null;
        return Grade.fromInt(integer);
    }
}
