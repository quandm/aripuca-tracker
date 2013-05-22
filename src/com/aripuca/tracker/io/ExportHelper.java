package com.aripuca.tracker.io;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Locale;

import android.database.Cursor;

import com.aripuca.tracker.utils.Utils;

public class ExportHelper {

	public static void writeGPXTrackPoint(PrintWriter pw, Cursor cursor) {

		String timeStr = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")).format(cursor.getLong(cursor
				.getColumnIndex("time")));

		String lat = Utils.formatCoord(cursor.getInt(cursor.getColumnIndex("lat")) / 1E6);
		String lng = Utils.formatCoord(cursor.getInt(cursor.getColumnIndex("lng")) / 1E6);

		pw.println("<trkpt lat=\"" + lat + "\" lon=\"" + lng + "\">");
		pw.println("<ele>" + Utils.formatNumberUS(cursor.getFloat(cursor.getColumnIndex("elevation")), 1)
				+ "</ele>");
		pw.println("<time>" + timeStr + "</time>");

		pw.println("<extensions>");
		pw.println("<number>" + cursor.getString(cursor.getColumnIndex("_id")) + "</number>");
		pw.println("<speed>" + Utils.formatNumberUS(cursor.getFloat(cursor.getColumnIndex("speed")), 2)
				+ "</speed>");
		pw.println("<distance>" + cursor.getInt(cursor.getColumnIndex("distance")) + "</distance>");
		pw.println("<accuracy>" + cursor.getInt(cursor.getColumnIndex("accuracy")) + "</accuracy>");
		pw.println("<segment_index>" + cursor.getInt(cursor.getColumnIndex("segment_index")) + "</segment_index>");
		pw.println("</extensions>");

		pw.println("</trkpt>");

	}

	public static void writeKMLTrackPoint(PrintWriter pw, Cursor cursor) {

		String lat = Utils.formatCoord(cursor.getInt(cursor.getColumnIndex("lat")) / 1E6);
		String lng = Utils.formatCoord(cursor.getInt(cursor.getColumnIndex("lng")) / 1E6);

		pw.println(lng + "," + lat + ","
				+ Utils.formatNumberUS(cursor.getFloat(cursor.getColumnIndex("elevation")), 1) + " ");

	}
	
	public static void writeGPXWaypoint(PrintWriter pw, Cursor cursor) {

		String wpTime = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")).format(cursor.getLong(cursor
				.getColumnIndex("time")));

		String lat = Utils.formatCoord(cursor.getInt(cursor.getColumnIndex("lat")) / 1E6);
		String lng = Utils.formatCoord(cursor.getInt(cursor.getColumnIndex("lng")) / 1E6);

		pw.println("<wpt lat=\"" + lat + "\" lon=\"" + lng + "\">");
		pw.println("<ele>" + Utils.formatNumberUS(cursor.getFloat(cursor.getColumnIndex("elevation")), 1)
				+ "</ele>");
		pw.println("<time>" + wpTime + "</time>");
		pw.println("<name><![CDATA[" + cursor.getString(cursor.getColumnIndex("title")) + "]]></name>");
		pw.println("<desc><![CDATA[" + cursor.getString(cursor.getColumnIndex("descr")) + "]]></desc>");
		// pw.println("<type>" + + "</type>");
		pw.println("</wpt>");

	}

	public static void writeGPXSegment(PrintWriter pw, Cursor cursor) {
		
		String startTime = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)).format(cursor
				.getLong(cursor
						.getColumnIndex("start_time")));

		String finishTime = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)).format(cursor
				.getLong(cursor
						.getColumnIndex("finish_time")));

		String totalTime = Utils.formatInterval(cursor.getLong(cursor.getColumnIndex("total_time")), true);
		String movingTime = Utils.formatInterval(cursor.getLong(cursor.getColumnIndex("moving_time")), true);

		pw.println("<segment>");
		pw.println("<number>" + cursor.getString(cursor.getColumnIndex("_id")) + "</number>");
		pw.println("<index>" + cursor.getString(cursor.getColumnIndex("_id")) + "</index>");
		pw.println("<distance>" + cursor.getInt(cursor.getColumnIndex("distance")) + "</distance>");
		pw.println("<total_time>" + totalTime + "</total_time>");
		pw.println("<moving_time>" + movingTime + "</moving_time>");
		pw.println("<max_speed>"
				+ Utils.formatNumberUS(cursor.getFloat(cursor.getColumnIndex("max_speed")), 2)
				+ "</max_speed>");
		pw.println("<max_elevation>" + cursor.getInt(cursor.getColumnIndex("max_elevation"))
				+ "</max_elevation>");
		pw.println("<min_elevation>" + cursor.getInt(cursor.getColumnIndex("min_elevation"))
				+ "</min_elevation>");
		pw.println("<elevation_gain>" + cursor.getInt(cursor.getColumnIndex("elevation_gain"))
				+ "</elevation_gain>");
		pw.println("<elevation_loss>" + cursor.getInt(cursor.getColumnIndex("elevation_loss"))
				+ "</elevation_loss>");
		pw.println("<start_time>" + startTime + "</start_time>");
		pw.println("<finish_time>" + finishTime + "</finish_time>");

		pw.println("</segment>");
		
	}
	
	
}
