package com.aripuca.tracker.track;

import com.aripuca.tracker.MyApp;
import com.aripuca.tracker.app.Constants;
import com.aripuca.tracker.util.Population;

import android.location.Location;
import android.os.SystemClock;

/**
 * TrackRecorder class. Handles tracks and segments statistics
 */
public class TrackRecorder {

	private static TrackRecorder instance = null;

	/**
	 * Reference to Application object
	 */
	private MyApp myApp;

	private int minDistance;

	private int minAccuracy;

	protected Location lastLocation;

	protected Location lastRecordedLocation;

	/**
	 * distance increments buffer
	 */
	protected Population distancePopulation;

	/**
	 * track being recorded
	 */
	private Track track;

	public Track getTrack() {
		return track;
	}

	/**
	 * Segment statistics object
	 */
	private Segment segment;

	/**
	 * recording paused flag
	 */
	protected boolean recordingPaused = false;

	/**
	 * recording paused start time
	 */
	protected long pauseTimeStart = 0;

	protected long idleTimeStart = 0;

	private long currentSystemTime = 0;

	private int pointsCount = 0;

	public int getPointsCount() {
		return pointsCount;
	}

	/**
	 * recording start time
	 */
	private long startTime = 0;

	/**
	 * 
	 */
	private int segmentingMode;

	/**
	 * Index of the current track segment
	 */
	private int segmentIndex = 0;
	private float segmentInterval;
	private float[] segmentIntervals;
	/**
	 * segmenting time interval in minutes
	 */
	private float segmentTimeInterval;

	/**
	 * Returns number of segments created for the track
	 */
	public int getSegmentsCount() {
		return segmentIndex + 1;
	}

	/**
	 * Singleton pattern
	 */
	public static TrackRecorder getInstance(MyApp myApp) {

		if (instance == null) {
			instance = new TrackRecorder(myApp);
		}

		return instance;

	}

	/**
	 * Private constructor
	 */
	private TrackRecorder(MyApp myApp) {

		this.myApp = myApp;

		this.distancePopulation = new Population(10);

	}

	/**
	 * Start track recording
	 */
	public void start() {

		currentSystemTime = 0;
		startTime = 0;
		pauseTimeStart = 0;
		idleTimeStart = 0;

		pointsCount = 0;

		segmentIndex = 0;

		minDistance = Integer.parseInt(myApp.getPreferences().getString("min_distance", "15"));
		minAccuracy = Integer.parseInt(myApp.getPreferences().getString("min_accuracy", "15"));

		this.distancePopulation.reset();

		// create new track statistics object
		this.track = new Track(myApp);

		this.segmentingMode = Integer.parseInt(myApp.getPreferences().getString("segmenting_mode", "0"));

		// creating default segment
		// if no segments will be created during track recording
		// we won't insert segment data to db
		if (this.segmentingMode != Constants.SEGMENT_NONE) {

			this.segment = new Segment(myApp);

			switch (this.segmentingMode) {

				case Constants.SEGMENT_DISTANCE:

					// setting segment interval
					segmentInterval = Float.parseFloat(myApp.getPreferences().getString("segment_distance", "5"));
				break;

				case Constants.SEGMENT_TIME:

					// default time segmenting: 10 minutes
					segmentTimeInterval = Float.parseFloat(myApp.getPreferences().getString("segment_time", "10"));
				break;

				case Constants.SEGMENT_CUSTOM_1:
				case Constants.SEGMENT_CUSTOM_2:

					this.setSegmentIntervals();
				break;

			}

		}

	}

	/**
	 * Stop track recording
	 */
	public void stop() {

		if (this.segmentingMode != Constants.SEGMENT_NONE) {
			// insert segment in db only if there were more then one segments in
			// this track
			if (this.segmentIndex > 0) {
				this.segment.insertSegment(this.getTrack().getTrackId(), this.segmentIndex);
				this.segment = null;
			}
		}

		// updating track statistics in db
		this.track.updateNewTrack();
		this.track = null;

	}

	/**
	 * Pause track recording
	 */
	public void pause() {

		this.recordingPaused = true;

	}

	/**
	 * Resume track recording
	 */
	public void resume() {

		this.recordingPaused = false;

		// segmenting by pause/resume
		if (this.segmentingMode == Constants.SEGMENT_PAUSE_RESUME) {
			this.addNewSegment();
		}

	}

