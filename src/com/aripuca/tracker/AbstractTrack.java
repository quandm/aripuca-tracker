package com.aripuca.tracker;

import java.util.Date;

import android.location.Location;

public abstract class AbstractTrack {

	/**
	 * Reference to application object for accessing db etc.
	 */
	protected MyApp myApp;

	protected float distance = 0;

	protected float averageSpeed = 0;

	protected float acceleration = 0;

	protected float maxSpeed = 0;

	protected double currentElevation = 0;

	protected double minElevation = 8848;

	protected double maxElevation = -10971;

	protected double elevationGain = 0;

	protected double elevationLoss = 0;

	protected int trackPointsCount = 0;

	/**
	 * real time of the track start
	 */
	protected long trackTimeStart;

	public long getTrackTimeStart() {
		return trackTimeStart;
	}

	// --------------------------------------------------------------------------------------------------------

	public AbstractTrack(MyApp myApp) {

		this.myApp = myApp;

		this.trackTimeStart = (new Date()).getTime();

	}

	/**
	 * Get average trip speed in meters per second
	 */
	public float getAverageSpeed() {

		if (this.getTotalTime() < 1000) {
			return 0;
		}

		return this.getDistance() / (this.getTotalTime() / 1000.0f);
	}

	/**
	 * @return Average moving speed in meters per second
	 */
	public float getAverageMovingSpeed() {

		if (this.getMovingTime() < 1000) {
			return 0;
		}

		return this.getDistance() / (this.getMovingTime() / 1000.0f);
	}

	/**
	 * Get total trip distance
	 */
	public float getDistance() {
		return this.distance;
	}

	/**
	 * Get maximum trip speed
	 */
	public float getMaxSpeed() {
		return this.maxSpeed;
	}

	/**
	 * @return Elevation gain during track recording
	 */
	public int getElevationGain() {
		return (int) this.elevationGain;
	}

	/**
	 * @return Elevation loss during track recording
	 */
	public int getElevationLoss() {
		return (int) this.elevationLoss;
	}

	/**
	 * @return Max elevation during track recording
	 */
	public double getMaxElevation() {
		return this.maxElevation;
	}

	/**
	 * @return Min elevation during track recording
	 */
	public double getMinElevation() {
		return this.minElevation;
	}

	/**
	 * Get total trip time in milliseconds
	 */
	public long getTotalTime() {
		return this.currentSystemTime - this.startTime - this.totalPauseTime;
	}

	/**
	 * Get total moving trip time in milliseconds
	 */
	public long getMovingTime() {
		return this.getTotalTime() - this.totalIdleTime;
	}

	protected void processElevation(Location location) {

		// processing elevation data
		if (location.hasAltitude()) {
			double e = location.getAltitude();
			// max elevation
			if (this.maxElevation < e) {
				this.maxElevation = e;
			}
			// min elevation
			if (e < this.minElevation) {
				this.minElevation = e;
			}
			// if current elevation not set yet
			if (this.currentElevation == 0) {
				this.currentElevation = e;
			} else {
				// elevation gain/loss
				if (e > this.currentElevation) {
					this.elevationGain += e - this.currentElevation;
				} else {
					this.elevationLoss += this.currentElevation - e;
				}
				this.currentElevation = e;
			}
		}
	}

	protected void processSpeed(Location lastLocation, Location currentLocation) {

		if (lastLocation==null) {
			return;
		}

		// calculate acceleration
		if (lastLocation.hasSpeed() && currentLocation.hasSpeed()) {
			
			this.acceleration = 0;

			long timeInterval = (currentLocation.getTime() - lastLocation.getTime()) / 1000;
			if (timeInterval>0) {
				this.acceleration = Math.abs(lastLocation.getSpeed() - currentLocation.getSpeed()) / timeInterval;
			}

			// abnormal accelerations will not affect max speed
			if (this.acceleration > Constants.MAX_ACCELERATION) {
				return;
			}
		}

		// currentLocation.getSpeed() > Constants.MIN_SPEED

		// calculating max speed
		if (currentLocation.hasSpeed()) {

			float s = currentLocation.getSpeed();

			if (s == 0) {
				s = this.getAverageSpeed();
			}
			if (s > this.maxSpeed) {
				this.maxSpeed = s;
			}

		}

	}

	public void updateDistance(float d) {

		this.distance += d;

	}

	/*
	 * total idle time
	 */
	protected long totalIdleTime = 0;

	public void updateTotalIdleTime(long t) {
		this.totalIdleTime += t;
	}

	public long getTotalIdleTime() {
		return this.totalIdleTime;
	}

	/**
	 * total time recording was paused
	 */
	protected long totalPauseTime = 0;

	public void updateTotalPauseTime(long t) {
		this.totalPauseTime += t;
	}

	public long getTotalPauseTime() {
		return this.totalPauseTime;
	}

	protected long currentSystemTime = 0;

	public void setCurrentSystemTime(long cst) {
		this.currentSystemTime = cst;
	}

	protected long startTime = 0;

	public void setStartTime(long st) {
		this.startTime = st;
	}

	public long getStartTime() {
		return this.startTime;
	}

	public float getAcceleration() {
		return this.acceleration;
	}

}
