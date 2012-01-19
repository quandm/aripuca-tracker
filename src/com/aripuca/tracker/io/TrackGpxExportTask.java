package com.aripuca.tracker.io;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;

import android.content.Context;
import android.database.Cursor;

import com.aripuca.tracker.util.Utils;

public class TrackGpxExportTask extends TrackExportTask {

	/**
	 * Segments table cursor
	 */
	protected Cursor segCursor = null;

	public TrackGpxExportTask(Context c) {

		super(c);

		extension = "gpx";

	}

	/**
	 * Creates database cursors
	 */
	protected void prepareCursors() {

		super.prepareCursors();

		// track cursor
		String sql = "SELECT * FROM segments WHERE track_id=" + trackId;
		segCursor = myApp.getDatabase().rawQuery(sql, null);
		segCursor.moveToFirst();

	}

	/**
	 * Writes header of gpx file
	 */
	protected void writeHeader() {

		String startTime = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(tCursor.getLong(tCursor
				.getColumnIndex("start_time")));

		String finishTime = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(tCursor.getLong(tCursor
				.getColumnIndex("finish_time")));

		String totalTime = Utils.formatInterval(tCursor.getLong(tCursor.getColumnIndex("total_time")), true);
		String movingTime = Utils.formatInterval(tCursor.getLong(tCursor.getColumnIndex("moving_time")), true);

		// write gpx header
		pw.format("<?xml version=\"1.0\" encoding=\"%s\" standalone=\"yes\"?>\n", Charset.defaultCharset()
					.name());
		pw.println("<?xml-stylesheet type=\"text/xsl\" href=\"details.xsl\"?>");
		pw.println("<gpx");
		pw.println(" version=\"1.1\"");
		pw.println(" creator=\"AripucaTracker for Android\"");
		pw.println(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
		pw.println(" xmlns=\"http://www.topografix.com/GPX/1/1\"");
		pw.print(" xmlns:topografix=\"http://www.topografix.com/GPX/Private/TopoGrafix/0/1\"");
		pw.print(" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 ");
		pw.print("http://www.topografix.com/GPX/1/1/gpx.xsd ");
		pw.print("http://www.topografix.com/GPX/Private/TopoGrafix/0/1 ");
		pw.println("http://www.topografix.com/GPX/Private/TopoGrafix/0/1/topografix.xsd\">");

		// track start
		pw.println("<trk>");
		pw.println("<name>" + tCursor.getString(tCursor.getColumnIndex("title")) + "</name>");
		pw.println("<desc>" + tCursor.getString(tCursor.getColumnIndex("descr")) + "</desc>");
		pw.println("<number>" + tCursor.getString(tCursor.getColumnIndex("_id")) + "</number>");

		// tracker specific info
		pw.println("<extensions>");
		pw.println("<distance>" + Utils.formatNumberUS(tCursor.getFloat(tCursor.getColumnIndex("distance")),1) + "</distance>");
		pw.println("<total_time>" + totalTime + "</total_time>");
		pw.println("<moving_time>" + movingTime + "</moving_time>");
		pw.println("<max_speed>" + Utils.formatNumberUS(tCursor.getFloat(tCursor.getColumnIndex("max_speed")),1) + "</max_speed>");
		pw.println("<max_elevation>" + tCursor.getInt(tCursor.getColumnIndex("max_elevation")) + "</max_elevation>");
		pw.println("<min_elevation>" + tCursor.getInt(tCursor.getColumnIndex("min_elevation")) + "</min_elevation>");
		pw.println("<elevation_gain>" + tCursor.getInt(tCursor.getColumnIndex("elevation_gain"))
				+ "</elevation_gain>");
		pw.println("<elevation_loss>" + tCursor.getInt(tCursor.getColumnIndex("elevation_loss"))
				+ "</elevation_loss>");
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
	 * Exporting track segment details as an extension to GPX format
	 */
	protected void writeSegments() {

		while (segCursor.isAfterLast() == false) {

			String startTime = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(segCursor.getLong(segCursor
					.getColumnIndex("start_time")));

			String finishTime = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(segCursor.getLong(segCursor
					.getColumnIndex("finish_time")));

			String totalTime = Utils.formatInterval(segCursor.getLong(segCursor.getColumnIndex("total_time")), true);
			String movingTime = Utils.formatInterval(segCursor.getLong(segCursor.getColumnIndex("moving_time")), true);

			pw.println("<segment>");
			pw.println("<number>" + segCursor.getString(segCursor.getColumnIndex("_id")) + "</number>");
			pw.println("<index>" + segCursor.getString(segCursor.getColumnIndex("_id")) + "</index>");
			pw.println("<distance>" + segCursor.getInt(segCursor.getColumnIndex("distance")) + "</distance>");
			pw.println("<total_time>" + totalTime + "</total_time>");
			pw.println("<moving_time>" + movingTime + "</moving_time>");
			pw.println("<max_speed>" + Utils.formatNumberUS(segCursor.getFloat(segCursor.getColumnIndex("max_speed")),2) + "</max_speed>");
			pw.println("<max_elevation>" + segCursor.getInt(segCursor.getColumnIndex("max_elevation"))
					+ "</max_elevation>");
			pw.println("<min_elevation>" + segCursor.getInt(segCursor.getColumnIndex("min_elevation"))
					+ "</min_elevation>");
			pw.println("<elevation_gain>" + segCursor.getInt(segCursor.getColumnIndex("elevation_gain"))
					+ "</elevation_gain>");
			pw.println("<elevation_loss>" + segCursor.getInt(segCursor.getColumnIndex("elevation_loss"))
					+ "</elevation_loss>");
			pw.println("<start_time>" + startTime + "</start_time>");
			pw.println("<finish_time>" + finishTime + "</finish_time>");

			pw.println("</segment>");

			segCursor.moveToNext();

		}

	}

	protected void writeTrackPoint() {

		if (!segmentOpen) {
			prevSegmentIndex = tpCursor.getInt(tpCursor.getColumnIndex("segment_index"));
			segmentOpen = true;
		}

		if (prevSegmentIndex != tpCursor.getInt(tpCursor.getColumnIndex("segment_index"))) {
			pw.println("</trkseg>");
			pw.println("<trkseg>");
			segmentOpen = false;
		}

		String timeStr = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(tpCursor.getLong(tpCursor
					.getColumnIndex("time")));
		
		String lat = Utils.formatCoord(tpCursor.getInt(tpCursor.getColumnIndex("lat"))/1E6);
		String lng = Utils.formatCoord(tpCursor.getInt(tpCursor.getColumnIndex("lng"))/1E6);

		pw.println("<trkpt lat=\"" + lat + "\" lon=\"" + lng + "\">");
		pw.println("<ele>" + Utils.formatNumberUS(tpCursor.getFloat(tpCursor.getColumnIndex("elevation")),1) + "</ele>");
		pw.println("<time>" + timeStr + "</time>");

		pw.println("<extensions>");
		pw.println("<number>" + tpCursor.getString(tpCursor.getColumnIndex("_id")) + "</number>");
		pw.println("<speed>" + Utils.formatNumberUS(tpCursor.getFloat(tpCursor.getColumnIndex("speed")), 2) + "</speed>");
		pw.println("<distance>" + tpCursor.getInt(tpCursor.getColumnIndex("distance")) + "</distance>");
		pw.println("<accuracy>" + tpCursor.getInt(tpCursor.getColumnIndex("accuracy")) + "</accuracy>");
		pw.println("<segment_index>" + tpCursor.getInt(tpCursor.getColumnIndex("segment_index")) + "</segment_index>");
		pw.println("</extensions>");

		pw.println("</trkpt>");

	}

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
	protected void closeWriter() {

		super.closeWriter();

		if (segCursor != null) {
			segCursor.close();
		}

	}

}
