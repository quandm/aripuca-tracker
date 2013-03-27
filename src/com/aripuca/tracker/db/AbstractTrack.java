package com.aripuca.tracker.db;

import android.database.Cursor;

public class AbstractTrack {

	private long id;

	private float distance;

	private int totalTime;

	private int movingTime;
	
	private float maxSpeed;
	
	private double maxElevation;

	private double minElevation;

	private float elevationGain;

	private float elevationLoss;
	
	private int startTime;

	private int finishTime;	

	private int pointsCount;
	
	public AbstractTrack(Cursor cursor) {
		
		this.id = cursor.getLong(cursor.getColumnIndex("_id"));

		this.distance = cursor.getFloat(cursor.getColumnIndex("distance"));

		this.totalTime = cursor.getInt(cursor.getColumnIndex("total_time"));

		this.movingTime = cursor.getInt(cursor.getColumnIndex("moving_time"));

		this.maxSpeed = cursor.getFloat(cursor.getColumnIndex("max_speed"));
		
		this.maxElevation = cursor.getDouble(cursor.getColumnIndex("max_elevation"));

		this.minElevation = cursor.getDouble(cursor.getColumnIndex("min_elevation"));

		this.elevationGain = cursor.getFloat(cursor.getColumnIndex("elevation_gain"));
		
		this.elevationLoss = cursor.getFloat(cursor.getColumnIndex("elevation_loss"));
		
		this.startTime = cursor.getInt(cursor.getColumnIndex("start_time"));
		
		this.finishTime = cursor.getInt(cursor.getColumnIndex("finish_time"));
		
		this.pointsCount = cursor.getCount();
	}
	
	public long getId() {
		return id;
	}


	public void setId(long id) {
		this.id = id;
	}

	public float getDistance() {
		return distance;
	}


	public void setDistance(float distance) {
		this.distance = distance;
	}


	public int getTotalTime() {
		return totalTime;
	}


	public void setTotalTime(int totalTime) {
		this.totalTime = totalTime;
	}


	public int getMovingTime() {
		return movingTime;
	}


	public void setMovingTime(int movingTime) {
		this.movingTime = movingTime;
	}


	public float getMaxSpeed() {
		return maxSpeed;
	}


	public void setMaxSpeed(float maxSpeed) {
		this.maxSpeed = maxSpeed;
	}


	public double getMaxElevation() {
		return maxElevation;
	}


	public void setMaxElevation(double maxElevation) {
		this.maxElevation = maxElevation;
	}


	public double getMinElevation() {
		return minElevation;
	}


	public void setMinElevation(double minElevation) {
		this.minElevation = minElevation;
	}


	public float getElevationGain() {
		return elevationGain;
	}


	public void setElevationGain(float elevationGain) {
		this.elevationGain = elevationGain;
	}


	public float getElevationLoss() {
		return elevationLoss;
	}


	public void setElevationLoss(float elevationLoss) {
		this.elevationLoss = elevationLoss;
	}


	public int getStartTime() {
		return startTime;
	}


	public void setStartTime(int startTime) {
		this.startTime = startTime;
	}


	public int getFinishTime() {
		return finishTime;
	}


	public void setFinishTime(int finishTime) {
		this.finishTime = finishTime;
	}

	public int getPointsCount() {
		return pointsCount;
	}
	
	
	
}
