package com.aripuca.tracker;

import android.content.Intent;
import android.os.Bundle;

/**
 * Scheduled tracks list activity
 */
public class ScheduledTracksListActivity extends AbstractTracksListActivity {

	// instance initialization block
	{
//		sqlSelectAllTracks = "SELECT tracks.*, COUNT(track_points._id) AS count FROM tracks " +
//				"LEFT JOIN track_points ON tracks._id = track_points.track_id "+
//				"WHERE recording=0 AND activity=1";

		sqlSelectAllTracks = "SELECT * FROM tracks WHERE recording=0 AND activity=1";
		
		listItemResourceId = R.layout.scheduled_track_list_item;
		
		infoDisplayed = false;
	}

	@Override
	public void deleteAllTracks() {
		
		// delete from segments table
		String sql = "DELETE FROM segments WHERE track_id IN " +
						"(SELECT track_id FROM tracks WHERE activity=1);";
		myApp.getDatabase().execSQL(sql);

		// delete from track_points table
		sql = "DELETE FROM track_points WHERE track_id IN " +
				"(SELECT track_id FROM tracks WHERE activity=1);";
		myApp.getDatabase().execSQL(sql);
		
		// clear track_id in waypoints table
		sql = "UPDATE waypoints SET track_id=NULL WHERE track_id IN " + 
				"(SELECT track_id FROM tracks WHERE activity=1);";
		myApp.getDatabase().execSQL(sql);
		
		// delete from tracks table
		sql = "DELETE FROM tracks WHERE activity=1";
		myApp.getDatabase().execSQL(sql);
		
	}
	
	
	/**
	 * Start the track details activity
	 * 
	 * @param id Track id
	 */
	@Override
	protected void viewTrackDetails(long id) {

		Intent intent = new Intent(this, TrackpointsListActivity.class);

		// using Bundle to pass track id into new activity
		Bundle bundle = new Bundle();
		bundle.putLong("track_id", id);

		intent.putExtras(bundle);

		startActivity(intent);

	}
	
}
