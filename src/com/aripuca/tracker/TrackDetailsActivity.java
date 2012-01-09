package com.aripuca.tracker;

import java.util.ArrayList;

import com.aripuca.tracker.R;
import com.aripuca.tracker.app.Constants;
import com.aripuca.tracker.map.MyMapActivity;
import com.aripuca.tracker.util.Utils;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * main application activity
 */
public class TrackDetailsActivity extends Activity {

	/**
	 * Reference to myApp object
	 */
	private MyApp myApp;

	private long trackId;

	private int currentSegment = 0;

	private int numberOfSegments = 0;

	/**
	 * Array of segment table unique ids (_id field)
	 */
	private ArrayList<Integer> segmentIds;

	private OnClickListener prevSegmentButtonClick = new OnClickListener() {
		@Override
		public void onClick(View v) {

			if (numberOfSegments == 0) {
				Toast.makeText(TrackDetailsActivity.this, R.string.no_segments, Toast.LENGTH_SHORT).show();
				return;
			}

			if (currentSegment == 0) {
				currentSegment = numberOfSegments;
				updateSegment(currentSegment);
				return;

			}

			if (currentSegment > 1) {

				currentSegment--;
				updateSegment(currentSegment);

			} else {

				currentSegment = 0;
				updateTrack();

			}

		}

	};

	private OnClickListener nextSegmentButtonClick = new OnClickListener() {
		@Override
		public void onClick(View v) {

			if (numberOfSegments == 0) {
				Toast.makeText(TrackDetailsActivity.this, R.string.no_segments, Toast.LENGTH_SHORT).show();
				return;
			}

			if (currentSegment < numberOfSegments) {

				currentSegment++;
				updateSegment(currentSegment);

			} else {

				currentSegment = 0;
				updateTrack();

			}

		}

	};

	/**
	 * Initialize activity
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		myApp = ((MyApp) getApplicationContext());

		setContentView(R.layout.track_details);

		Bundle b = getIntent().getExtras();

		this.trackId = b.getLong("track_id", 0);

		this.getSegments();

		((Button) findViewById(R.id.prevSegment)).setOnClickListener(prevSegmentButtonClick);
		((Button) findViewById(R.id.nextSegment)).setOnClickListener(nextSegmentButtonClick);

	}

	/**
	 * 
	 */
	private void getSegments() {

		// get track data
		String sql = "SELECT _id FROM segments WHERE track_id=" + this.trackId;

		Cursor cursor = myApp.getDatabase().rawQuery(sql, null);
		cursor.moveToFirst();

		this.numberOfSegments = cursor.getCount();
		Log.d(Constants.TAG, "Number of segments: " + this.numberOfSegments);

		segmentIds = new ArrayList<Integer>();

		if (this.numberOfSegments != 0) {

			while (cursor.isAfterLast() == false) {
				segmentIds.add(cursor.getInt(cursor.getColumnIndex("_id")));
				cursor.moveToNext();
			}
		}

		cursor.close();

	}

	/**
	 * onResume event handler
	 */
	@Override
	protected void onResume() {

		this.updateTrack();

		super.onResume();

	}

	/**
	 * Update track details view
	 */
	protected void updateTrack() {

		// get track data
		String sql = "SELECT tracks.*, COUNT(track_points._id) AS count " +
				" FROM tracks" +
				" LEFT JOIN track_points ON tracks._id = track_points.track_id " +
				" WHERE " +
				" tracks._id=" + this.trackId;

		Cursor cursor = myApp.getDatabase().rawQuery(sql, null);
		cursor.moveToFirst();

		((TextView) findViewById(R.id.title)).setText(cursor.getString(cursor.getColumnIndex("title")));
		((TextView) findViewById(R.id.descr)).setText(cursor.getString(cursor.getColumnIndex("descr")));

		this.updateActivity(cursor);

		cursor.close();

	}

	/**
	 * Update track details view
	 */
	protected void updateSegment(int segmentIndex) {

		int segmentId = segmentIds.get(segmentIndex - 1);

		// get track data
		String sql = "SELECT segments.*, COUNT(track_points._id) AS count FROM segments, track_points WHERE" +
				" segments._id=" + segmentId +
				" AND segments.track_id=" + this.trackId +
				" AND track_points.segment_index=" + (segmentIndex - 1) +
				" AND segments.track_id = track_points.track_id";

		Cursor cursor = myApp.getDatabase().rawQuery(sql, null);
		cursor.moveToFirst();

		((TextView) findViewById(R.id.descr)).setText("Segment: " + segmentIndex);

		this.updateActivity(cursor);

		cursor.close();

	}

