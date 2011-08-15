package com.aripuca.tracker;

import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;

import android.preference.Preference.OnPreferenceChangeListener;

import com.aripuca.tracker.util.ArrayUtils;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	protected MyApp myApp;

	/**
	 * Called when the activity created
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.settings);

		// reference to application object
		myApp = ((MyApp) getApplicationContext());

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		preferences.registerOnSharedPreferenceChangeListener(this);

		// Initializing preferences with current values
		//		onSharedPreferenceChanged(preferences, "user_name");
		//		onSharedPreferenceChanged(preferences, "user_password");
		//		onSharedPreferenceChanged(preferences, "sync_url");

		onSharedPreferenceChanged(preferences, "coord_units");
		onSharedPreferenceChanged(preferences, "distance_units");
		onSharedPreferenceChanged(preferences, "speed_units");
		onSharedPreferenceChanged(preferences, "elevation_units");

		onSharedPreferenceChanged(preferences, "segmenting_mode");

		// setting listener for application language changes
		ListPreference languagePreference = (ListPreference) findPreference("language");
		languagePreference.setOnPreferenceChangeListener(
				new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference, Object newValue) {
						
						// let's broadcast compass data to any activity waiting for updates
						Intent intent = new Intent("com.aripuca.tracker.LANGUAGE_UPDATES_ACTION");
						// broadcasting compass updates 
						sendBroadcast(intent);
						
						return true;
					}
				});
		
		// setting listener for distance units changes
		ListPreference distanceUnitsPreference = (ListPreference) findPreference("distance_units");
		distanceUnitsPreference.setOnPreferenceChangeListener(
				new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference, Object newValue) {
						updatePreferenceLists(newValue.toString());
						return true;
					}
				});

		// update preference lists dependent on distance units
		String distanceUnit = distanceUnitsPreference.getValue();
		updatePreferenceLists(distanceUnit);

	}

	/**
	 * update preference lists dependent on distance units
	 */
	private void updatePreferenceLists(String distanceUnit) {

		// changing labels for min_accuracy 
		final ListPreference minAccuracy = (ListPreference) findPreference("min_accuracy");
		minAccuracy.setEntries(distanceUnit.equals("km") ? R.array.min_accuracy_labels : R.array.min_accuracy_labels_ft);

		// changing labels for min_distance 
		final ListPreference minDistance = (ListPreference) findPreference("min_distance");
		minDistance.setEntries(distanceUnit.equals("km") ? R.array.min_distance_labels : R.array.min_distance_labels_ft);
	}

	/**
	 *
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

		Preference pref = findPreference(key);

		//----------------------------------------------------------------------
		// validate user input 
		if (key.equals("segment_custom_1") || key.equals("segment_custom_2")) {

			if (pref instanceof EditTextPreference) {

				EditTextPreference textPref = (EditTextPreference) pref;

				String[] tmpArr = textPref.getText().split(",");

				for (int i = 0; i < tmpArr.length; i++) {

					try {

						// check if values entered are in ascending order and unique
						if (tmpArr.length > 1 && i < tmpArr.length - 1) {
							if (Double.parseDouble(tmpArr[i]) >= Double.parseDouble(tmpArr[i + 1])) {
								textPref.setText("5,10,15,20");
								return;
							}
						} else {
							// check number format only
							double d = Double.parseDouble(tmpArr[i]);
						}

					} catch (NumberFormatException e) {
						//TODO: show error message to user with explanation
						// setting default values
						textPref.setText("5,10,15,20");
						return;
					}
				}

			}
		}
		//----------------------------------------------------------------------

		if (pref instanceof ListPreference) {

			String[] prefKeys = { "speed_units", "distance_units", "elevation_units", "coord_units", "segmenting_mode" };
			// show set values only for defined keys 
			if (!ArrayUtils.contains(prefKeys, key)) {
				return;
			}

			ListPreference listPref = (ListPreference) pref;
			listPref.setSummary(listPref.getEntry());

		}

		/*
		 * if (pref instanceof EditTextPreference) {
		 * 
		 * EditTextPreference textPref = (EditTextPreference) pref;
		 * 
		 * if (textPref.getText() == null || textPref.getText().equals("")) {
		 * 
		 * textPref.setSummary("not set");
		 * 
		 * } else { if (textPref.getKey().equals("user_password")) {
		 * textPref.setSummary(""); } else { // all fields except password will
		 * display current value textPref.setSummary(textPref.getText()); } }
		 * 
		 * }
		 */

	}

}