package com.aripuca.tracker.track;

import android.database.sqlite.SQLiteException;
import android.location.Location;
import android.os.SystemClock;

import com.aripuca.tracker.App;
import com.aripuca.tracker.Constants;
import com.aripuca.tracker.db.Segment;
import com.aripuca.tracker.db.Segments;
import com.aripuca.tracker.db.Track;
import com.aripuca.tracker.db.TrackPoints;
import com.aripuca.tracker.utils.TrackStatsBundle;

/**
 * TrackRecorder class. Handles tracks and segments statistics
 */
public class TrackRecorder {

	private boolean isResumeInterruptedTrack = false;

	private static TrackRecorder instance = null;

	/**
	 * Reference to Application object
	 */
	private App app;

	private int minDistance;

	private int minAccuracy;

	protected Location lastLocation;

	protected Location lastRecordedLocation;

	private long interruptedTrackTotalTime;

	/**
	 * track statistics object
	 */
	private TrackStats trackStats;

	/**
	 * Segment statistics object
	 */
	private SegmentStats segmentStats;

	/**
	 * recording paused flag
	 */
	protected boolean recordingPaused = false;

	/**
	 * recording paused start time
	 */
	protected long pauseTimeStart = 0;

	protected long idleTimeStart = 0;

	/**
	 * value of SystemClock.uptimeMillis();
	 */
	private long currentSystemTime = 0;

	private int pointsCount = 0;

	/**
	 * 
	 */
	private int segmentingMode;

	/**
	 * Index of the current track segment
	 */
	private int segmentIndex = 0;

	/**
	 * 
	 */
	private float segmentInterval;

	/**
	 * 
	 */
	private float[] segmentIntervals;

	/**
	 * segmenting time interval in minutes
	 */
	private float segmentTimeInterval;

	/**
	 * Singleton pattern
	 */
	public static TrackRecorder getInstance(App app) {
		if (instance == null) {
			instance = new TrackRecorder(app);
		}
		return instance;
	}

	/**
	 * Private constructor
	 */
	private TrackRecorder(App app) {
		this.app = app;
	}

	/**
	 * Start track recording
	 */
	public void start() {

		this.isResumeInterruptedTrack = false;

		// latest preferences
		this.readPreferences();

		this.lastLocation = null;

		// currentSystemTime = 0;
		this.pauseTimeStart = 0;
		this.idleTimeStart = 0;

		this.pointsCount = 0;
		this.segmentIndex = 0;

		// create new track statistics object
		this.trackStats = new TrackStats(app);

		if (this.segmentingMode != Constants.SEGMENT_NONE) {
			this.segmentStats = new SegmentStats(app, this.trackStats.getTrack().getId(), this.segmentIndex);
		}

	}

	/**
	 * Resume recording interrupted track
	 */
	public void resumeInterruptedTrack(Track lastRecordingTrack) {

		this.isResumeInterruptedTrack = true;
		this.interruptedTrackTotalTime = lastRecordingTrack.getTotalTime();

		// latest preferences
		this.readPreferences();

//		this.lastLocation = TrackPoints.getLast(app.getDatabase(), lastRecordingTrack.getId()).getLocation();
		this.lastLocation = null;

		// this.currentSystemTime = 0;
		this.pauseTimeStart = 0;
		this.idleTimeStart = 0;

		this.pointsCount = TrackPoints.getCount(app.getDatabase(), lastRecordingTrack.getId());

		// create new track statistics object
		this.trackStats = new TrackStats(app, lastRecordingTrack);

		// number of saved segments
		int savedSegmentsCount = Segments.getCount(app.getDatabase(), lastRecordingTrack.getId());
		// number of started segments, from track_points table
		int startedSegmentsCount = Segments.getStartedCount(app.getDatabase(), lastRecordingTrack.getId());

		if (savedSegmentsCount == startedSegmentsCount) {
			// no segment restoration required
			this.segmentIndex = startedSegmentsCount;
		} else {
			this.segmentIndex = startedSegmentsCount - 1;
			this.restoreLastSegment(lastRecordingTrack.getId());
		}

		if (this.segmentingMode != Constants.SEGMENT_NONE) {
			this.segmentStats = new SegmentStats(app, this.getTrackStats().getTrack().getId(), this.segmentIndex);
		}

	}

