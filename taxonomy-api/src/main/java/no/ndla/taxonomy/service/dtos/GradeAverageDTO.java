/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Optional;
import no.ndla.taxonomy.domain.Node;

@Schema(
        name = "GradeAverage",
        requiredProperties = {"averageValue", "count"})
public class GradeAverageDTO {
    public GradeAverageDTO(double averageValue, int count) {
        this.averageValue = averageValue;
        this.count = count;
    }

    @JsonProperty
    double averageValue;

    @JsonProperty
    int count;

    public static double roundToSingleDecimal(double value) {
        return (double) Math.round(value * 10) / 10;
    }

    public static Optional<GradeAverageDTO> fromNode(Node node) {
        return node.getChildQualityEvaluationAverage().map(ga -> {
            var avg = (double) ga.getAverageSum() / ga.getCount();
            var roundedAvg = roundToSingleDecimal(avg);
            var count1 = ga.getCount();
            return new GradeAverageDTO(roundedAvg, count1);
        });
    }

    public double getAverageValue() {
        return averageValue;
    }

    public int getCount() {
        return count;
    }
}