	/**
	 * Insert current segment to db and create new one
	 */
	private void addNewSegment() {

		this.segment.insertSegment(this.getTrack().getTrackId(), this.segmentIndex);

		this.segment = null;

		this.segmentIndex++;

		this.segment = new Segment(myApp);

	}

	public boolean isRecording() {
		return this.track != null;
	}

	public boolean isRecordingPaused() {

		return this.recordingPaused;

	}

	/**
	 * Updates track statistics when recording
	 * 
	 * @param location
	 */
	public void updateStatistics(Location location) {

		// set interval start time
		// measure time intervals (idle, pause)
		if (!this.measureTrackTimes(location)) {
			return;
		}

		// calculating total distance starting from 2nd update
		// speed at last location should be non-zero
		if (this.lastLocation != null && this.lastLocation.getSpeed() > Constants.MIN_SPEED) {

			// distance between current and last location
			float distanceIncrement = this.lastLocation.distanceTo(location);

			// adding distance increment to buffer
			this.distancePopulation.addValue(distanceIncrement);

			// false readings should not affect distance calculation 
			if (distanceIncrement < this.distancePopulation.getAverage() * 10) {

				// accumulate track distance
				this.track.updateDistance(distanceIncrement);

				// accumulate segment distance
				if (this.segmentingMode != Constants.SEGMENT_NONE) {
					this.segment.updateDistance(distanceIncrement);
				}
			}
		}

		// calculate maxSpeed and acceleration
		this.track.processSpeed(this.lastLocation, location);

		this.track.processElevation(location);

		this.processSegments(location);

		// save current location once distance is incremented
		this.lastLocation = location;

		// add new track point to db
		this.recordTrackPoint(location);

	}

	private void processSegments(Location location) {

		// SEGMENTING
		switch (this.segmentingMode) {

			// segmenting track by distance
			case Constants.SEGMENT_DISTANCE:
			case Constants.SEGMENT_CUSTOM_1:
			case Constants.SEGMENT_CUSTOM_2:

				this.segmentTrack();
			break;

			// segmenting track by time
			case Constants.SEGMENT_TIME:

				this.segmentTrackByTime();
			break;
		}

		// updating segment statistics
		if (this.segmentingMode != Constants.SEGMENT_NONE) {

			this.segment.processElevation(location);
			this.segment.processSpeed(this.lastLocation, location);

		}

	}

	/**
	 * 
	 */
	private boolean measureTrackTimes(Location location) {

		// all times measured got synchronized with currentSystemTime
		this.currentSystemTime = SystemClock.uptimeMillis();

		this.track.setCurrentSystemTime(this.currentSystemTime);
		if (this.segmentingMode != Constants.SEGMENT_NONE) {
			this.segment.setCurrentSystemTime(this.currentSystemTime);
		}

		if (this.startTime == 0) {
			this.startTime = this.currentSystemTime;
			this.track.setStartTime(this.currentSystemTime);
		}

		if (this.segmentingMode != Constants.SEGMENT_NONE) {
			if (this.segment.getStartTime() == 0) {
				this.segment.setStartTime(this.currentSystemTime);
			}
		}

		// ------------------------------------------------------------------------------
		// times are recorded even if accuracy is not acceptable
		this.processPauseTime();

		if (this.recordingPaused) {
			// after resuming the recording we will start measuring distance
			// from saved location
			this.lastLocation = location;
			return false;
		}

		this.processIdleTime(location);
		// ------------------------------------------------------------------------------

		return true;

	}

	/**
	 * Process time the device was not moving
	 */
	protected void processIdleTime(Location location) {

		// updating idle time in track
		if (location.getSpeed() < Constants.MIN_SPEED) {

			// if idle interval started increment total idle time
			if (this.idleTimeStart != 0) {

				this.track.updateTotalIdleTime(this.currentSystemTime - this.idleTimeStart);

				if (this.segmentingMode != Constants.SEGMENT_NONE) {
					this.segment.updateTotalIdleTime(this.currentSystemTime - this.idleTimeStart);
				}

			}
			// save start idle time
			this.idleTimeStart = this.currentSystemTime;

		} else {

			// increment total idle time with already started interval
			if (this.idleTimeStart != 0) {

				this.track.updateTotalIdleTime(this.currentSystemTime - this.idleTimeStart);

				if (this.segmentingMode != Constants.SEGMENT_NONE) {
					this.segment.updateTotalIdleTime(this.currentSystemTime - this.idleTimeStart);
				}

				this.idleTimeStart = 0;

			}

		}

	}

