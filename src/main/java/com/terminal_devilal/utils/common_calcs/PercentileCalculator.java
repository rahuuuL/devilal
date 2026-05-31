package com.terminal_devilal.utils.common_calcs;

import java.util.List;

public final class PercentileCalculator {

	private PercentileCalculator() {
	}

	public static double computePercentileSorted(List<Double> sorted, double target) {

		int lo = 0;
		int hi = sorted.size();

		while (lo < hi) {
			int mid = (lo + hi) >>> 1;

			if (sorted.get(mid) < target) {
				lo = mid + 1;
			} else {
				hi = mid;
			}
		}

		return (100.0 * lo) / sorted.size();
	}

	public static double computePercentileSorted(double[] sorted, double target) {

		if (sorted == null || sorted.length == 0)
			return 0.0;

		int lo = 0;
		int hi = sorted.length;

		while (lo < hi) {
			int mid = (lo + hi) >>> 1;

			if (sorted[mid] < target) {
				lo = mid + 1;
			} else {
				hi = mid;
			}
		}

		return (100.0 * lo) / sorted.length;
	}
}