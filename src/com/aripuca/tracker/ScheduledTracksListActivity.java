package com.aripuca.tracker;

/**
 * Scheduled tracks list activity
 */
public class ScheduledTracksListActivity extends AbstractTracksListActivity {

	// instance initialization block
	{
		sqlSelectAllTracks = "SELECT * FROM tracks WHERE recording=0 AND activity=1";
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
}
