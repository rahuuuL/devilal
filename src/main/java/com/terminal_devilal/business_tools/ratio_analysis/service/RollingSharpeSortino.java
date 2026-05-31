package com.terminal_devilal.business_tools.ratio_analysis.service;

public final class RollingSharpeSortino {

    private final double[] returns;

    private int head = 0;
    private int size = 0;

    private double sum = 0.0;
    private double sumSq = 0.0;
    private double downsideSumSq = 0.0;

    public RollingSharpeSortino(int window) {
        this.returns = new double[window];
    }

    public void add(double value) {

        if (size == returns.length) {

            double old = returns[head];

            sum -= old;
            sumSq -= old * old;

            if (old < 0) {
                downsideSumSq -= old * old;
            }

        } else {
            size++;
        }

        returns[head] = value;

        sum += value;
        sumSq += value * value;

        if (value < 0) {
            downsideSumSq += value * value;
        }

        head = (head + 1) % returns.length;
    }

    public boolean isReady() {
        return size == returns.length;
    }

    public double getMean() {
        return sum / size;
    }

    public double getStdDev() {

        double mean = getMean();

        double variance =
                (sumSq / size) - (mean * mean);

        if (variance <= 0) {
            return 0;
        }

        return Math.sqrt(variance);
    }

    public double getDownsideDeviation() {

        double downsideVariance =
                downsideSumSq / size;

        if (downsideVariance <= 0) {
            return 0;
        }

        return Math.sqrt(downsideVariance);
    }

    public double getSharpe(double riskFreeRate) {

        double stdDev = getStdDev();

        if (stdDev == 0) {
            return 0;
        }

        return (getMean() - riskFreeRate) / stdDev;
    }

    public double getSortino(double riskFreeRate) {

        double downsideDeviation =
                getDownsideDeviation();

        if (downsideDeviation == 0) {
            return 0;
        }

        return (getMean() - riskFreeRate)
                / downsideDeviation;
    }
}
