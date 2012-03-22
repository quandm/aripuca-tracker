package com.aripuca.tracker.util;

public class OrientationValues {

	private float azimuth;

	private float pitch;

	private float roll;
	
	public OrientationValues(float azimuth, float pitch, float roll) {
		this.azimuth = azimuth;
		this.pitch = pitch;
		this.roll = roll;
	}
	/**
	 * @return the azimuth
	 */
	public float getAzimuth() {
		return azimuth;
	}

	/**
	 * @param azimuth the azimuth to set
	 */
	public void setAzimuth(float azimuth) {
		this.azimuth = azimuth;
	}

	/**
	 * @return the pitch
	 */
	public float getPitch() {
		return pitch;
	}

	/**
	 * @param pitch the pitch to set
	 */
	public void setPitch(float pitch) {
		this.pitch = pitch;
	}

	/**
	 * @return the roll
	 */
	public float getRoll() {
		return roll;
	}

	/**
	 * @param roll the roll to set
	 */
	public void setRoll(float roll) {
		this.roll = roll;
	}

}
