package com.aripuca.tracker.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import com.aripuca.tracker.R;

import android.app.Activity;
import android.content.Context;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.view.Display;
import android.view.Surface;

public class Utils {

	public final static long ONE_SECOND = 1000;
	public final static long SECONDS = 60;
	public final static long ONE_MINUTE = ONE_SECOND * 60;
	public final static long MINUTES = 60;
	public final static long ONE_HOUR = ONE_MINUTE * 60;
	public final static long HOURS = 24;
	public final static long ONE_DAY = ONE_HOUR * 24;

	public static final char DEGREE_CHAR = (char) 0x00B0;
	public static final char PLUSMINUS_CHAR = (char) 0x00B1;

	protected static final double KM_TO_MI = 0.621371192;
	protected static final double M_TO_FT = 3.2808399;
	protected static final double MI_TO_M = 1609.344;
	protected static final double MI_TO_FEET = 5280.0;
	protected static final double KMH_TO_MPH = 0.621371192;
	protected static final double KMH_TO_KNOTS = 0.539957; // 1 knot = 1
															// nautical mile
															// (1.852km) per
															// hour

	public static String formatNumber(Object value, int max) {
		return Utils.formatNumber(value, max, 0);
	}

	/**
	 * Number formatting according to default locale
	 */
	public static String formatNumber(Object value, int max, int min) {

		NumberFormat f = NumberFormat.getInstance();
		f.setMaximumFractionDigits(max);
		f.setMinimumFractionDigits(min);
		f.setGroupingUsed(false);

		try {
			return f.format(value);
		} catch (IllegalArgumentException e) {
			return "err";
		}

	}

	/**
	 * Number formatting according to US locale. Required for export to GPX
	 */
	public static String formatNumberUS(Object value, int max) {

		NumberFormat f = NumberFormat.getInstance(Locale.US);
		f.setMaximumFractionDigits(max);
		f.setGroupingUsed(false);

		try {
			return f.format(value);
		} catch (IllegalArgumentException e) {
			return "err";
		}

	}

	/**
	 * Format distance based on unit type
	 * 
	 * @param float value
	 * @return String
	 */
	public static String formatDistance(float value, String unit) {

		if (unit.equals("km")) {

			if (value > 100000) {
				return Utils.formatNumber(value / 1000, 1);
			}

			// convert to km
			if (value > 1000) {
				return Utils.formatNumber(value / 1000, 2, 1);
			}

			// leave value in meters
			return Utils.formatNumber(value, 0);

		}

		if (unit.equals("mi")) {

			if (value > 100 * MI_TO_M) {
				return Utils.formatNumber(value / 1000, 1);
			}

			// convert to miles
			if (value > MI_TO_M) {
				return Utils.formatNumber(value / MI_TO_M, 2, 1);
			}

			// value is in feet
			return Utils.formatNumber(value * M_TO_FT, 0);
		}

		return "";

	}

	public static String getLocalizedDistanceUnit(Context context, float value, String unit) {

		if (unit.equals("km")) {

			if (value > 1000) {
				return context.getString(R.string.km);
			}
			return context.getString(R.string.m);
		}

		if (unit.equals("mi")) {
			if (value > MI_TO_M) {
				return context.getString(R.string.mi);
			}
			return context.getString(R.string.ft);
		}

		return "";

	}

	public static String formatElevation(double value, String unit) {

		if (unit.equals("m")) {
			return Utils.formatNumber(value, 0);
		}

		if (unit.equals("ft")) {
			return Utils.formatNumber(value * M_TO_FT, 0);
		}

		return "";
	}

	public static String getLocalizedElevationUnit(Context context, String unit) {

		if (unit.equals("m")) {
			return context.getString(R.string.m);
		}

		if (unit.equals("ft")) {
			return context.getString(R.string.ft);
		}

		return "";
	}

	/**
	 * Format speed value (kph, mph or knots)
	 */
	public static String formatSpeed(float value, String unit) {

		if (value < 0.224) {
			return "0";
		}

		if (unit.equals("kph")) {
			return Utils.formatNumber(value * 3.6, 1, 1);
		}

		if (unit.equals("mph")) {
			return Utils.formatNumber(value * 3.6 * KM_TO_MI, 1, 1);
		}

		if (unit.equals("kn")) {
			return Utils.formatNumber(value * 3.6 * KMH_TO_KNOTS, 1);
		}

		return "";

	}

