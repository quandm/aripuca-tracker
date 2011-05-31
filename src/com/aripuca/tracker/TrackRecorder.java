package com.aripuca.tracker;

import android.location.Location;
import android.os.SystemClock;
import android.util.Log;

public class TrackRecorder {

	private static TrackRecorder instance = null;

	/**
	 * Reference to Application object
	 */
	private MyApp myApp;

	protected Location currentLocation;

	protected Location lastRecordedLocation;

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

	/**
	 * recording start time
	 */
	private long startTime = 0;

	/**
	 * 
	 */
	private int segmentingMode;
	/**
	 * Id of the current track segment
	 */
	private int segmentId = 0;
	private float segmentInterval;
	private float[] segmentIntervals;

	public static TrackRecorder getInstance(MyApp myApp) {

		if (instance == null) {
			instance = new TrackRecorder(myApp);
		}

		return instance;

	}

	private TrackRecorder(MyApp myApp) {

		this.myApp = myApp;

	}

	/**
	 * Start track recording
	 */
	public void start() {
		
		currentSystemTime = 0;
		startTime = 0;
		pauseTimeStart = 0;
		idleTimeStart = 0;

		// create new track statistics object
		this.track = new Track(myApp);

		// creating default segment
		// if no segments will be created during track recording 
		// we won't insert segment data to db
		this.segment = new Segment(myApp);

		this.segmentingMode = Integer.parseInt(myApp.getPreferences().getString("segmenting_mode", "0"));

		if (this.segmentingMode == Constants.SEGMENT_EQUAL) {

			// setting segment interval
			this.setSegmentInterval();
		}

		if (this.segmentingMode == Constants.SEGMENT_CUSTOM_1 ||
				this.segmentingMode == Constants.SEGMENT_CUSTOM_2) {

			this.setSegmentIntervals();
		}

		if (this.segmentingMode != Constants.SEGMENT_NONE) {

			//TODO: create segment object
			// this.segment = new Segment();

		}

	}

	/**
	 * Stop track recording
	 */
	public void stop() {

		// insert segment in db only if there were more then one segments in this track
		if (this.segmentId > 0) {
			this.segment.insertSegment(this.getTrack().getTrackId());
			this.segment = null;
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

			//TODO: add new segment stats to db

			this.addNewSegment();
		}

	}

	private void addNewSegment() {

		this.segment.insertSegment(this.getTrack().getTrackId());

		this.segment = null;
		
		this.segment = new Segment(myApp);

		this.segmentId++;

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

		this.currentSystemTime = SystemClock.uptimeMillis();

		this.track.setCurrentSystemTime(this.currentSystemTime);
		this.segment.setCurrentSystemTime(this.currentSystemTime);

		if (this.startTime == 0) {
			this.startTime = this.currentSystemTime;
			this.track.setStartTime(this.currentSystemTime);
		}
		
		if (this.segment.getStartTime()==0) {
			this.segment.setStartTime(this.currentSystemTime);
		}

		this.processPauseTime();

		if (this.recordingPaused) {
			// after resuming the recording we will start measuring distance from saved location
			this.currentLocation = location;
			return;
		}

		// calculating total distance starting from 2nd update
		if (this.currentLocation != null && this.currentLocation.getSpeed() != 0) {
			this.track.setDistance(this.currentLocation.distanceTo(location));
			this.segment.setDistance(this.currentLocation.distanceTo(location));
		}

		// save current location once distance is incremented
		this.currentLocation = location;

		//TODO: segmenting by distance
		this.segmentTrack();

		this.processIdleTime(location);

		this.track.processElevation(location);
		this.segment.processElevation(location);

		this.track.processSpeed(location);
		this.segment.processSpeed(location);

		// add new track point to db

		// record points only if distance between 2 consecutive points is greater than min_distance
		if (this.lastRecordedLocation == null) {

			this.track.recordTrackPoint(location, this.segmentId);
			this.lastRecordedLocation = location;

		} else {

			if (this.lastRecordedLocation.distanceTo(location) >= Constants.MIN_DISTANCE) {
				this.track.recordTrackPoint(location, this.segmentId);
				this.lastRecordedLocation = location;
			}

		}

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
				this.segment.updateTotalIdleTime(this.currentSystemTime - this.idleTimeStart);

			}
			// save start idle time
			this.idleTimeStart = this.currentSystemTime;

		} else {

			// increment total idle time with already started interval  
			if (this.idleTimeStart != 0) {

				this.track.updateTotalIdleTime(this.currentSystemTime - this.idleTimeStart);
				this.segment.updateTotalIdleTime(this.currentSystemTime - this.idleTimeStart);

				this.idleTimeStart = 0;

			}

		}
		
		Log.v(Constants.TAG, "processIdleTime: Moving: "+this.segment.getMovingTime()+"; Total: "+this.segment.getTotalTime());

	}
	
	/**
	 * Process time this track was paused
	 */
	private void processPauseTime() {

		if (this.recordingPaused) {

			// if idle interval started increment total idle time
			if (this.idleTimeStart != 0) {
				this.track.updateTotalIdleTime(this.currentSystemTime - this.idleTimeStart);
				this.segment.updateTotalIdleTime(this.currentSystemTime - this.idleTimeStart);
				this.idleTimeStart = 0;
			}

			if (this.pauseTimeStart != 0) {
				this.track.updateTotalPauseTime(this.currentSystemTime - this.pauseTimeStart);
				this.segment.updateTotalPauseTime(this.currentSystemTime - this.pauseTimeStart);
			}

			// saving new pause time start
			this.pauseTimeStart = this.currentSystemTime;

		} else {

			if (this.pauseTimeStart != 0) {
				this.track.updateTotalPauseTime(this.currentSystemTime - this.pauseTimeStart);
				this.segment.updateTotalPauseTime(this.currentSystemTime - this.pauseTimeStart);
			}

			this.pauseTimeStart = 0;
		}

		Log.i(Constants.TAG, "processPauseTime: Moving: "+this.segment.getMovingTime()+"; Total: "+this.segment.getTotalTime());
		
		
	}

	/**
	 * 
	 */
	private void setSegmentInterval() {

		// if user entered invalid value - set default interval
		try {
			segmentInterval = Float.parseFloat(myApp.getPreferences().getString("segment_equal", "5"));
		} catch (NumberFormatException e) {
			// default interval 5 km
			segmentInterval = 5;
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

		if (this.segmentingMode == Constants.SEGMENT_NONE ||
				this.segmentingMode == Constants.SEGMENT_PAUSE_RESUME) {
			return;
		}

		if (this.track.getDistance() / this.getNextSegment() > 1) {

			//TODO: add new segment stats to db
			this.addNewSegment();

		}

	}

	/**
	 * Calculate interval where to start new segment
	 */
	private float getNextSegment() {

		switch (this.segmentingMode) {

			case Constants.SEGMENT_EQUAL:

				float nextSegment = 0;
				for (int i = 0; i <= this.segmentId; i++) {
					nextSegment += segmentInterval;
				}
				return nextSegment * 1000;

			case Constants.SEGMENT_CUSTOM_1:
			case Constants.SEGMENT_CUSTOM_2:

				// processing custom segment intervals

				if (this.segmentId < segmentIntervals.length) {

					return segmentIntervals[this.segmentId] * 1000;

				} else {

					// no more segmenting if not enough intervals set by user
					return 10000000;
				}
		}

		return 10000000;
	}

}
