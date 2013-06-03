package com.aripuca.tracker.utils;

public class Population {

	private double[] values;

	private int nextIndex = 0;

	private boolean isFull = false;

	public Population(int size) {

		values = new double[size];

	}

	public void addValue(double value) {

		if (nextIndex == values.length) {
			nextIndex = 0;
		}

		values[nextIndex] = value;

		nextIndex++;
		if (nextIndex == values.length) {
			isFull = true;
		}

	}

	public boolean isFull() {
		return isFull;
	}

	public double getAverage() {

		int totalValues = isFull ? values.length : nextIndex;
		if (totalValues == 0) { return 0; }

		double sum = 0;
		for (int i = 0; i < totalValues; i++) {
			sum += values[i];
		}

		return sum / totalValues;
	}

	public void reset() {
		nextIndex = 0;
		isFull = false;
	}

}