	public static String getLocalizedSpeedUnit(Context context, String unit) {

		if (unit.equals("kph")) {
			return context.getString(R.string.kph);
		}

		if (unit.equals("mph")) {
			return context.getString(R.string.mph);
		}

		if (unit.equals("kn")) {
			return context.getString(R.string.kn);
		}

		return "";

	}

	/**
	 * @param value
	 *            Speed value is in meters per second
	 * @param unit
	 *            kph or mph
	 * @return
	 */
	public static String formatPace(float value, String unit) {

		if (value < 0.224) {
			return "00:00";
		}

		if (unit.equals("kph")) {
			return formatInterval((long) (1000000 / value), false);
		}

		if (unit.equals("mph")) {
			return formatInterval((long) (1000000 / (value * KMH_TO_MPH)), false);
		}

		if (unit.equals("kn")) {
			return formatInterval((long) (1000000 / (value * KMH_TO_KNOTS)), false);
		}

		return "";

	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////////
	// FORMAT COORDINATES
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static String formatLat(double lat, int outputType) {

		String direction = "N";
		if (lat < 0) {
			direction = "S";
			lat = -lat;
		}

		return formatCoord(lat, outputType) + direction;

	}

	public static String formatLat(double lat) {
		return formatLat(lat, Location.FORMAT_DEGREES);
	}

	public static String formatLng(double lng, int outputType) {

		String direction = "E";
		if (lng < 0) {
			direction = "W";
			lng = -lng;
		}

		return formatCoord(lng, outputType) + direction;

	}

	public static String formatLng(double lng) {
		return formatLng(lng, Location.FORMAT_DEGREES);
	}

	/**
	 * Formats coordinate value to string based on output type (modified version from Android API)
	 */
	public static String formatCoord(double coordinate, int outputType) {

		StringBuilder sb = new StringBuilder();
		char endChar = DEGREE_CHAR;

		DecimalFormat df = new DecimalFormat("###.######");
		if (outputType == Location.FORMAT_MINUTES || outputType == Location.FORMAT_SECONDS) {

			df = new DecimalFormat("##.###");

			int degrees = (int) Math.floor(coordinate);
			sb.append(degrees);
			sb.append(DEGREE_CHAR); // degrees sign
			endChar = '\''; // minutes sign
			coordinate -= degrees;
			coordinate *= 60.0;

			if (outputType == Location.FORMAT_SECONDS) {

				df = new DecimalFormat("##.##");

				int minutes = (int) Math.floor(coordinate);
				sb.append(minutes);
				sb.append('\''); // minutes sign
				endChar = '\"'; // seconds sign
				coordinate -= minutes;
				coordinate *= 60.0;
			}
		}

		sb.append(df.format(coordinate));
		sb.append(endChar);

		return sb.toString();
	}

	/**
	 * Simple coordinate decimal formatter
	 * 
	 * @param coord
	 * @return
	 */
	public static String formatCoord(double coord) {
		DecimalFormat df = new DecimalFormat("###.######");
		df.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		return df.format(coord);
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////////
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static String shortenStr(String s, int maxLength) {

		if (s.length() > maxLength) {
			return s.substring(0, maxLength) + "...";
		}

		return s;
	}

	public static String getDirectionCode(float azimuth) {
		String directionCodes[] = { "N", "NE", "E", "SE", "S", "SW", "W", "NW", "N" };
		return directionCodes[Math.round(azimuth / 45)];
	}

	/**
	 * Get md5 hash
	 * 
	 * @param s
	 *            String to be md5-ed
	 * @return md5 hash
	 */
	public static String md5(String s) {
		try {
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
			digest.update(s.getBytes());
			byte messageDigest[] = digest.digest();

			// Create Hex String
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < messageDigest.length; i++) {

				// hexString.append(Integer.toHexString(0xFF &
				// messageDigest[i]));
				String h = Integer.toHexString(0xFF & messageDigest[i]);
				while (h.length() < 2)
					h = "0" + h;
				hexString.append(h);
			}
			return hexString.toString();

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return "";
	}

	public static String formatInterval(long milliseconds, boolean showHours) {

		int seconds = Math.round(milliseconds / 1000.0f);

		int hours = (int) (seconds / 3600);
		int minutes = (int) (seconds / 60);
		if (minutes >= 60) {
			minutes = (int) (minutes % 60);
		}
		seconds = (int) (seconds % 60);

		StringBuilder builder = new StringBuilder();

		if (hours > 0 || showHours) {
			builder.append(hours);
			builder.append(":");
		}

		if (minutes <= 9) {
			builder.append("0");
		}
		builder.append(minutes);

		builder.append(":");

		if (seconds <= 9) {
			builder.append("0");
		}
		builder.append(seconds);

		return builder.toString();

	}

	/**
	 * Get current magnetic declination
	 * 
	 * @param location
	 * @param timestamp
	 * @return
	 */
	public static float getDeclination(Location location, long timestamp) {

		GeomagneticField field = new GeomagneticField((float) location.getLatitude(), (float) location.getLongitude(),
				(float) location.getAltitude(), timestamp);

		return field.getDeclination();
	}

	/**
	 * Converts time (in milliseconds) to human-readable format "<w> days, <x> hours, <y> minutes and (z) seconds"
	 */
	public static String timeToHumanReadableString(long duration) {

		StringBuffer res = new StringBuffer();

		long temp = 0;

		if (duration >= ONE_SECOND) {

			temp = duration / ONE_DAY;

			if (temp > 0) {
				duration -= temp * ONE_DAY;
				res.append(temp).append(" day").append(temp > 1 ? "s" : "").append(duration >= ONE_MINUTE ? ", " : "");
			}

			temp = duration / ONE_HOUR;
			if (temp > 0) {
				duration -= temp * ONE_HOUR;
				res.append(temp).append(" hour").append(temp > 1 ? "s" : "").append(duration >= ONE_MINUTE ? ", " : "");
			}

			temp = duration / ONE_MINUTE;
			if (temp > 0) {
				duration -= temp * ONE_MINUTE;
				res.append(temp).append(" minute").append(temp > 1 ? "s" : "");
			}

			if (!res.toString().equals("") && duration >= ONE_SECOND) {
				res.append(" and ");
			}

			temp = duration / ONE_SECOND;
			if (temp > 0) {
				res.append(temp).append(" second").append(temp > 1 ? "s" : "");
			}

			return res.toString();

		} else {

			return "0 second";
		}
	}

	/**
	 * Converts time (in milliseconds) to human-readable format (localized)
	 * "<w> days, <x> hours, <y> minutes and (z) seconds"
	 */
	public static String timeToHumanReadableString(Context context, long duration) {

		StringBuffer res = new StringBuffer();

		long temp = 0;

		if (duration >= ONE_SECOND) {

			temp = duration / ONE_DAY;

			if (temp > 0) {
				duration -= temp * ONE_DAY;
				res.append(temp).append(" ").append(context.getString(R.string.days))
						.append(duration >= ONE_MINUTE ? ", " : "");
			}

			temp = duration / ONE_HOUR;
			if (temp > 0) {
				duration -= temp * ONE_HOUR;
				res.append(temp).append(" ").append(context.getString(R.string.hours))
						.append(duration >= ONE_MINUTE ? ", " : "");
			}

			temp = duration / ONE_MINUTE;
			if (temp > 0) {
				duration -= temp * ONE_MINUTE;
				res.append(temp).append(" ").append(context.getString(R.string.minutes));
			}

			if (!res.toString().equals("") && duration >= ONE_SECOND) {
				res.append(" ").append(context.getString(R.string.and)).append(" ");
			}

			temp = duration / ONE_SECOND;
			if (temp > 0) {
				res.append(temp).append(" ").append(context.getString(R.string.seconds));
			}

			return res.toString();

		} else {

			return "0 " + context.getString(R.string.seconds);
		}
	}

	public static int roundToNearest(int number) {

		int place = Integer.toString(number).length() - 2;

		int i = 1;
		if (place <= 0) {
			return number;
		}

		while (place > 0) {
			i = i * 10;
			place--;
		}

		int r = number % i;

		if (r < (i / 2)) {
			return number - r;
		} else {
			return number - r + i;
		}

	}

	public static int roundToNearestFloor(int number) {

		int place = Integer.toString(number).length() - 2;

		int i = 1;
		if (place <= 0) {
			return number;
		}

		while (place > 0) {
			i = i * 10;
			place--;
		}

		int r = number % i;

		return number - r;

	}

	/**
	 * Returns device rotation as integer number from 0 to 270
	 * 
	 * @param activity
	 * @return
	 */
	public static int getDeviceRotation(Activity activity) {
		
		int[] rotations = {0,90,180,270}; 

		final int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();

		if (rotation>0 && rotation<=3) {
			return rotations[rotation];
		} else {
			return 0;
		}

	}

}