	/**
	 * Restore last segment that was not saved in db
	 */
	protected void restoreLastSegment(long trackId) {

		TrackStatsBundle tsb = TrackPoints.getStats(app.getDatabase(), trackId, this.segmentIndex);

		Segment segment = new Segment(trackId, this.segmentIndex);

		segment.setDistance(tsb.getDistance());
		segment.setTotalTime(tsb.getTotalTime());
		segment.setMovingTime(tsb.getTotalTime());
		segment.setMaxSpeed(tsb.getMaxSpeed());
		segment.setMaxElevation(tsb.getMaxElevation());
		segment.setMinElevation(tsb.getMinElevation());
		segment.setElevationGain((float) (tsb.getMaxElevation() - tsb.getMinElevation()));
		segment.setElevationLoss((float) (tsb.getMaxElevation() - tsb.getMinElevation()));
		segment.setStartTime(tsb.getStartTime());
		segment.setFinishTime(tsb.getFinishTime());

		try {
			Segments.insert(app.getDatabase(), segment);
		} catch (SQLiteException e) {

		}

		this.segmentIndex++;

	}

	protected void readPreferences() {

		this.minDistance = Integer.parseInt(app.getPreferences().getString("min_distance", "15"));
		this.minAccuracy = Integer.parseInt(app.getPreferences().getString("min_accuracy", "15"));

		this.segmentingMode = Integer.parseInt(app.getPreferences().getString("segmenting_mode", "2"));

		switch (this.segmentingMode) {
			case Constants.SEGMENT_DISTANCE:
				// setting segment interval
				this.segmentInterval = Float.parseFloat(app.getPreferences().getString("segment_distance", "5"));
			break;

			case Constants.SEGMENT_TIME:
				// default time segmenting: 10 minutes
				this.segmentTimeInterval = Float.parseFloat(app.getPreferences().getString("segment_time", "10"));
			break;

			case Constants.SEGMENT_CUSTOM_1:
			case Constants.SEGMENT_CUSTOM_2:
				this.setSegmentIntervals();
			break;

		}

	}

	/**
	 * Stop track recording
	 */
	public void stop() {

		if (this.segmentingMode != Constants.SEGMENT_NONE) {
			//this.segmentStats.insertSegment(this.trackStats.getTrack().getId(), this.segmentIndex);
			this.segmentStats.updateNewSegment();
			this.segmentStats = null;
		}

		// updating track statistics in db
		this.trackStats.finishNewTrack();
		this.trackStats = null;

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
	 * Updates track statistics when recording
	 * 
	 * @param location
	 */
	public void updateStatistics(Location location) {

		// measure time intervals (idle, pause)
		if (!this.measureTrackTimes(location)) {
			// recording paused
			// after resuming the recording we will start measuring distance from saved location
			this.lastLocation = location;
			return;
		}

		// distance between current and last location
		float distanceIncrement;

		// calculating total distance starting from 2nd update
		if (this.lastLocation != null) {

			distanceIncrement = this.lastLocation.distanceTo(location);

			// check for standing still
			if (distanceIncrement < Constants.MIN_DISTANCE && location.getSpeed() < Constants.MIN_SPEED) {

				// update last location and wait for next update
				this.lastLocation = location;
				return;
			}

			// accumulate track distance
			this.trackStats.updateDistance(distanceIncrement);

			// accumulate segment distance
			if (this.segmentingMode != Constants.SEGMENT_NONE) {
				this.segmentStats.updateDistance(distanceIncrement);
			}

		} else {
			// update last location and wait for next update
			this.lastLocation = location;
			return;
		}

		// calculate maxSpeed and acceleration
		boolean speedValid = this.trackStats.isSpeedValid(this.lastLocation, location);

		if (speedValid) {
			this.trackStats.processSpeed(location.getSpeed());
		}

		this.trackStats.processElevation(location);

		this.processSegments(location, speedValid);

		// add new track point to db
		this.recordTrackPoint(location);

		// update new track and segment to avoid losing some statistics in case of application failure
		if (this.pointsCount % 5 == 0) {
			
			this.trackStats.updateNewTrack();
			
			if (this.segmentingMode != Constants.SEGMENT_NONE && this.segmentStats != null) {
				this.segmentStats.updateNewSegment();
			}
			
		}

		// update new last location
		this.lastLocation = location;

	}

	private void processSegments(Location location, boolean validSpeed) {

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

			if (validSpeed) {
				this.segmentStats.processSpeed(location.getSpeed());
			}

			this.segmentStats.processElevation(location);

		}

	}

