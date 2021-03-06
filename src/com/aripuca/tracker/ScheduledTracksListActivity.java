package com.aripuca.tracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

import com.aripuca.tracker.db.Tracks;

/**
 * Scheduled tracks list activity
 */
public class ScheduledTracksListActivity extends AbstractTracksListActivity {

	// instance initialization block
	{
		this.listItemResourceId = R.layout.scheduled_track_list_item;
		this.mapMode = Constants.SHOW_SCHEDULED_TRACK;
	}

	@Override
	public void deleteAllTracks() {
		Tracks.deleteAll(app.getDatabase(), Constants.ACTIVITY_SCHEDULED_TRACK);
	}

	/**
	 * Start the track details activity
	 * 
	 * @param id
	 *            Track id
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

	@Override
	protected void setQuery() {

		if (app.getPreferences().getBoolean("debug_on", false)) {
			// in debug mode show all track including being recorded ones
			sqlSelectAllTracks = "SELECT * FROM tracks WHERE activity=" + Constants.ACTIVITY_SCHEDULED_TRACK;
		} else {
			sqlSelectAllTracks = "SELECT * FROM tracks WHERE recording=0 AND activity="
					+ Constants.ACTIVITY_SCHEDULED_TRACK;
		}

	}

	protected void addTrackDetailsMenu(Menu menu) {
		
		menu.add(Menu.NONE, 1, 1, R.string.show_track_points);
		
	}
	
}
