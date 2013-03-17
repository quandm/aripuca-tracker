package com.aripuca.tracker;

/**
 * Tracks list activity
 */
public class TracksListActivity extends AbstractTracksListActivity {

	// instance initialization block
	{
		listItemResourceId = R.layout.track_list_item;
	}

	@Override
	public void deleteAllTracks() {

		// delete from segments table
		String sql = "DELETE FROM segments WHERE track_id IN "
				+ "(SELECT track_id FROM tracks WHERE activity=0 OR activity IS NULL);";
		app.getDatabase().execSQL(sql);

		// delete from track_points table
		sql = "DELETE FROM track_points WHERE track_id IN "
				+ "(SELECT track_id FROM tracks WHERE activity=0 OR activity IS NULL);";
		app.getDatabase().execSQL(sql);

		// clear track_id in waypoints table
		sql = "UPDATE waypoints SET track_id=NULL WHERE track_id IN "
				+ "(SELECT track_id FROM tracks WHERE activity=0 OR activity IS NULL);";
		app.getDatabase().execSQL(sql);

		// delete from tracks table
		sql = "DELETE FROM tracks WHERE activity=0 OR activity IS NULL";
		app.getDatabase().execSQL(sql);

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
