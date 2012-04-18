package com.aripuca.tracker.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import com.aripuca.tracker.Constants;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;


public class AppLog {

	private Context context;
	
	private String appDir;
	
	private static final int ERROR = 1;//;
	private static final int WARNING = 2;//;
	private static final int INFO = 3;//;
	private static final int DEBUG = 4;//;
	
	private String[] loggingLevels = new String[]{"no logging", "errors", "warning", "info", "debug"};
	
	/**
	 * Private constructor
	 */
	public AppLog(Context context) {
		
		this.context = context;
		// set application external storage folder
		appDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Constants.APP_NAME;
	}
	
	private void log(int loggingLevel, String message) {
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		if (loggingLevel > Integer.parseInt(preferences.getString("logging_level", "0"))) {
			return;
		}
	
		String fileName = loggingLevels[loggingLevel]+"_"+DateFormat.format("yyyy-MM-dd", new Date()) + ".log";

		StringBuilder sb = new StringBuilder();
		sb.append(DateFormat.format("yyyy-MM-dd kk-mm-ss", new Date()));
		sb.append(" | ");
		sb.append(message);

		File logFile = new File(appDir + "/logs/" + fileName);

		if (!logFile.exists()) {
			try {
				logFile.createNewFile();
			} catch (IOException e) {
				return;
			}
		}

		try {
			BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
			buf.append(sb.toString());
			buf.newLine();
			buf.close();
		} catch (IOException e) {
			return;
		}
		
	}
	
	public void e(String message) {
		this.log(ERROR, message);
	}
	public void w(String message) {
		this.log(WARNING, message);
	}
	public void i(String message) {
		this.log(INFO, message);
	}
	public void d(String message) {
		this.log(DEBUG, message);
	}
	
}
