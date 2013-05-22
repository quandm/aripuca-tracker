package com.aripuca.tracker.io;

import java.io.PrintWriter;

import android.content.Context;

import com.aripuca.tracker.utils.Utils;

/**
 * Export to KML AsyncTask class
 */
public class TrackKmlExportTask extends TrackExportTask {

	public TrackKmlExportTask(Context c) {
		super(c);

		extension = "kml";
	}

	/**
	 * Creates database cursors
	 */
	@Override
	protected void prepareCursors() {

		super.prepareCursors();

		// track points table cursor
		String sql = "SELECT * FROM track_points WHERE track_id=" + trackId + ";";
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
	
	@Override
	protected void writeFooter() {

		// end track
		pw.println("</coordinates></LineString></MultiGeometry>");
		pw.println("</Placemark>");

		// footer
		pw.println("</Document>");
		pw.println("</kml>");

	}

}
