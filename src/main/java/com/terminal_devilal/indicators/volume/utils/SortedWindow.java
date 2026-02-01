package com.terminal_devilal.indicators.volume.utils;

import java.util.ArrayList;
import java.util.Collections;

public class SortedWindow {

	private final int maxSize;
	private final ArrayList<Double> data = new ArrayList<>();

	public SortedWindow(int maxSize) {
		this.maxSize = maxSize;
	}

	public void add(double value) {
		int idx = Collections.binarySearch(data, value);
		if (idx < 0) {
			idx = -idx - 1;
		}
		data.add(idx, value);
	}

	public void remove(double value) {
		int idx = Collections.binarySearch(data, value);
		if (idx >= 0) {
			data.remove(idx);
		}
	}

	public boolean isFull() {
		return data.size() >= maxSize;
	}

	public double percentile(double percentile) {
		if (data.isEmpty())
			return Double.NaN;

		int index = (int) Math.ceil((percentile / 100.0) * data.size()) - 1;
		index = Math.max(0, Math.min(index, data.size() - 1));
		return data.get(index);
	}

	public double filteredMean(double lowPercentile, double highPercentile) {
		if (data.isEmpty())
			return Double.NaN;

		int lo = (int) Math.floor(lowPercentile / 100.0 * data.size());
		int hi = (int) Math.ceil(highPercentile / 100.0 * data.size());

		lo = Math.max(0, lo);
		hi = Math.min(data.size(), hi);

		if (lo >= hi)
			return Double.NaN;

		double sum = 0;
		for (int i = lo; i < hi; i++) {
			sum += data.get(i);
		}
		return sum / (hi - lo);
	}
}
