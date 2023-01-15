package io.prometheus.metrics.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class FixedBuckets implements Iterable<FixedBucket> {

    private final double[] upperBounds;
    private final long[] cumulativeCounts;

    private FixedBuckets(double[] upperBounds, long[] cumulativeCounts) {
        this.upperBounds = upperBounds;
        this.cumulativeCounts = cumulativeCounts;
    }

    public static FixedBuckets of(List<Double> upperBounds, List<Long> cumulativeCounts) {
        double[] upperBoundsCopy = new double[upperBounds.size()];
        for (int i=0; i<upperBounds.size(); i++) {
            upperBoundsCopy[i] = upperBounds.get(i);
        }
        long[] cumulativeCountsCopy = new long[cumulativeCounts.size()];
        for (int i=0; i<cumulativeCounts.size(); i++) {
            cumulativeCountsCopy[i] = cumulativeCounts.get(i);
        }
        sortAndValidate(upperBoundsCopy, cumulativeCountsCopy);
        return new FixedBuckets(upperBoundsCopy, cumulativeCountsCopy);
    }

    public static FixedBuckets of(double[] upperBounds, long[] cumulativeCounts) {
        double[] upperBoundsCopy = Arrays.copyOf(upperBounds, upperBounds.length);
        long[] cumulativeCountsCopy = Arrays.copyOf(cumulativeCounts, cumulativeCounts.length);
        sortAndValidate(upperBoundsCopy, cumulativeCountsCopy);
        return new FixedBuckets(upperBoundsCopy, cumulativeCountsCopy);
    }

    private static void sortAndValidate(double[] upperBounds, long[] cumulativeCounts) {
        if (upperBounds.length != cumulativeCounts.length) {
            throw new IllegalArgumentException("upperBounds.length == " + upperBounds.length + " but cumulativeCounts.length == " + cumulativeCounts.length + ". Expected the same length.");
        }
        sort(upperBounds, cumulativeCounts);
        validate(upperBounds, cumulativeCounts);
    }

    private static void sort(double[] upperBounds, long[] cumulativeCounts) {
        // Bubblesort. Should be efficient here as in most cases upperBounds is already sorted.
        int n = upperBounds.length;
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (upperBounds[j] > upperBounds[j + 1]) {
                    swap(j, j+1, upperBounds, cumulativeCounts);
                }
            }
        }
    }

    private static void swap(int i, int j, double[] upperBounds, long[] cumulativeCounts) {
        double tmpDouble = upperBounds[j];
        upperBounds[j] = upperBounds[i];
        upperBounds[i] = tmpDouble;
        long tmpLong = cumulativeCounts[j];
        cumulativeCounts[j] = cumulativeCounts[i];
        cumulativeCounts[i] = tmpLong;
    }

    private static void validate(double[] upperBounds, long[] cumulativeCounts) {
        // Preconditions:
        // * upperBounds sorted
        // * upperBounds and cumulativeCounts have the same length
        if (upperBounds.length == 0) {
            throw new IllegalArgumentException("Buckets cannot be empty. They must contain at least the +Inf bucket.");
        }
        if (upperBounds[upperBounds.length-1] != Double.POSITIVE_INFINITY) {
            throw new IllegalArgumentException("Buckets must contain the +Inf bucket.");
        }
        for (int i=0; i<upperBounds.length; i++) {
            if (Double.isNaN(upperBounds[i])) {
                throw new IllegalArgumentException("Cannot use NaN as an upper bound.");
            }
            if (cumulativeCounts[i] < 0) {
                throw new IllegalArgumentException("Bucket counts cannot be negative.");
            }
            if (i > 0) {
            if (upperBounds[i-1] == upperBounds[i]) {
                throw new IllegalArgumentException("Duplicate upper bound " + upperBounds[i]);
            }
            if (cumulativeCounts[i-1] > cumulativeCounts[i]) {
                throw new IllegalArgumentException("Bucket counts must be cumulative.");
            }
            }
        }
    }

    public int size() {
        return upperBounds.length;
    }

    public double getUpperBound(int i) {
        return upperBounds[i];
    }

    public long getCumulativeCount(int i) {
        return cumulativeCounts[i];
    }

    private List<FixedBucket> asList() {
        List<FixedBucket> result = new ArrayList<>(size());
        for (int i=0; i<upperBounds.length; i++) {
            result.add(new FixedBucket(upperBounds[i], cumulativeCounts[i]));
        }
        return result;
    }

    @Override
    public Iterator<FixedBucket> iterator() {
        return asList().iterator();
    }

    public Stream<FixedBucket> stream() {
        return asList().stream();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private final List<Double> upperBounds = new ArrayList<>();
        private final List<Long> cumulativeCounts = new ArrayList<>();

        private Builder() {}

        public Builder addBucket(double upperBound, long cumulativeCount) {
            upperBounds.add(upperBound);
            cumulativeCounts.add(cumulativeCount);
            return this;
        }

        public FixedBuckets build() {
            return FixedBuckets.of(upperBounds, cumulativeCounts);
        }
    }

}