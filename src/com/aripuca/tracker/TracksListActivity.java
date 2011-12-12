package com.aripuca.tracker;

//TODO: compare 2 tracks on the map, select track to compare
//TODO: show main parameters of the track and segments on the map 

/**
 * Tracks list activity
 */
public class TracksListActivity extends AbstractTracksListActivity {

	// instance initialization block
	{
		sqlSelectAllTracks = "SELECT * FROM tracks WHERE recording=0 AND (activity=0 OR activity IS NULL)";
		listItemResourceId = R.layout.track_list_item;
	}

	@Override
	public void deleteAllTracks() {
		
		// delete from segments table
		String sql = "DELETE FROM segments WHERE track_id IN " +
						"(SELECT track_id FROM tracks WHERE activity=0 OR activity IS NULL);";
		myApp.getDatabase().execSQL(sql);

		// delete from track_points table
		sql = "DELETE FROM track_points WHERE track_id IN " +
				"(SELECT track_id FROM tracks WHERE activity=0 OR activity IS NULL);";
		myApp.getDatabase().execSQL(sql);
		
		// clear track_id in waypoints table
		sql = "UPDATE waypoints SET track_id=NULL WHERE track_id IN " + 
				"(SELECT track_id FROM tracks WHERE activity=0 OR activity IS NULL);";
		myApp.getDatabase().execSQL(sql);
		
		// delete from tracks table
		sql = "DELETE FROM tracks WHERE activity=0 OR activity IS NULL";
		myApp.getDatabase().execSQL(sql);
		
	}
	
}
