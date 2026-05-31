package com.terminal_devilal.business_tools.ratio_analysis.service;

public final class RollingSharpeSortino {

	private final double[] returns;
	private final int window;

	private int head = 0;
	private int size = 0;

	private double sum = 0.0;
	private double sumSq = 0.0;

	// downside deviation uses MAR-adjusted returns
	private double downsideSumSq = 0.0;

	private final double mar; // mean annual risk-free converted to per-period

	public RollingSharpeSortino(int window, double riskFreeRateAnnual) {
		this.window = window;
		this.returns = new double[window];

		this.mar = riskFreeRateAnnual / 252.0;
	}

	public void add(double r) {

		// If buffer is full → remove oldest value
		if (size == window) {
			double old = returns[head];

			sum -= old;
			sumSq -= old * old;

			double oldDiff = old - mar;
			if (oldDiff < 0) {
				downsideSumSq -= oldDiff * oldDiff;
			}

		} else {
			size++;
		}

		// insert new value
		returns[head] = r;

		sum += r;
		sumSq += r * r;

		double diff = r - mar;
		if (diff < 0) {
			downsideSumSq += diff * diff;
		}

		head = (head + 1) % window;
	}

	public boolean isReady() {
		return size == window;
	}

	// ===== Sharpe =====
	public double getMean() {
		return sum / size;
	}

	public double getStdDev() {
		double mean = getMean();
		double variance = (sumSq / size) - (mean * mean);

		return variance > 0 ? Math.sqrt(variance) : 0.0;
	}

	public double getSharpe() {
		double std = getStdDev();
		if (std == 0)
			return 0;

		double excess = getMean() - mar;
		return excess / std;
	}

	// ===== Sortino =====
	public double getDownsideDeviation() {
		double variance = downsideSumSq / size;
		return variance > 0 ? Math.sqrt(variance) : 0.0;
	}

	public double getSortino() {
		double dd = getDownsideDeviation();
		if (dd == 0)
			return 0;

		double excess = getMean() - mar;
		return excess / dd;
	}
}
