package com.aripuca.tracker;

import com.aripuca.tracker.R;
import com.aripuca.tracker.app.Constants;
import com.aripuca.tracker.io.TrackExportTask;
import com.aripuca.tracker.io.TrackGpxExportTask;
import com.aripuca.tracker.io.TrackKmlExportTask;
import com.aripuca.tracker.map.MyMapActivity;
import com.aripuca.tracker.util.Utils;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

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
