package com.terminal_devilal.utils.common_calcs;

import java.util.Collection;

// OLS slope calculator for a series of y values, where x is the index of the y value in the collection.
// Helps to determine Price Growth Rate.. How many points per day is price increasing?
public class SlopeCalculator {

	public static double computeSlope(Collection<Double> y) {

		int n = y.size();
		if (n < 2)
			return 0.0;

		double sumX = 0.0;
		double sumY = 0.0;
		double sumXY = 0.0;
		double sumXX = 0.0;

		int i = 0;
		for (double yi : y) {
			double xi = i++;
			sumX += xi;
			sumY += yi;
			sumXY += xi * yi;
			sumXX += xi * xi;
		}

		double denominator = n * sumXX - sumX * sumX;
		if (denominator == 0.0)
			return 0.0;

		return (n * sumXY - sumX * sumY) / denominator;
	}
}
