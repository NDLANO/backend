/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import java.util.Collection;
import java.util.Optional;

public class GradeAverage {
    public GradeAverage(int averageSum, int count) {
        this.averageSum = averageSum;
        this.count = count;
    }

    int averageSum;
    int count;

    public static GradeAverage fromGrades(Collection<Optional<Grade>> grades) {
        var existing = grades.stream().flatMap(Optional::stream).toList();
        var count = existing.size();
        var sum = existing.stream().mapToInt(Grade::toInt).sum();
        return new GradeAverage(sum, count);
    }

    public int getAverageSum() {
        return averageSum;
    }

    public int getCount() {
        return count;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        GradeAverage that = (GradeAverage) obj;
        return averageSum == that.averageSum && count == that.count;
    }

    @Override
    public String toString() {
        return "GradeAverage{averageSum=" + averageSum + ", count=" + count + '}';
    }

    public double getAverageValue() {
        return (double) averageSum / count;
    }
}
