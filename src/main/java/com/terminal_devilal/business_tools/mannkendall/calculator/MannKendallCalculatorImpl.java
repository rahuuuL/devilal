package com.terminal_devilal.business_tools.mannkendall.calculator;

import java.util.Arrays;
import java.util.List;

import com.terminal_devilal.utils.common_calcs.StatisticsUtils;

public class MannKendallCalculatorImpl implements MannKendallCalculator {

	private static final double ALPHA = 0.05d;

	@Override
	public MannKendallCalcResult calculate(List<Double> values) {
		validate(values);

		int size = values.size();
		double[] originalValues = new double[size];
		for (int index = 0; index < size; index++) {
			Double value = values.get(index);
			if (value == null || !Double.isFinite(value)) {
				throw new IllegalArgumentException("Input contains invalid numeric value at index " + index + ".");
			}
			originalValues[index] = value;
		}

		double[] sortedValues = Arrays.copyOf(originalValues, size);
		Arrays.sort(sortedValues);

		long s = StatisticsUtils.computeS(originalValues);
		double varS = StatisticsUtils.computeVarianceS(sortedValues);
		double z = StatisticsUtils.computeZ(s, varS);
		double tau = StatisticsUtils.computeTau(s, size);
		double p = StatisticsUtils.computePValue(z);
		boolean h = p < ALPHA;
		double slope = StatisticsUtils.computeSenSlope(originalValues);
		double intercept = StatisticsUtils.computeIntercept(StatisticsUtils.medianSorted(sortedValues), slope, size);

		MannKendallCalcResult result = new MannKendallCalcResult();
		result.setTrend(StatisticsUtils.determineTrend(h, z));
		result.setH(h);
		result.setP(p);
		result.setZ(z);
		result.setTau(tau);
		result.setS(s);
		result.setVarS(varS);
		result.setSlope(slope);
		result.setIntercept(intercept);
		return result;
	}

	private static void validate(List<Double> values) {
		if (values == null) {
			throw new IllegalArgumentException("Input values cannot be null.");
		}
		if (values.size() < 3) {
			throw new IllegalArgumentException("Input values must contain at least 3 observations.");
		}
	}
}