package com.aripuca.tracker;

import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.View;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;

import com.aripuca.tracker.view.CompassImage;


public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener  {

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
		onSharedPreferenceChanged(preferences, "user_name");
		onSharedPreferenceChanged(preferences, "user_password");
		onSharedPreferenceChanged(preferences, "sync_url");
		onSharedPreferenceChanged(preferences, "coord_units");
		onSharedPreferenceChanged(preferences, "distance_units");
		onSharedPreferenceChanged(preferences, "speed_units");
		onSharedPreferenceChanged(preferences, "elevation_units");
		
	}
	/**
	 *
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		
		Preference pref = findPreference(key);

	    if (pref instanceof EditTextPreference) {
	    	
	    	EditTextPreference textPref = (EditTextPreference) pref;
	    	
    		if(textPref.getText()==null || textPref.getText().equals("")) {
    			
    			textPref.setSummary("not set");
    			
    		} else {
    			if (textPref.getKey().equals("user_password")) {
    				textPref.setSummary("");
    	    	} else {
    	    		// all fields except password will display current value
    	    		textPref.setSummary(textPref.getText());
    	    	}
    		}
	    	
	    }

	    if (pref instanceof ListPreference) {

	    	ListPreference listPref = (ListPreference) pref;
	    	listPref.setSummary(listPref.getEntry());
	    	
	    }
	    
		// show or hide compass on main activity
		if (key.equals("show_compass") && myApp.getMainActivity().findViewById(R.id.compassImage)!=null) {
			if (sharedPreferences.getBoolean(key, true)) {
				((CompassImage) myApp.getMainActivity().findViewById(R.id.compassImage)).setVisibility(View.VISIBLE);
			} else {
				((CompassImage) myApp.getMainActivity().findViewById(R.id.compassImage)).setVisibility(View.GONE);
			}
		}
		
	}

	
}