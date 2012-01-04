package com.aripuca.tracker.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import com.aripuca.tracker.MyApp;

import android.content.Context;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;


public class AppLog {

	private static AppLog instance = null;
	
	private String appDir;
	
	private MyApp myApp;
	
	private static final int ERROR = 1;//;
	private static final int WARNING = 2;//;
	private static final int INFO = 3;//;
	private static final int DEBUG = 4;//;
	
	private String[] loggingLevels = new String[]{"no logging", "errors", "warning", "info", "debug"};
	
	/**
	 * Singleton pattern
	 */
	public static AppLog getInstance(MyApp myApp) {

		if (instance == null) {
			instance = new AppLog(myApp);
		}

		return instance;
	}
	
	/**
	 * Private constructor
	 */
	private AppLog(MyApp myApp) {
		
		this.myApp = myApp; 

		// set application external storage folder
		appDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Constants.APP_NAME;

		
		
	}
	
	private void log(int loggingLevel, String message) {
		
		if (loggingLevel > Integer.parseInt(myApp.getPreferences().getString("logging_level", "0"))) {
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
