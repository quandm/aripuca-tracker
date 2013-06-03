package com.aripuca.tracker.io;

import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;

import android.content.Context;
import android.database.Cursor;

import com.aripuca.tracker.App;
import com.aripuca.tracker.utils.Utils;

public class TrackGpxExportTask extends TrackExportTask {

	/**
	 * Segments table cursor
	 */
	protected Cursor segCursor;

	protected Cursor wpCursor;

	public TrackGpxExportTask(Context context, App app, long trackId) {

		super(context, app, trackId);

		extension = "gpx";

	}

	/**
	 * Creates database cursors
	 */
	@Override
	protected void prepareCursors() {

		// tracks table cursor
		String sql = "SELECT * FROM tracks WHERE _id=" + trackId + ";";
		tCursor = app.getDatabase().rawQuery(sql, null);
		tCursor.moveToFirst();
		
		// track cursor
		sql = "SELECT * FROM segments WHERE track_id=" + trackId;
		segCursor = app.getDatabase().rawQuery(sql, null);
		segCursor.moveToFirst();

		// track points table cursor
		sql = "SELECT * FROM track_points WHERE track_id=" + trackId + ";";
		tpCursor = app.getDatabase().rawQuery(sql, null);
		tpCursor.moveToFirst();

		// waypoints cursor
		sql = "SELECT * FROM waypoints WHERE track_id=" + trackId + ";";
		wpCursor = app.getDatabase().rawQuery(sql, null);
		wpCursor.moveToFirst();

	}

	/**
	 * Writes header of gpx file
	 */
	@Override
	protected void writeHeader() {

		String startTime = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")).format(tCursor.getLong(tCursor
				.getColumnIndex("start_time")));

		String finishTime = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")).format(tCursor.getLong(tCursor
				.getColumnIndex("finish_time")));

		String totalTime = Utils.formatInterval(tCursor.getLong(tCursor.getColumnIndex("total_time")), true);
		String movingTime = Utils.formatInterval(tCursor.getLong(tCursor.getColumnIndex("moving_time")), true);

		// write gpx header
		pw.format("<?xml version=\"1.0\" encoding=\"%s\" standalone=\"yes\"?>\n", Charset.defaultCharset().name());
		pw.println("<?xml-stylesheet type=\"text/xsl\" href=\"details.xsl\"?>");
		pw.println("<gpx");
		pw.println(" version=\"1.1\"");
		pw.println(" creator=\"Aripuca GPS Tracker for Android\"");
		pw.println(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
		pw.println(" xmlns=\"http://www.topografix.com/GPX/1/1\"");
		pw.print(" xmlns:topografix=\"http://www.topografix.com/GPX/Private/TopoGrafix/0/1\"");
		pw.print(" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 ");
		pw.print("http://www.topografix.com/GPX/1/1/gpx.xsd ");
		pw.print("http://www.topografix.com/GPX/Private/TopoGrafix/0/1 ");
		pw.println("http://www.topografix.com/GPX/Private/TopoGrafix/0/1/topografix.xsd\">");

		// track start
		pw.println("<trk>");
		pw.println("<name><![CDATA[" + tCursor.getString(tCursor.getColumnIndex("title")) + "]]></name>");
		pw.println("<desc><![CDATA[" + tCursor.getString(tCursor.getColumnIndex("descr")) + "]]></desc>");
		pw.println("<number>" + tCursor.getString(tCursor.getColumnIndex("_id")) + "</number>");

		// tracker specific info
		pw.println("<extensions>");
		pw.println("<distance>" + Utils.formatNumberUS(tCursor.getFloat(tCursor.getColumnIndex("distance")), 1)
				+ "</distance>");
		pw.println("<total_time>" + totalTime + "</total_time>");
		pw.println("<moving_time>" + movingTime + "</moving_time>");
		pw.println("<max_speed>" + Utils.formatNumberUS(tCursor.getFloat(tCursor.getColumnIndex("max_speed")), 1)
				+ "</max_speed>");
		pw.println("<max_elevation>" + tCursor.getInt(tCursor.getColumnIndex("max_elevation")) + "</max_elevation>");
		pw.println("<min_elevation>" + tCursor.getInt(tCursor.getColumnIndex("min_elevation")) + "</min_elevation>");
		pw.println("<elevation_gain>" + tCursor.getInt(tCursor.getColumnIndex("elevation_gain")) + "</elevation_gain>");
		pw.println("<elevation_loss>" + tCursor.getInt(tCursor.getColumnIndex("elevation_loss")) + "</elevation_loss>");
		pw.println("<start_time>" + startTime + "</start_time>");
		pw.println("<finish_time>" + finishTime + "</finish_time>");

		// write segments info
		pw.println("<segments>");
		this.writeSegments();
		pw.println("</segments>");

		pw.println("</extensions>");

		pw.println("<trkseg>");

	}

	/**
	 * 
	 */
	@Override
	protected boolean writePoints() {
		
		// add waypoints data associated with this track
		// progress is not updated until track points exporting starts
		while (wpCursor.isAfterLast() == false) {
			
			writePoint(pw, wpCursor);
			
			wpCursor.moveToNext();
			
			// safely stopping AsyncTask, removing file
			if (this.isCancelled()) {
				return false;
			}
			
		}
		
		// continue exporting track points

		return super.writePoints();
		
	}

	@Override
	protected void writePoint(PrintWriter pw, Cursor cursor) {
		
		ExportHelper.writeGPXTrackPoint(pw, cursor);
		
	}
	
	/**
	 * Exporting track segment details as an extension to GPX format
	 */
	protected void writeSegments() {

		while (segCursor.isAfterLast() == false) {
			
			ExportHelper.writeGPXSegment(pw, segCursor);

			segCursor.moveToNext();

		}

	}

	/**
	 * 
	 * @param pw
	 */
	@Override
	protected void writeSegmentStart(PrintWriter pw) {
		
		pw.println("</trkseg>");
		pw.println("<trkseg>");
		
	}

	@Override
	protected void writeFooter() {

		pw.println("</trkseg>");

		// end track
		pw.println("</trk>");

		// footer
		pw.println("</gpx>");

	}

	/**
	 * Closes writer and cursors
	 */
	@Override
	protected void closeWriter() {

		super.closeWriter();

		if (tCursor != null) {
			tCursor.close();
		}

		if (segCursor != null) {
			segCursor.close();
		}

		if (tpCursor != null) {
			tpCursor.close();
		}

		if (wpCursor != null) {
			wpCursor.close();
		}
		
	}

}
