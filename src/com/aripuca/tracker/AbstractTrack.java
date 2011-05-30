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

	protected float maxSpeed = 0;

	protected double currentElevation = 0;

	protected double minElevation = 8848;

	protected double maxElevation = -10971;

	protected double elevationGain = 0;

	protected double elevationLoss = 0;

	protected int trackPointsCount = 0;


	/**
	 * recording start time
	 */
	protected long startTime = 0;

	/**
	 * real time of the track start
	 */
	protected long trackTimeStart;

	/**
	 * total time of recording
	 */
	protected long totalTime = 0;
	protected long movingTime = 0;

	// --------------------------------------------------------------------------------------------------------

	public AbstractTrack(MyApp myApp) {
		
		this.myApp = myApp;
		
		this.trackTimeStart = (new Date()).getTime();

	}
	
	/**
	 * Return time track recording started
	 */
	public long getTrackTimeStart() {

		return this.trackTimeStart;

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

	protected void processSpeed(Location location) {

		// calculating max speed
		if (location.hasSpeed()) {

			float s = location.getSpeed();

			if (s == 0) {
				s = this.getAverageSpeed();
			}
			if (s > this.maxSpeed) {
				this.maxSpeed = s;
			}
		}
	}
	
	public void setDistance(float d) {
		
		this.distance+=d;
		
	}
	
	

}