	/**
	 * Process time this track was paused
	 */
	private void processPauseTime() {

		if (this.recordingPaused) {

			// if idle interval started increment total idle time
			if (this.idleTimeStart != 0) {

				this.track.updateTotalIdleTime(this.currentSystemTime - this.idleTimeStart);

				if (this.segmentingMode != Constants.SEGMENT_NONE) {
					this.segment.updateTotalIdleTime(this.currentSystemTime - this.idleTimeStart);
				}

				this.idleTimeStart = 0;
			}

			if (this.pauseTimeStart != 0) {

				this.track.updateTotalPauseTime(this.currentSystemTime - this.pauseTimeStart);

				if (this.segmentingMode != Constants.SEGMENT_NONE) {
					this.segment.updateTotalPauseTime(this.currentSystemTime - this.pauseTimeStart);
				}
			}

			// saving new pause time start
			this.pauseTimeStart = this.currentSystemTime;

		} else {

			if (this.pauseTimeStart != 0) {

				this.track.updateTotalPauseTime(this.currentSystemTime - this.pauseTimeStart);

				if (this.segmentingMode != Constants.SEGMENT_NONE) {
					this.segment.updateTotalPauseTime(this.currentSystemTime - this.pauseTimeStart);
				}
			}

			this.pauseTimeStart = 0;
		}

	}

	/**
	 * Record new track point if it's not too close to previous recorded one
	 */
	private void recordTrackPoint(Location location) {

		// let's not record this update if accuracy is not acceptable
		if (location.getAccuracy() > minAccuracy) {
			return;
		}

		// record points only if distance between 2 consecutive points is
		// greater than min_distance
		// if new segment just started we may not add new points for it
		if (this.lastRecordedLocation == null) {

			this.track.recordTrackPoint(location, this.segmentIndex);
			this.lastRecordedLocation = location;

			pointsCount++;

		} else {

			if (this.lastRecordedLocation.distanceTo(location) >= minDistance) {

				this.track.recordTrackPoint(location, this.segmentIndex);
				this.lastRecordedLocation = location;

				pointsCount++;
			}
		}
	}

	/**
	 * 
	 */
	private void setSegmentIntervals() {

		String segmentIntervalsKey;
		if (this.segmentingMode == Constants.SEGMENT_CUSTOM_1) {
			segmentIntervalsKey = "segment_custom_1";
		} else if (this.segmentingMode == Constants.SEGMENT_CUSTOM_2) {
			segmentIntervalsKey = "segment_custom_2";
		} else {
			return;
		}

		String[] tmpArr = myApp.getPreferences().getString(segmentIntervalsKey, "").split(",");

		segmentIntervals = new float[tmpArr.length];

		for (int i = 0; i < tmpArr.length; i++) {

			try {
				segmentIntervals[i] = Float.parseFloat(tmpArr[i]);
			} catch (NumberFormatException e) {
				// default interval 5 km
				segmentIntervals[i] = 5;
			}

		}

	}

	/**
	 * Check if segment id incrementing is required
	 */
	public void segmentTrack() {

		if (this.track.getDistance() / this.getNextSegment() > 1) {

			this.addNewSegment();

		}

	}

	public void segmentTrackByTime() {

		if (this.track.getMovingTime() / this.getNextSegment() > 1) {

			this.addNewSegment();

		}

	}

	/**
	 * Calculate interval where to start new segment
	 */
	private float getNextSegment() {

		switch (this.segmentingMode) {

			case Constants.SEGMENT_DISTANCE:

				float nextSegment = 0;
				for (int i = 0; i <= this.segmentIndex; i++) {
					nextSegment += segmentInterval;
				}
				return nextSegment * 1000;

			case Constants.SEGMENT_TIME:

				float nextSegment1 = 0;
				for (int i = 0; i <= this.segmentIndex; i++) {
					nextSegment1 += segmentTimeInterval;
				}

				// minutes to milliseconds
				return nextSegment1 * 1000 * 60;

			case Constants.SEGMENT_CUSTOM_1:
			case Constants.SEGMENT_CUSTOM_2:

				// processing custom segment intervals
				if (this.segmentIndex < segmentIntervals.length) {

					return segmentIntervals[this.segmentIndex] * 1000;

				} else {

					// no more segmenting if not enough intervals set by user
					return 10000000;
				}

		}

		return 10000000;
	}

}