	private void updateActivity(Cursor cursor) {

		// measuring units
		String speedUnit = myApp.getPreferences().getString("speed_units", "kph");
		String speedUnitLocalized = Utils.getLocalizedSpeedUnit(TrackDetailsActivity.this, speedUnit);

		String distanceUnit = myApp.getPreferences().getString("distance_units", "km");

		String elevationUnit = myApp.getPreferences().getString("elevation_units", "m");
		String elevationUnitLocalized = Utils.getLocalizedElevationUnit(TrackDetailsActivity.this, elevationUnit);

		// total distance
		float distance = cursor.getFloat(cursor.getColumnIndex("distance"));

		// localized distance unit depends on distance value  
		String distanceUnitLocalized = Utils.getLocalizedDistanceUnit(TrackDetailsActivity.this, distance, distanceUnit);
		
		// average speed
		float averageSpeed = 0;
		long totalTime = cursor.getLong(cursor.getColumnIndex("total_time"));
		if (totalTime != 0) {
			averageSpeed = distance / (totalTime / 1000f);
		}

		// moving average speed
		long movingTime = cursor.getLong(cursor.getColumnIndex("moving_time"));
		float averageMovingSpeed = 0;
		if (movingTime > 0 && distance > 0) {
			averageMovingSpeed = distance / (movingTime / 1000f);
		}

		float maxSpeed = cursor.getFloat(cursor.getColumnIndex("max_speed"));

		// --------------------------------------------------------
		// distance
		// --------------------------------------------------------
		((TextView) findViewById(R.id.distance)).setText(Utils.formatDistance(
				distance, distanceUnit) + " " + distanceUnitLocalized);

		((TextView) findViewById(R.id.pointsCount)).setText(cursor.getString(cursor.getColumnIndex("count")));

		// --------------------------------------------------------
		// times
		// --------------------------------------------------------
		((TextView) findViewById(R.id.totalTime)).setText(Utils.formatInterval(
				cursor.getLong(cursor.getColumnIndex("total_time")), true));

		((TextView) findViewById(R.id.movingTime)).setText(Utils.formatInterval(
				cursor.getLong(cursor.getColumnIndex("moving_time")), true));

		((TextView) findViewById(R.id.idleTime)).setText(Utils.formatInterval(
				cursor.getLong(cursor.getColumnIndex("total_time"))
						- cursor.getLong(cursor.getColumnIndex("moving_time")), true));

		((TextView) findViewById(R.id.startTime)).setText(DateFormat.format("yyyy-MM-dd k:mm", cursor
				.getLong(cursor.getColumnIndex("start_time"))));

		((TextView) findViewById(R.id.finishTime)).setText(DateFormat.format("yyyy-MM-dd k:mm", cursor
				.getLong(cursor.getColumnIndex("finish_time"))));

		// --------------------------------------------------------
		// speed
		// --------------------------------------------------------
		((TextView) findViewById(R.id.averageSpeed)).setText(Utils.formatSpeed(averageSpeed, speedUnit) + " " +
				speedUnitLocalized);

		((TextView) findViewById(R.id.averageMovingSpeed)).setText(Utils.formatSpeed(averageMovingSpeed, speedUnit)
				+ " " +
				speedUnitLocalized);

		((TextView) findViewById(R.id.maxSpeed)).setText(Utils.formatSpeed(maxSpeed, speedUnit) + " " +
				speedUnitLocalized);

		// --------------------------------------------------------
		// pace
		// --------------------------------------------------------
		if (averageSpeed != 0) {
			((TextView) findViewById(R.id.averagePace)).setText(Utils.formatPace(averageSpeed, speedUnit));
		}

		if (averageMovingSpeed != 0) {
			((TextView) findViewById(R.id.averageMovingPace)).setText(
					Utils.formatPace(averageMovingSpeed, speedUnit));
		}

		if (maxSpeed != 0) {
			((TextView) findViewById(R.id.maxPace)).setText(
					Utils.formatPace(maxSpeed, speedUnit));
		}

		// --------------------------------------------------------
		// elevation
		// --------------------------------------------------------
		((TextView) findViewById(R.id.maxElevation)).setText(Utils.formatElevation(
				cursor.getFloat(cursor.getColumnIndex("max_elevation")), elevationUnit) + " " +
				elevationUnitLocalized);

		((TextView) findViewById(R.id.minElevation)).setText(Utils.formatElevation(
				cursor.getFloat(cursor.getColumnIndex("min_elevation")), elevationUnit) + " " + elevationUnitLocalized);

		((TextView) findViewById(R.id.elevationGain))
				.setText(Utils.formatElevation(
						cursor.getFloat(cursor.getColumnIndex("elevation_gain")), elevationUnit) + " " +
						elevationUnitLocalized);

		((TextView) findViewById(R.id.elevationLoss))
				.setText(Utils.formatElevation(
						cursor.getFloat(cursor.getColumnIndex("elevation_loss")), elevationUnit) + " " +
						elevationUnitLocalized);

	}

	/**
	 * onCreateOptionsMenu handler
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.track_details_menu, menu);
		return true;
	}

	/**
	 * Make all changes to the menu before it loads
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		// MenuItem mi = menu.findItem(R.id.showSegments);

		return true;
	}

	/**
	 * Process main activity menu
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		Intent i;
		Bundle b;
		
		// Handle item selection
		switch (item.getItemId()) {

			case R.id.showOnMap:

				i = new Intent(this, MyMapActivity.class);

				// using Bundle to pass track id into new activity
				b = new Bundle();
				b.putInt("mode", Constants.SHOW_TRACK);
				b.putLong("track_id", this.trackId);

				i.putExtras(b);
				startActivity(i);

				return true;

			case R.id.showTrackChart:

				i = new Intent(this, TrackChartActivity.class);

				// using Bundle to pass track id into new activity
				b = new Bundle();
				b.putLong("track_id", this.trackId);

				i.putExtras(b);
				startActivity(i);

				return true;
				
			default:

				return super.onOptionsItemSelected(item);

		}

	}
}
