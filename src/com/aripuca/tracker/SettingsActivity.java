package com.aripuca.tracker;

import android.app.backup.BackupManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.aripuca.tracker.utils.ArrayUtils;
import com.google.analytics.tracking.android.EasyTracker;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	/**
	 * Reference to Application object
	 */
	protected App app;

	protected SharedPreferences preferences;

	/**
	 * Called when the activity created
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.settings);

		// reference to application object
		app = ((App) getApplicationContext());

		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		preferences.registerOnSharedPreferenceChangeListener(this);

		// Initializing preferences with current values
		// onSharedPreferenceChanged(preferences, "user_name");
		// onSharedPreferenceChanged(preferences, "user_password");
		// onSharedPreferenceChanged(preferences, "sync_url");

		onSharedPreferenceChanged(preferences, getString(R.string.settings_coord_units));
		onSharedPreferenceChanged(preferences, getString(R.string.settings_distance_units));
		onSharedPreferenceChanged(preferences, getString(R.string.settings_speed_units));
		onSharedPreferenceChanged(preferences, getString(R.string.settings_elevation_units));

		onSharedPreferenceChanged(preferences, getString(R.string.settings_segmenting_mode));

		// setting listener for true north changes
		CheckBoxPreference trueNorthPreference = (CheckBoxPreference) findPreference("true_north");
		trueNorthPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {

				// enabling/disabling "show_magnetic" preference
				CheckBoxPreference showMagnetic = (CheckBoxPreference) findPreference("show_magnetic");
				showMagnetic.setEnabled(Boolean.parseBoolean(newValue.toString()));

				return true;
			}
		});

		// setting listener for distance units changes
		ListPreference distanceUnitsPreference = (ListPreference) findPreference("distance_units");
		distanceUnitsPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				updatePreferenceLists(newValue.toString());
				return true;
			}
		});

		// update preference lists dependent on distance units
		String distanceUnit = distanceUnitsPreference.getValue();
		updatePreferenceLists(distanceUnit);

		CheckBoxPreference trueNorth = (CheckBoxPreference) findPreference("true_north");
		CheckBoxPreference showMagnetic = (CheckBoxPreference) findPreference("show_magnetic");
		showMagnetic.setEnabled(trueNorth.isChecked());

	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		super.onStart();
		
		EasyTracker.getInstance(this).activityStart(this);		
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		super.onStop();
		
		EasyTracker.getInstance(this).activityStop(this);
	}
	
	/**
	 * update preference lists dependent on distance units
	 */
	private void updatePreferenceLists(String distanceUnit) {

		// changing labels for min_accuracy
		final ListPreference minAccuracy = (ListPreference) findPreference("min_accuracy");
		minAccuracy
				.setEntries(distanceUnit.equals("km") ? R.array.min_accuracy_labels : R.array.min_accuracy_labels_ft);

		// changing labels for min_distance
		final ListPreference minDistance = (ListPreference) findPreference("min_distance");
		minDistance
				.setEntries(distanceUnit.equals("km") ? R.array.min_distance_labels : R.array.min_distance_labels_ft);

		// changing labels for wpt_min_distance
		final ListPreference wptMinDistance = (ListPreference) findPreference("wpt_min_distance");
		wptMinDistance.setEntries(distanceUnit.equals("km") ? R.array.wpt_min_distance_labels
				: R.array.wpt_min_distance_labels_ft);


		//updateSegmentingModeEntries(distanceUnit);
		
	}
	
	private void updateSegmentingModeEntries(String distanceUnit) {

		// display current segmenting info
 		final ListPreference segmentingMode = (ListPreference) findPreference("segmenting_mode");

		CharSequence[] entries = segmentingMode.getEntries();
		for (int i = 0; i < entries.length; i++) {

			switch (i) {
				case 2:
					entries[i] = entries[i] + " (" + preferences.getString("segment_distance", "5") + " "
							+ distanceUnit + ")";
				break;
				case 3:
					entries[i] = entries[i] + " (" + preferences.getString("segment_time", "5") + " "
							+ getString(R.string.minute) + ")";
				break;
				default:
			}
		}
		segmentingMode.setEntries(entries);
		
	}

	/**
	 *
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

		Preference pref = findPreference(key);

		// ----------------------------------------------------------------------
		// validate user input
		if (key.equals("segment_custom_1") || key.equals("segment_custom_2")) {

			if (pref instanceof EditTextPreference) {

				EditTextPreference textPref = (EditTextPreference) pref;

				String[] tmpArr = textPref.getText().split(",");

				for (int i = 0; i < tmpArr.length; i++) {

					try {

						// check if values entered are in ascending order and
						// unique
						if (tmpArr.length > 1 && i < tmpArr.length - 1) {
							if (Double.parseDouble(tmpArr[i]) >= Double.parseDouble(tmpArr[i + 1])) {
								textPref.setText("5,10,15,20");
								return;
							}
						} else {
							// check number format only
							Double.parseDouble(tmpArr[i]);
						}

					} catch (NumberFormatException e) {
						// TODO: show error message to user with explanation
						// setting default values
						textPref.setText("5,10,15,20");
						return;
					}
				}

			}
		}
		// ----------------------------------------------------------------------

		if (pref instanceof ListPreference) {

			String[] prefKeys = { "speed_units", "distance_units", "elevation_units", "coord_units", "segmenting_mode" };
			// show set values only for defined keys
			if (!ArrayUtils.contains(prefKeys, key)) {
				return;
			}

			ListPreference listPref = (ListPreference) pref;
			listPref.setSummary(listPref.getEntry());

		}

		// requesting backup to the cloud
		BackupManager bm = new BackupManager(this);
		bm.dataChanged();

		/*
		 * if (pref instanceof EditTextPreference) { EditTextPreference textPref = (EditTextPreference) pref; if
		 * (textPref.getText() == null || textPref.getText().equals("")) { textPref.setSummary("not set"); } else { if
		 * (textPref.getKey().equals("user_password")) { textPref.setSummary(""); } else { // all fields except password
		 * will display current value textPref.setSummary(textPref.getText()); } } }
		 */

	}

}