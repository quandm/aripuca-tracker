package com.aripuca.tracker.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import android.location.Location;

public class Utils {

	public static final char degreeChar = (char) 0x00B0;

	protected static final double KM_TO_MI = 0.621371192;
	protected static final double M_TO_FT = 3.2808399;
	protected static final double MI_TO_M = 1609.344;
	protected static final double MI_TO_FEET = 5280.0;
	protected static final double KMH_TO_MPH = 0.621371192;
	protected static final double KMH_TO_KNOTS = 0.539957; // 1 knot = 1 nautical mile (1.852km) per hour

	public static String formatNumber(Object value, int decimals) {
		return Utils.formatNumber(value, decimals, 0);
	}

	public static String formatNumber(Object value, int decimals, int fraction) {

		NumberFormat f = NumberFormat.getInstance();
		f.setMaximumFractionDigits(decimals);
		f.setMinimumFractionDigits(fraction);
		f.setGroupingUsed(false);

		try {
			return f.format(value);
		} catch (IllegalArgumentException e) {
			return "";
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
				return Utils.formatNumber(value / 1000, 0) + " km";
			}

			if (value > 10000) {
				return Utils.formatNumber(value / 1000, 1) + " km";
			}

			if (value > 1000) {
				return Utils.formatNumber(value / 1000, 2) + " km";
			}

			return Utils.formatNumber(value, 0) + " m";

		}

		if (unit.equals("mi")) {

			if (value > MI_TO_M * 100) {
				return Utils.formatNumber(value / MI_TO_M, 0) + " mi";
			}

			if (value > MI_TO_M * 10) {
				return Utils.formatNumber(value / MI_TO_M, 1) + " mi";
			}

			if (value > MI_TO_M) {
				return Utils.formatNumber(value / MI_TO_M, 2) + " mi";
			}

			return Utils.formatNumber(value * M_TO_FT, 0) + " ft";
		}

		return "";

	}

	public static String formatElevation(float value, String unit) {

		if (unit.equals("m")) {
			return Utils.formatNumber(value, 0) + " m";
		}

		if (unit.equals("ft")) {
			return Utils.formatNumber(value * M_TO_FT, 0) + " ft";
		}

		return "";
	}

	/**
	 * Format speed value (kph, mph or knots)
	 */
	public static String formatSpeed(float value, String unit) {

		if (value < 0.224) {
			return "0.0";
		}

		if (unit.equals("kph")) {
			return Utils.formatNumber(value * 3.6, 1) + " kph";
		}

		if (unit.equals("mph")) {
			return Utils.formatNumber(value * 3.6 * KM_TO_MI, 1) + " mph"; // 1000 * M_TO_FT / MI_TO_FEET;
		}

		if (unit.equals("kn")) {
			return Utils.formatNumber(value * 3.6 * KMH_TO_KNOTS, 1) + " kn";
		}

		return "";

	}

	/**
	 * @param value Speed value is in meters per second
	 * @param unit kph or mph
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

	//////////////////////////////////////////////////////////////////////////////////////////////////////////
	// FORMAT COORDINATES
	//////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static String formatLat(double lat, int outputType) {

		String direction = "N";
		if (lat < 0) {
			direction = "S";
			lat = -lat;
		}

		return formatCoord(lat, outputType) + " " + direction;

	}

	public static String formatLng(double lng, int outputType) {

		String direction = "E";
		if (lng < 0) {
			direction = "W";
			lng = -lng;
		}

		return formatCoord(lng, outputType) + " " + direction;

	}

	/**
	 * Formats coordinate value to string based on output type (modified version
	 * from Android API)
	 */
	public static String formatCoord(double coordinate, int outputType) {

		StringBuilder sb = new StringBuilder();
		char endChar = degreeChar;
		
		DecimalFormat df = new DecimalFormat("###.######");
		if (outputType == Location.FORMAT_MINUTES || outputType == Location.FORMAT_SECONDS) {

			df = new DecimalFormat("##.###");
			
			int degrees = (int) Math.floor(coordinate);
			sb.append(degrees);
			sb.append(degreeChar); // degrees sign
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
		return df.format(coord);	
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static String shortenStr(String s, int maxLength) {

		if (s.length() > maxLength) {
			return s.substring(0, maxLength) + "...";
		}

		return s;
	}

	public static String getDirectionCode(float azimuth) {
		String directionCodes[] = { "N", "NE", "E", "SE", "S", "SW", "W", "NW",
				"N" };
		return directionCodes[Math.round(azimuth / 45)];
	}

	/**
	 * Get md5 hash
	 * 
	 * @param s String to be md5-ed
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

}
