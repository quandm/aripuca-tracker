package com.aripuca.tracker.io;

import com.aripuca.tracker.R;
import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

public class MyBackupAgent extends BackupAgentHelper {

	static final String SETTINGS_BACKUP_KEY = "app_settings";

	@Override
	public void onCreate() {

		SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this,
				getString(R.string.settings_wake_lock),
				getString(R.string.settings_quick_help),
				getString(R.string.settings_speed_units),
				getString(R.string.settings_distance_units),
				getString(R.string.settings_elevation_units),
				getString(R.string.settings_coord_units),
				getString(R.string.settings_true_north),
				getString(R.string.settings_show_magnetic),
				getString(R.string.settings_compass_vibration),
				getString(R.string.settings_waypoint_default_description),
				getString(R.string.settings_min_distance),
				getString(R.string.settings_min_accuracy),
				getString(R.string.settings_segmenting_mode),
				getString(R.string.settings_segment_distance),
				getString(R.string.settings_segment_time),
				getString(R.string.settings_segment_custom_1),
				getString(R.string.settings_segment_custom_2),
				getString(R.string.settings_wpt_request_interval),
				getString(R.string.settings_wpt_stop_recording_after),
				getString(R.string.settings_wpt_gps_fix_wait_time),
				getString(R.string.settings_wpt_min_accuracy),
				getString(R.string.settings_wpt_min_distance),
				getString(R.string.settings_logging_level),
				getString(R.string.settings_debug_on)
				);

		addHelper(SETTINGS_BACKUP_KEY, helper);

	}

}