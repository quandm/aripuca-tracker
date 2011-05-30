package com.aripuca.tracker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import com.aripuca.tracker.util.Utils;
import com.google.android.maps.GeoPoint;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

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
	
	private ArrayList<Integer> segmentIds;

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

	}

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

				Log.d(Constants.TAG, "Segment id: " + cursor.getInt(cursor.getColumnIndex("_id")));

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

		// if user changes application settings (ex: measurement units) from this activity
		// call of update here will display correct units 
		update();

		super.onResume();
	}

	/**
	 * Update track details view
	 */
	protected void update() {

		// measuring units
		String speedUnit = myApp.getPreferences().getString("speed_units", "kph");
		String distanceUnit = myApp.getPreferences().getString("distance_units", "km");
		String elevationUnit = myApp.getPreferences().getString("elevation_units", "m");

		// get track data
		String sql = "SELECT * FROM tracks WHERE tracks._id=" + this.trackId;

		Cursor cursor = myApp.getDatabase().rawQuery(sql, null);
		cursor.moveToFirst();

		float distance = cursor.getFloat(cursor.getColumnIndex("distance"));

		// average speed in meters per second
		float averageSpeed = distance / (cursor.getLong(cursor.getColumnIndex("total_time")) / 1000f);

		long movingTime = cursor.getLong(cursor.getColumnIndex("moving_time"));

		float averageMovingSpeed = 0;
		if (movingTime > 0 && distance > 0) {
			averageMovingSpeed = distance / (movingTime / 1000f);
		}

		float maxSpeed = cursor.getFloat(cursor.getColumnIndex("max_speed"));

		((TextView) findViewById(R.id.title)).setText(cursor.getString(cursor.getColumnIndex("title")));

		((TextView) findViewById(R.id.descr)).setText(cursor.getString(cursor.getColumnIndex("descr")));

		((TextView) findViewById(R.id.distance)).setText(Utils.formatDistance(
				cursor.getInt(cursor.getColumnIndex("distance")), distanceUnit));

		((TextView) findViewById(R.id.totalTime)).setText(Utils.formatInterval(
				cursor.getLong(cursor.getColumnIndex("total_time")), true));

		((TextView) findViewById(R.id.movingTime)).setText(Utils.formatInterval(
				cursor.getLong(cursor.getColumnIndex("moving_time")), true));

		((TextView) findViewById(R.id.idleTime)).setText(Utils.formatInterval(
				cursor.getLong(cursor.getColumnIndex("total_time"))
						- cursor.getLong(cursor.getColumnIndex("moving_time")), true));

		// --------------------------------------------------------

		((TextView) findViewById(R.id.averageSpeed)).setText(Utils.formatSpeed(averageSpeed, speedUnit));

		((TextView) findViewById(R.id.averageMovingSpeed)).setText(Utils.formatSpeed(averageMovingSpeed, speedUnit));

		((TextView) findViewById(R.id.maxSpeed)).setText(Utils.formatSpeed(maxSpeed, speedUnit));

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

		((TextView) findViewById(R.id.maxElevation)).setText(Utils.formatElevation(
				cursor.getFloat(cursor.getColumnIndex("max_elevation")), elevationUnit));

		((TextView) findViewById(R.id.minElevation)).setText(Utils.formatElevation(
				cursor.getFloat(cursor.getColumnIndex("min_elevation")), elevationUnit));

		((TextView) findViewById(R.id.elevationGain))
				.setText(Utils.formatElevation(
						cursor.getFloat(cursor.getColumnIndex("elevation_gain")), elevationUnit));

		((TextView) findViewById(R.id.elevationLoss))
				.setText(Utils.formatElevation(
						cursor.getFloat(cursor.getColumnIndex("elevation_loss")), elevationUnit));

		((TextView) findViewById(R.id.startTime)).setText((new SimpleDateFormat("yyyy-MM-dd H:mm")).format(cursor
				.getLong(cursor.getColumnIndex("start_time"))));

		((TextView) findViewById(R.id.finishTime)).setText((new SimpleDateFormat("yyyy-MM-dd H:mm")).format(cursor
				.getLong(cursor.getColumnIndex("finish_time"))));

		cursor.close();

	}

	/**
	 * Update track details view
	 */
	protected void updateSegment() {

		// measuring units
		String speedUnit = myApp.getPreferences().getString("speed_units", "kph");
		String distanceUnit = myApp.getPreferences().getString("distance_units", "km");
		String elevationUnit = myApp.getPreferences().getString("elevation_units", "m");

		// get track data
		String sql = "SELECT * FROM segments WHERE track_id=" + this.trackId;

		Cursor cursor = myApp.getDatabase().rawQuery(sql, null);
		cursor.moveToFirst();

		float distance = cursor.getFloat(cursor.getColumnIndex("distance"));

		// average speed in meters per second
		float averageSpeed = distance / (cursor.getLong(cursor.getColumnIndex("total_time")) / 1000f);

		long movingTime = cursor.getLong(cursor.getColumnIndex("moving_time"));

		float averageMovingSpeed = 0;
		if (movingTime > 0 && distance > 0) {
			averageMovingSpeed = distance / (movingTime / 1000f);
		}

		float maxSpeed = cursor.getFloat(cursor.getColumnIndex("max_speed"));

		((TextView) findViewById(R.id.descr)).setText(cursor.getString(cursor.getColumnIndex("count")));

		((TextView) findViewById(R.id.distance)).setText(Utils.formatDistance(
				cursor.getInt(cursor.getColumnIndex("distance")), distanceUnit));

		((TextView) findViewById(R.id.totalTime)).setText(Utils.formatInterval(
				cursor.getLong(cursor.getColumnIndex("total_time")), true));

		((TextView) findViewById(R.id.movingTime)).setText(Utils.formatInterval(
				cursor.getLong(cursor.getColumnIndex("moving_time")), true));

		((TextView) findViewById(R.id.idleTime)).setText(Utils.formatInterval(
				cursor.getLong(cursor.getColumnIndex("total_time"))
						- cursor.getLong(cursor.getColumnIndex("moving_time")), true));

		// --------------------------------------------------------

		((TextView) findViewById(R.id.averageSpeed)).setText(Utils.formatSpeed(averageSpeed, speedUnit));

		((TextView) findViewById(R.id.averageMovingSpeed)).setText(Utils.formatSpeed(averageMovingSpeed, speedUnit));

		((TextView) findViewById(R.id.maxSpeed)).setText(Utils.formatSpeed(maxSpeed, speedUnit));

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

		((TextView) findViewById(R.id.maxElevation)).setText(Utils.formatElevation(
				cursor.getFloat(cursor.getColumnIndex("max_elevation")), elevationUnit));

		((TextView) findViewById(R.id.minElevation)).setText(Utils.formatElevation(
				cursor.getFloat(cursor.getColumnIndex("min_elevation")), elevationUnit));

		((TextView) findViewById(R.id.elevationGain))
				.setText(Utils.formatElevation(
						cursor.getFloat(cursor.getColumnIndex("elevation_gain")), elevationUnit));

		((TextView) findViewById(R.id.elevationLoss))
				.setText(Utils.formatElevation(
						cursor.getFloat(cursor.getColumnIndex("elevation_loss")), elevationUnit));

		((TextView) findViewById(R.id.startTime)).setText((new SimpleDateFormat("yyyy-MM-dd H:mm")).format(cursor
				.getLong(cursor.getColumnIndex("start_time"))));

		((TextView) findViewById(R.id.finishTime)).setText((new SimpleDateFormat("yyyy-MM-dd H:mm")).format(cursor
				.getLong(cursor.getColumnIndex("finish_time"))));

		cursor.close();

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

		//		MenuItem mi = menu.findItem(R.id.showSegments);

		return true;
	}

	/**
	 * Process main activity menu
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		// Handle item selection
		switch (item.getItemId()) {

			case R.id.showSegments:

				return true;

			case R.id.showOnMap:

				Intent i = new Intent(this, MyMapActivity.class);

				// using Bundle to pass track id into new activity
				Bundle b = new Bundle();
				b.putInt("mode", Constants.SHOW_TRACK);
				b.putLong("track_id", this.trackId);

				i.putExtras(b);
				startActivity(i);

				return true;

			case R.id.settingsMenuItem:

				startActivity(new Intent(this, SettingsActivity.class));

				return true;

			default:

				return super.onOptionsItemSelected(item);

		}

	}
}
