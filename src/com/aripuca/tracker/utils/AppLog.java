package com.aripuca.tracker.utils;

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

	private static final int ERROR = 1;
	private static final int WARNING = 2;
	private static final int INFO = 3;
	private static final int DEBUG = 4;

	private static String[] loggingLevels = new String[] { "no logging", "errors", "warning", "info", "debug" };

	private static void log(Context context, int loggingLevel, String message) {

		String appDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Constants.APP_NAME;

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		int currentLoggingLevel = Integer.parseInt(preferences.getString("logging_level", "0"));

		if (loggingLevel > currentLoggingLevel) {
			return;
		}

		String fileName = loggingLevels[loggingLevel] + "_" + DateFormat.format("yyyy-MM-dd", new Date()) + ".log";

		StringBuilder sb = new StringBuilder();
		sb.append(DateFormat.format("yyyy-MM-dd kk-mm-ss", new Date()));
		sb.append(" | ");
		sb.append(message);

		File logFile = new File(appDir + "/" + Constants.PATH_LOGS + "/" + fileName);

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

	public static void e(Context context, String message) {
		AppLog.log(context, ERROR, message);
	}

	public static void w(Context context, String message) {
		AppLog.log(context, WARNING, message);
	}

	public static void i(Context context, String message) {
		AppLog.log(context, INFO, message);
	}

	public static void d(Context context, String message) {
		AppLog.log(context, DEBUG, message);
	}

}
