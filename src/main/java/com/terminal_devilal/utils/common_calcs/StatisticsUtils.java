package com.terminal_devilal.utils.common_calcs;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class StatisticsUtils {

	private static final double SQRT_2PI_INV = 0.3989422804014327d;

	private StatisticsUtils() {
	}

	// Sen Slope implementation - Is the stock genuinely trending upward?
	public static long computeS(double[] values) {
		long s = 0L;

		for (int i = 0; i < values.length - 1; i++) {
			double left = values[i];
			for (int j = i + 1; j < values.length; j++) {
				double diff = values[j] - left;
				if (diff > 0.0d) {
					s++;
				} else if (diff < 0.0d) {
					s--;
				}
			}
		}

		return s;
	}

	public static double computeVarianceS(double[] sortedValues) {
		int n = sortedValues.length;
		if (n < 2) {
			return 0.0d;
		}

		double tieAdjustment = 0.0d;
		int runLength = 1;

		for (int i = 1; i < n; i++) {
			if (Double.compare(sortedValues[i], sortedValues[i - 1]) == 0) {
				runLength++;
			} else {
				tieAdjustment += tieContribution(runLength);
				runLength = 1;
			}
		}

		tieAdjustment += tieContribution(runLength);

		double nn = n;
		return (nn * (nn - 1.0d) * (2.0d * nn + 5.0d) - tieAdjustment) / 18.0d;
	}

	public static double computeZ(long s, double varS) {
		if (varS <= 0.0d) {
			return 0.0d;
		}

		double stdDev = Math.sqrt(varS);
		if (s > 0L) {
			return (s - 1.0d) / stdDev;
		}
		if (s < 0L) {
			return (s + 1.0d) / stdDev;
		}
		return 0.0d;
	}

	public static double computeTau(long s, int n) {
		if (n < 2) {
			return 0.0d;
		}

		double denominator = n * (n - 1L) / 2.0d;
		if (denominator == 0.0d) {
			return 0.0d;
		}

		return s / denominator;
	}

	public static double computePValue(double z) {
		double p = 2.0d * (1.0d - normalCdf(Math.abs(z)));
		if (p < 0.0d) {
			return 0.0d;
		}
		if (p > 1.0d) {
			return 1.0d;
		}
		return p;
	}

	public static double computeSenSlope(double[] values) {
		int n = values.length;
		int pairCount = n * (n - 1) / 2;
		if (pairCount == 0) {
			return 0.0d;
		}

		double[] slopes = new double[pairCount];
		int cursor = 0;
		for (int i = 0; i < n - 1; i++) {
			double left = values[i];
			for (int j = i + 1; j < n; j++) {
				slopes[cursor++] = (values[j] - left) / (j - i);
			}
		}

		Arrays.sort(slopes);
		return medianSorted(slopes);
	}

	public static double medianSorted(double[] sortedValues) {
		int n = sortedValues.length;
		if (n == 0) {
			return 0.0d;
		}

		int middle = n / 2;
		if ((n & 1) == 1) {
			return sortedValues[middle];
		}

		return (sortedValues[middle - 1] + sortedValues[middle]) / 2.0d;
	}

	public static double computeIntercept(double medianY, double slope, int observationCount) {
		return medianY - (slope * medianTimeIndex(observationCount));
	}

    public static double mean(double[] values) {
        if (values == null || values.length == 0) {
            return 0.0d;
        }
        double sum = 0.0d;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    public static double stdDev(double[] values, double mean) {
        if (values == null || values.length == 0) {
            return 0.0d;
        }
        double variance = 0.0d;
        for (double value : values) {
            double delta = value - mean;
            variance += delta * delta;
        }
        variance /= values.length;
        return Math.sqrt(variance);
    }

    public static double zScore(double value, double mean, double stdDev) {
        if (stdDev == 0.0d) {
            return 0.0d;
        }
        return (value - mean) / stdDev;
    }

    public static Map<String, Double> zScoreMap(Map<String, Double> rawValuesByKey) {
        if (rawValuesByKey == null || rawValuesByKey.isEmpty()) {
            return new LinkedHashMap<>();
        }

        double[] values = rawValuesByKey.values().stream().mapToDouble(Double::doubleValue).toArray();
        double avg = mean(values);
        double stdDev = stdDev(values, avg);

        Map<String, Double> result = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : rawValuesByKey.entrySet()) {
            result.put(entry.getKey(), zScore(entry.getValue(), avg, stdDev));
        }
        return result;
    }

    public static Map<String, Double> percentileMap(Map<String, Double> rawValuesByKey) {
        if (rawValuesByKey == null || rawValuesByKey.isEmpty()) {
            return new LinkedHashMap<>();
        }

        Map<String, Double> sortedByValue = rawValuesByKey.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        LinkedHashMap::new));

        int size = sortedByValue.size();
        Map<String, Double> result = new LinkedHashMap<>();
        int rank = 0;
        for (Map.Entry<String, Double> entry : sortedByValue.entrySet()) {
            double percentile = (100.0d * (rank + 1) / size);
            result.put(entry.getKey(), percentile);
            rank++;
        }
        return result;
    }

    public static String determineTrend(boolean h, double z) {
        if (!h || z == 0.0d) {
            return "no trend";
        }
        return z > 0.0d ? "increasing" : "decreasing";
    }

	private static double tieContribution(int groupSize) {
		if (groupSize < 2) {
			return 0.0d;
		}
		return groupSize * (groupSize - 1.0d) * (2.0d * groupSize + 5.0d);
	}

	private static double medianTimeIndex(int observationCount) {
		if (observationCount <= 0) {
			return 0.0d;
		}
		return (observationCount - 1) / 2.0d;
	}

	private static double normalCdf(double value) {
		if (value <= 0.0d) {
			return 0.5d;
		}

		double t = 1.0d / (1.0d + 0.2316419d * value);
		double polynomial = t
				* (0.319381530d + t * (-0.356563782d + t * (1.781477937d + t * (-1.821255978d + t * 1.330274429d))));
		double pdf = SQRT_2PI_INV * Math.exp(-0.5d * value * value);
		return 1.0d - (pdf * polynomial);
	}
}