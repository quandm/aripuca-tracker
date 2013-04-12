package com.aripuca.tracker;

import android.view.Menu;

import com.aripuca.tracker.db.Tracks;

/**
 * Tracks list activity
 */
public class TracksListActivity extends AbstractTracksListActivity {

	// instance initialization block
	{
		this.listItemResourceId = R.layout.track_list_item;
		this.mapMode = Constants.SHOW_TRACK;
	}

	@Override
	public void deleteAllTracks() {
		Tracks.deleteAll(app.getDatabase(), Constants.ACTIVITY_TRACK);
	}

	@Override
	protected void setQuery() {

		if (app.getPreferences().getBoolean("debug_on", false)) {
			// in debug mode show all track including being recorded ones
			sqlSelectAllTracks = "SELECT * FROM tracks WHERE (activity=" + Constants.ACTIVITY_TRACK
					+ " OR activity IS NULL)";
		} else {
			sqlSelectAllTracks = "SELECT * FROM tracks WHERE recording=0 AND (activity=" + Constants.ACTIVITY_TRACK
					+ " OR activity IS NULL)";
		}

	}
	
	protected void addTrackDetailsMenu(Menu menu) {
		
		menu.add(Menu.NONE, 1, 1, R.string.show_track_details);
		
	}
	

}
