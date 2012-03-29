package net.zaczek.PGps.Data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import android.content.Context;
import android.database.Cursor;
import android.location.Address;
import com.parrot.parrotmaps.geocoding.Geocoder;
import android.location.Location;
import android.os.Environment;
import android.text.format.Time;
import android.util.Log;

public class DataManager {
	private final static String TAG = "PGps";

	public static FileReader openRead(String name) throws IOException {
		File root = Environment.getExternalStorageDirectory();
		File dir = new File(root, "PGps");
		dir.mkdir();
		File file = new File(dir, name);
		if (!file.exists()) {
			file.createNewFile();
		}
		return new FileReader(file);
	}

	public static OutputStreamWriter openWrite(String name, boolean append) throws IOException {
		File root = Environment.getExternalStorageDirectory();
		File dir = new File(root, "PGps");
		dir.mkdir();
		File file = new File(dir, name);
		if (!file.exists()) {
			file.createNewFile();
		}
		OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file, append), "UTF-8");
		if (append == false)
			out.write('\ufeff');
		return out;
	}

	public static String readLine(BufferedReader in) throws IOException {
		String line = in.readLine();
		if (line == null)
			return null;
		line = line.trim();
		return line;
	}

	public static synchronized void updateTripsGeoLocations(Context context, DatabaseManager db) {
		Log.i(TAG, "updateTripsGeoLocations");
		if(PGpsPreferences.getInstance(context).trips_geocode == false) {
			Log.i(TAG, "updateTripsGeoLocations - disabled");
			return;
		}
		Geocoder geocoder = new Geocoder(context);
		try {
			Cursor c = null;
			FileWriter w = null;
			try {
				c = db.getTripsToGeocode();
				while (c.moveToNext()) {
					final long log_trip_id = c.getLong(DatabaseHelper.COL_IDX_ID);
					if (c.isNull(DatabaseManager.COL_IDX_TRIPS_START_ADR)) {
						updateAddress(geocoder, db, c, log_trip_id, DatabaseManager.COL_IDX_TRIPS_START_LOC_LAT, DatabaseManager.COL_IDX_TRIPS_START_LOC_LON, DatabaseManager.COL_TRIPS_START_ADR);
					}
					if (c.isNull(DatabaseManager.COL_IDX_TRIPS_LAST_END_ADR)) {
						updateAddress(geocoder, db, c, log_trip_id, DatabaseManager.COL_IDX_TRIPS_LAST_END_LOC_LAT, DatabaseManager.COL_IDX_TRIPS_LAST_END_LOC_LON, DatabaseManager.COL_TRIPS_LAST_END_ADR);
					}
					if (c.isNull(DatabaseManager.COL_IDX_TRIPS_END_ADR)
							&& c.getInt(DatabaseManager.COL_IDX_TRIPS_IS_RECORDING) == 0) {
						updateAddress(geocoder, db, c, log_trip_id, DatabaseManager.COL_IDX_TRIPS_END_LOC_LAT, DatabaseManager.COL_IDX_TRIPS_END_LOC_LON, DatabaseManager.COL_TRIPS_END_ADR);
					}
					
					if (c.isNull(DatabaseManager.COL_IDX_TRIPS_START_POI)) {
						updatePOI(db, c, log_trip_id, DatabaseManager.COL_IDX_TRIPS_START_LOC_LAT, DatabaseManager.COL_IDX_TRIPS_START_LOC_LON, DatabaseManager.COL_TRIPS_START_POI);
					}					
					if (c.isNull(DatabaseManager.COL_IDX_TRIPS_LAST_END_POI)) {
						updatePOI(db, c, log_trip_id, DatabaseManager.COL_IDX_TRIPS_LAST_END_LOC_LAT, DatabaseManager.COL_IDX_TRIPS_LAST_END_LOC_LON, DatabaseManager.COL_TRIPS_LAST_END_POI);
					}
					if (c.isNull(DatabaseManager.COL_IDX_TRIPS_END_POI)
							&& c.getInt(DatabaseManager.COL_IDX_TRIPS_IS_RECORDING) == 0) {
						updatePOI(db, c, log_trip_id, DatabaseManager.COL_IDX_TRIPS_END_LOC_LAT, DatabaseManager.COL_IDX_TRIPS_END_LOC_LON, DatabaseManager.COL_TRIPS_END_POI);
					}
				}
			} finally {
				if (c != null)
					c.close();
				if (w != null)
					w.close();
			}
			Log.i(TAG, "updateTripsGeoLocations finished");
		} catch (Exception ex) {
			Log.e(TAG, "Unable to update geo locations", ex);
		}
	}

	private static void updatePOI(DatabaseManager db, Cursor c,
			long log_trip_id, int idxLat,
			int idxLon, String poiCol) {
		if(c.isNull(idxLat) || c.isNull(idxLon)) return;
		final double lat = Location.convert(c.getString(idxLat).replace(',', '.'));
		final double lng = Location.convert(c.getString(idxLon).replace(',', '.'));
		final Location loc = new Location("");
		loc.setLatitude(lat);
		loc.setLongitude(lng);
		POI p = POI.get(loc);		
		if(p != null) {
			final float dist = p.getLocation().distanceTo(loc);
			if(dist < 1000.0f) {
				db.updateTripPOI(log_trip_id, p.getName(), poiCol);
			}
		}
	}

	private static void updateAddress(Geocoder geocoder, DatabaseManager db, Cursor c, long log_trip_id, int idxLat, int idxLon, String adrCol) throws IOException {
		if(c.isNull(idxLat) || c.isNull(idxLon)) return;
		final double lat = Location.convert(c.getString(idxLat).replace(',', '.'));
		final double lng = Location.convert(c.getString(idxLon).replace(',', '.'));
		final List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
		if (addresses != null && addresses.size() > 0) {
			final StringBuilder sb = new StringBuilder();
			final Address adr = addresses.get(0);
			for (int i = 0; i <= adr.getMaxAddressLineIndex(); i++) {
				sb.append(adr.getAddressLine(i) + ", ");
			}
			if (sb.length() > 2)
				sb.delete(sb.length() - 2, sb.length());
			Log.i(TAG, "Update address: " + sb.toString());
			db.updateTripAddress(log_trip_id, sb.toString(), adrCol);
		}
	}
	
	private static String getString(Cursor c, int idx, String nullValue) {
		if(c.isNull(idx)) {
			return nullValue;
		} else {
			return c.getString(idx);
		}
	}
	
	private static float getFloat(Cursor c, int idx, float nullValue) {
		if(c.isNull(idx)) {
			return nullValue;
		} else {
			return c.getFloat(idx);
		}
	}

	public static void exportTrips(Context context, DatabaseManager db) throws IOException {
		Log.i(TAG, "Exporting trips");
		// http://code.google.com/p/android/issues/detail?id=2626		
		char decimal = '.';
		char decimalToReplace = ',';
		
		if(PGpsPreferences.getInstance(context).use_comma_as_decimal_seperator) {
			decimal = ',';
			decimalToReplace = '.';			
		}
		
		db.deleteShortTrips();
		updateTripsGeoLocations(context, db);
		Cursor c = null;
		OutputStreamWriter w = null;
		try {
			Time now = new Time();
			now.setToNow();
			c = db.getExportableTrips();
			String name = "Trips-" + now.format("%Y%m%d-%H%M%S") + ".csv";
			w = openWrite(name, false);
			w.append("start date;start time;end date;end time;start location;last location;end location;start address;last address;end address;start poi;last poi;end poi;distance (km);dist from last (km);total dist(km)\n");
			while (c.moveToNext()) {
				Time start = new Time();
				start.set(c.getLong(DatabaseManager.COL_IDX_TRIPS_START));
				Time end = new Time();
				end.set(c.getLong(DatabaseManager.COL_IDX_TRIPS_END));

				w.append(start.format("%Y-%m-%d") + ";");
				w.append(start.format("%H:%M:%S") + ";");
				w.append(end.format("%Y-%m-%d") + ";");
				w.append(end.format("%H:%M:%S") + ";");

				w.append(getString(c, DatabaseManager.COL_IDX_TRIPS_START_LOC_LAT, "-") + "," + getString(c, DatabaseManager.COL_IDX_TRIPS_START_LOC_LON, "-") + ";");
				w.append(getString(c, DatabaseManager.COL_IDX_TRIPS_LAST_END_LOC_LAT, "-") + "," + getString(c, DatabaseManager.COL_IDX_TRIPS_LAST_END_LOC_LON, "-") + ";");
				w.append(getString(c, DatabaseManager.COL_IDX_TRIPS_END_LOC_LAT, "-") + "," + getString(c, DatabaseManager.COL_IDX_TRIPS_END_LOC_LON, "-") + ";");

				w.append(getString(c, DatabaseManager.COL_IDX_TRIPS_START_ADR, "-").replace("\"", "\"\"") + ";");
				w.append(getString(c, DatabaseManager.COL_IDX_TRIPS_LAST_END_ADR, "-").replace("\"", "\"\"") + ";");
				w.append(getString(c, DatabaseManager.COL_IDX_TRIPS_END_ADR, "-").replace("\"", "\"\"") + ";");		
				
				w.append(getString(c, DatabaseManager.COL_IDX_TRIPS_START_POI, "-").replace("\"", "\"\"") + ";");
				w.append(getString(c, DatabaseManager.COL_IDX_TRIPS_LAST_END_POI, "-").replace("\"", "\"\"") + ";");
				w.append(getString(c, DatabaseManager.COL_IDX_TRIPS_END_POI, "-").replace("\"", "\"\"") + ";");		

				float dist = getFloat(c, DatabaseManager.COL_IDX_TRIPS_DISTANCE, 0);
				float distFromLast = getFloat(c, DatabaseManager.COL_IDX_TRIPS_DISTANCE_FROM_LAST, 0);
				float totalDist = dist + distFromLast;
				w.append(String.format("%.2f", dist / 1000.0f).replace(decimalToReplace, decimal) + ";");
				w.append(String.format("%.2f", distFromLast / 1000.0f).replace(decimalToReplace, decimal) + ";");
				w.append(String.format("%.2f", totalDist / 1000.0f).replace(decimalToReplace, decimal));
				w.append("\n");
			}
			w.flush();
		} finally {
			if (c != null)
				c.close();
			if (w != null)
				w.close();
		}
	}

	public static OutputStreamWriter beginGPSLog() {
		OutputStreamWriter gpxwriter = null;
		try {
			Time t = new Time();
			t.setToNow();
			String fileName = t.format2445() + ".gpx";
			gpxwriter = openWrite(fileName, false);
			// Write header
			gpxwriter.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" standalone=\"yes\"?>\n<?xml-stylesheet href=\"gpx.xsl\" type=\"text/xsl\"?>\n");
			// Root Tag
			gpxwriter.write("<gpx version=\"1.0\"\n"
					+ "creator=\"RaceTracking\"\n"
					+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.topografix.com/GPX/1/0\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\">\n");
			// Track & track segment tags
			gpxwriter.write("<trk>\n<trkseg>\n");
		} catch (IOException e) {
			if(gpxwriter != null) {
				try {
					gpxwriter.flush();
					gpxwriter.close();
				} catch (IOException e1) {
					// Don't care
					e1.printStackTrace();
				}
				gpxwriter = null;
			}
			Log.e(TAG, "Could not write file " + e.getMessage());
		}
		return gpxwriter; 
	}
	
	public static void writeGPSLog(OutputStreamWriter gpxwriter, String lat, String lon, Time time, float speed, float accuracy) {
		// write to file
		if (gpxwriter != null) {
			try {
				gpxwriter.write("<trkpt lat=\"" + lat + "\" lon=\"" + lon + "\">\n");
				gpxwriter.write("<time>" + time.format3339(false) + "</time>\n");
				gpxwriter.write(String.format("<cmt>Accuracy: %.0f m; Speed: %.2f</cmt>\n", accuracy, speed));
				gpxwriter.write("</trkpt>\n");
				gpxwriter.flush();
			} catch (IOException e) {
				Log.e("RaceTracing", "Could write to file " + e.getMessage());
			}
		}
	}

	public static void endGPSLog(OutputStreamWriter gpxwriter) {
		if (gpxwriter != null) {
			try {
				gpxwriter.write("</trkseg>\n</trk>\n</gpx>");
				gpxwriter.flush();
				gpxwriter.close();
			} catch (IOException e) {
				Log.e(TAG, "Could close file " + e.getMessage());
			}
		}
	}

}