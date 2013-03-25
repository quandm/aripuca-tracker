package com.aripuca.tracker;

import com.aripuca.tracker.db.Tracks;

/**
 * Tracks list activity
 */
public class TracksListActivity extends AbstractTracksListActivity {

	// instance initialization block
	{
		this.listItemResourceId = R.layout.track_list_item;
		this.mapMode = Constants.SHOW_SCHEDULED_TRACK;
	}

	@Override
	public void deleteAllTracks() {
		Tracks.deleteAll(app.getDatabase(), 0);
	}

	@Override
	protected void setQuery() {

		if (app.getPreferences().getBoolean("debug_on", false)) {
			sqlSelectAllTracks = "SELECT * FROM tracks WHERE (activity=0 OR activity IS NULL)";
		} else {
			sqlSelectAllTracks = "SELECT * FROM tracks WHERE recording=0 AND (activity=0 OR activity IS NULL)";
		}

	}

}