	/**
	 * 
	 */
	private boolean measureTrackTimes(Location location) {

		// all times measured got synchronized with currentSystemTime
		this.currentSystemTime = SystemClock.uptimeMillis();

		this.trackStats.setCurrentSystemTime(this.currentSystemTime);

		// if (this.segmentingMode != Constants.SEGMENT_NONE) {
		// this.segmentStats.setCurrentSystemTime(this.currentSystemTime);
		// }

		// first update sets startTime to time elapsed since boot
		if (this.trackStats.getStartTime() == 0) {
			this.trackStats.setStartTime(this.currentSystemTime);
		}

		if (this.segmentingMode != Constants.SEGMENT_NONE) {
			this.segmentStats.setCurrentSystemTime(this.currentSystemTime);
			if (this.segmentStats.getStartTime() == 0) {
				this.segmentStats.setStartTime(this.currentSystemTime);
			}
		}

		// ------------------------------------------------------------------------------
		// times are recorded even if accuracy is not acceptable
		this.processPauseTime();

		if (this.recordingPaused) {
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

				this.trackStats.updateTotalIdleTime(this.currentSystemTime - this.idleTimeStart);

				if (this.segmentingMode != Constants.SEGMENT_NONE) {
					this.segmentStats.updateTotalIdleTime(this.currentSystemTime - this.idleTimeStart);
				}

			}
			// save start idle time
			this.idleTimeStart = this.currentSystemTime;

		} else {

			// increment total idle time with already started interval
			if (this.idleTimeStart != 0) {

				this.trackStats.updateTotalIdleTime(this.currentSystemTime - this.idleTimeStart);

				if (this.segmentingMode != Constants.SEGMENT_NONE) {
					this.segmentStats.updateTotalIdleTime(this.currentSystemTime - this.idleTimeStart);
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

				this.trackStats.updateTotalIdleTime(this.currentSystemTime - this.idleTimeStart);

				if (this.segmentingMode != Constants.SEGMENT_NONE) {
					this.segmentStats.updateTotalIdleTime(this.currentSystemTime - this.idleTimeStart);
				}

				this.idleTimeStart = 0;
			}

			if (this.pauseTimeStart != 0) {

				this.trackStats.updateTotalPauseTime(this.currentSystemTime - this.pauseTimeStart);

				if (this.segmentingMode != Constants.SEGMENT_NONE) {
					this.segmentStats.updateTotalPauseTime(this.currentSystemTime - this.pauseTimeStart);
				}
			}

			// saving new pause time start
			this.pauseTimeStart = this.currentSystemTime;

		} else {

			if (this.pauseTimeStart != 0) {

				this.trackStats.updateTotalPauseTime(this.currentSystemTime - this.pauseTimeStart);

				if (this.segmentingMode != Constants.SEGMENT_NONE) {
					this.segmentStats.updateTotalPauseTime(this.currentSystemTime - this.pauseTimeStart);
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
		if (location.hasAccuracy() && location.getAccuracy() > minAccuracy) {
			return;
		}

		// record points only if distance between 2 consecutive points is
		// greater than min_distance
		// if new segment just started we may not add new points for it
		if (this.lastRecordedLocation == null) {

			this.trackStats.recordTrackPoint(location, this.segmentIndex);
			this.lastRecordedLocation = location;

			pointsCount++;

			if (this.segmentingMode != Constants.SEGMENT_NONE) {
				this.segmentStats.incPointsCount();
			}

		} else {

			if (this.lastRecordedLocation.distanceTo(location) >= minDistance) {

				this.trackStats.recordTrackPoint(location, this.segmentIndex);
				this.lastRecordedLocation = location;

				pointsCount++;

				if (this.segmentingMode != Constants.SEGMENT_NONE) {
					this.segmentStats.incPointsCount();
				}
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

		String[] tmpArr = app.getPreferences().getString(segmentIntervalsKey, "").split(",");

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

		if (this.trackStats.getDistance() / this.getNextSegment() > 1) {

			this.addNewSegment();

		}

	}

	public void segmentTrackByTime() {

		if (this.trackStats.getMovingTime() / this.getNextSegment() > 1) {

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

	/**
	 * Insert current segment to db and create new one statistics object
	 */
	private void addNewSegment() {

		//this.segmentStats.insertSegment(this.trackStats.getTrack().getId(), this.segmentIndex);
		this.segmentStats.updateNewSegment();

		this.segmentIndex++;

		this.segmentStats = new SegmentStats(app, this.trackStats.getTrack().getId(), this.segmentIndex);

	}

	/**
	 * Track is being recorded if track statistics object exists
	 */
	public boolean isRecording() {
		return this.trackStats != null;
	}

	public boolean isRecordingPaused() {
		return this.recordingPaused;
	}

	public int getPointsCount() {
		return pointsCount;
	}

	public TrackStats getTrackStats() {
		return trackStats;
	}

	/**
	 * Returns number of segments created for the track
	 */
	public int getSegmentsCount() {
		return segmentIndex + 1;
	}

}
