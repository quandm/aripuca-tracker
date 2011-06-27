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

	
	protected void prepareCursors() {
		
		super.prepareCursors();

		// track cursor
		String sql = "SELECT * FROM segments WHERE track_id=" + trackId;
		segCursor = myApp.getDatabase().rawQuery(sql, null);
		segCursor.moveToFirst();

	}

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
		pw.println("<distance>" + tCursor.getFloat(tCursor.getColumnIndex("distance")) + "</distance>");
		pw.println("<total_time>" + totalTime + "</total_time>");
		pw.println("<moving_time>" + movingTime + "</moving_time>");
		pw.println("<max_speed>" + tCursor.getFloat(tCursor.getColumnIndex("max_speed")) + "</max_speed>");
		pw.println("<max_elevation>" + tCursor.getFloat(tCursor.getColumnIndex("max_elevation")) + "</max_elevation>");
		pw.println("<min_elevation>" + tCursor.getFloat(tCursor.getColumnIndex("min_elevation")) + "</min_elevation>");
		pw.println("<elevation_gain>" + tCursor.getFloat(tCursor.getColumnIndex("elevation_gain"))
				+ "</elevation_gain>");
		pw.println("<elevation_loss>" + tCursor.getFloat(tCursor.getColumnIndex("elevation_loss"))
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
			pw.println("<distance>" + segCursor.getFloat(segCursor.getColumnIndex("distance")) + "</distance>");
			pw.println("<total_time>" + totalTime + "</total_time>");
			pw.println("<moving_time>" + movingTime + "</moving_time>");
			pw.println("<max_speed>" + segCursor.getFloat(segCursor.getColumnIndex("max_speed")) + "</max_speed>");
			pw.println("<max_elevation>" + segCursor.getFloat(segCursor.getColumnIndex("max_elevation")) + "</max_elevation>");
			pw.println("<min_elevation>" + segCursor.getFloat(segCursor.getColumnIndex("min_elevation")) + "</min_elevation>");
			pw.println("<elevation_gain>" + segCursor.getFloat(segCursor.getColumnIndex("elevation_gain"))
					+ "</elevation_gain>");
			pw.println("<elevation_loss>" + segCursor.getFloat(segCursor.getColumnIndex("elevation_loss"))
					+ "</elevation_loss>");
			pw.println("<start_time>" + startTime + "</start_time>");
			pw.println("<finish_time>" + finishTime + "</finish_time>");

			pw.println("</segment>");
			
			segCursor.moveToNext();
			
		}
		
	
	}

	protected void writeTrackPoint() {

		if (!segmentOpen) {
			prevSegmentId = tpCursor.getInt(tpCursor.getColumnIndex("segment_id"));
			segmentOpen = true;
		} 
		
		if (prevSegmentId != tpCursor.getInt(tpCursor.getColumnIndex("segment_id"))) {
			pw.println("</trkseg>");
			pw.println("<trkseg>");
			segmentOpen = false;
		}
		
		String timeStr = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(tpCursor.getLong(tpCursor
					.getColumnIndex("time")));

		pw.println("<trkpt lat=\"" + tpCursor.getFloat(tpCursor.getColumnIndex("lat")) + "\" lon=\""
					+ tpCursor.getFloat(tpCursor.getColumnIndex("lng")) + "\">");
		pw.println("<ele>" + tpCursor.getFloat(tpCursor.getColumnIndex("elevation")) + "</ele>");
		pw.println("<time>" + timeStr + "</time>");

		pw.println("<extensions>");
		pw.println("<speed>" + tpCursor.getFloat(tpCursor.getColumnIndex("speed")) + "</speed>");
		pw.println("<distance>" + tpCursor.getFloat(tpCursor.getColumnIndex("distance")) + "</distance>");
		pw.println("<accuracy>" + tpCursor.getFloat(tpCursor.getColumnIndex("accuracy")) + "</accuracy>");
		pw.println("<segment_id>" + tpCursor.getInt(tpCursor.getColumnIndex("segment_id")) + "</segment_id>");
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
	
	protected void closeWriter() {
		
		super.closeWriter();

		if (segCursor!=null) {
			segCursor.close();
		}
		
	}
	
	

}
