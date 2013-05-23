package com.aripuca.tracker.io;

import java.io.PrintWriter;

import com.aripuca.tracker.App;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Export to KML AsyncTask class
 */
public class TrackKmlExportTask extends TrackExportTask {

	public TrackKmlExportTask(Context context, App app, long trackId) {
		
		super(context, app, trackId);

		extension = "kml";
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
		
		// track points table cursor
		sql = "SELECT * FROM track_points WHERE track_id=" + trackId + ";";
		tpCursor = app.getDatabase().rawQuery(sql, null);
		tpCursor.moveToFirst();
		
	}
	
	@Override
	protected void writeHeader() {

		pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		pw.print("<kml");
		pw.print(" xmlns=\"http://earth.google.com/kml/2.0\"");
		pw.println(" xmlns:atom=\"http://www.w3.org/2005/Atom\">");
		pw.println("<Document>");
		pw.println("<atom:author><atom:name>Aripuca GPS Tracker for Android" + "</atom:name></atom:author>");
		pw.println("<name>" + tCursor.getString(tCursor.getColumnIndex("title")) + "</name>");
		pw.println("<description>" + tCursor.getString(tCursor.getColumnIndex("descr")) + "</description>");

		// track start
		pw.println("<Placemark>");
		pw.println("<name>" + tCursor.getString(tCursor.getColumnIndex("title")) + "</name>");
		pw.println("<description>" + tCursor.getString(tCursor.getColumnIndex("descr")) + "</description>");
		pw.println("<MultiGeometry><LineString><coordinates>");

	}

	protected void writeSegmentStart(PrintWriter pw) {
		
		pw.println("</coordinates></LineString>");
		pw.println("<LineString><coordinates>");
		
	}
	
	protected void writePoint(PrintWriter pw, Cursor cursor) {
		
		ExportHelper.writeKMLTrackPoint(pw, cursor);
		
	}
	

	@Override
	protected void writeFooter() {

		// end track
		pw.println("</coordinates></LineString></MultiGeometry>");
		pw.println("</Placemark>");

		// footer
		pw.println("</Document>");
		pw.println("</kml>");

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

		if (tpCursor != null) {
			tpCursor.close();
		}

	}
	
}
