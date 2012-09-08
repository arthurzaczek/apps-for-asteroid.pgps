package net.zaczek.PGps.Data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.database.Cursor;
import android.location.Address;
import com.parrot.parrotmaps.geocoding.Geocoder;
import android.location.Location;
import android.os.Environment;
import android.text.TextUtils;
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
	
	public static void postTrips(Context context, DatabaseManager db) throws IOException {
		Log.i(TAG, "Exporting trips");
		// http://code.google.com/p/android/issues/detail?id=2626		
		char decimal = '.';
		char decimalToReplace = ',';
		
		final String formKey = PGpsPreferences.getInstance(context).googleFormKey;
		if(TextUtils.isEmpty(formKey)) {
			return;
		}
		
		if(PGpsPreferences.getInstance(context).use_comma_as_decimal_seperator) {
			decimal = ',';
			decimalToReplace = '.';			
		}
		
		db.deleteShortTrips();
		updateTripsGeoLocations(context, db);
		Cursor c = null;
		try {
			final Time now = new Time();
			now.setToNow();
			c = db.getPostableTrips();			
			final String url = "https://docs.google.com/spreadsheet/formResponse?formkey=" + formKey;
          
			while (c.moveToNext()) {
				final List<BasicNameValuePair> formParams = new ArrayList<BasicNameValuePair>();
				
				final Time start = new Time();
				start.set(c.getLong(DatabaseManager.COL_IDX_TRIPS_START));
				final Time end = new Time();
				end.set(c.getLong(DatabaseManager.COL_IDX_TRIPS_END));

				formParams.add(new BasicNameValuePair("entry.0.single", start.format("%Y-%m-%d")));
				formParams.add(new BasicNameValuePair("entry.1.single", start.format("%H:%M:%S")));
				formParams.add(new BasicNameValuePair("entry.2.single", end.format("%Y-%m-%d")));
				formParams.add(new BasicNameValuePair("entry.3.single", end.format("%H:%M:%S")));				

				formParams.add(new BasicNameValuePair("entry.4.single", getString(c, DatabaseManager.COL_IDX_TRIPS_START_LOC_LAT, "-") + "," + getString(c, DatabaseManager.COL_IDX_TRIPS_START_LOC_LON, "-")));
				formParams.add(new BasicNameValuePair("entry.5.single", getString(c, DatabaseManager.COL_IDX_TRIPS_LAST_END_LOC_LAT, "-") + "," + getString(c, DatabaseManager.COL_IDX_TRIPS_LAST_END_LOC_LON, "-")));
				formParams.add(new BasicNameValuePair("entry.6.single", getString(c, DatabaseManager.COL_IDX_TRIPS_END_LOC_LAT, "-") + "," + getString(c, DatabaseManager.COL_IDX_TRIPS_END_LOC_LON, "-")));

				formParams.add(new BasicNameValuePair("entry.7.single", getString(c, DatabaseManager.COL_IDX_TRIPS_START_ADR, "-").replace("\"", "\"\"")));
				formParams.add(new BasicNameValuePair("entry.8.single", getString(c, DatabaseManager.COL_IDX_TRIPS_LAST_END_ADR, "-").replace("\"", "\"\"")));
				formParams.add(new BasicNameValuePair("entry.9.single", getString(c, DatabaseManager.COL_IDX_TRIPS_END_ADR, "-").replace("\"", "\"\"")));		
				
				formParams.add(new BasicNameValuePair("entry.10.single", getString(c, DatabaseManager.COL_IDX_TRIPS_START_POI, "-").replace("\"", "\"\"")));
				formParams.add(new BasicNameValuePair("entry.11.single", getString(c, DatabaseManager.COL_IDX_TRIPS_LAST_END_POI, "-").replace("\"", "\"\"")));
				formParams.add(new BasicNameValuePair("entry.12.single", getString(c, DatabaseManager.COL_IDX_TRIPS_END_POI, "-").replace("\"", "\"\"")));		

				float dist = getFloat(c, DatabaseManager.COL_IDX_TRIPS_DISTANCE, 0);
				float distFromLast = getFloat(c, DatabaseManager.COL_IDX_TRIPS_DISTANCE_FROM_LAST, 0);
				float totalDist = dist + distFromLast;
				formParams.add(new BasicNameValuePair("entry.13.single", String.format("%.2f", dist / 1000.0f).replace(decimalToReplace, decimal)));
				formParams.add(new BasicNameValuePair("entry.14.single", String.format("%.2f", distFromLast / 1000.0f).replace(decimalToReplace, decimal)));
				formParams.add(new BasicNameValuePair("entry.15.single", String.format("%.2f", totalDist / 1000.0f).replace(decimalToReplace, decimal)));
				
				final HttpClient client = new DefaultHttpClient();
				final HttpPost post = new HttpPost(url);
				
				post.setEntity(new UrlEncodedFormEntity(formParams));
				final HttpResponse httpResponse = client.execute(post);
				if(httpResponse.getStatusLine().getStatusCode() != 200) {
					throw new IOException(httpResponse.getStatusLine().getReasonPhrase());
				} else {
					db.updateTripPosted(c.getLong(DatabaseHelper.COL_IDX_ID));
				}
			}
		} finally {
			if (c != null)
				c.close();			
		}				
	}

	public static OutputStreamWriter beginGPSLog() {
		OutputStreamWriter gpxwriter = null;
		try {
			Time t = new Time();
			t.setToNow();
			String name = t.format2445();
			String fileName = name + ".gpx";
			gpxwriter = openWrite(fileName, false);
			// Write header
			gpxwriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
			// Root Tag
			gpxwriter.write("<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" version=\"1.1\" creator=\"PGPs\">\n");
			// Track & track segment tags
			gpxwriter.write("  <trk>\n");
			gpxwriter.write("  <name>" + name + "</name>\n");
			gpxwriter.write("    <trkseg>\n");
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
				gpxwriter.write("      <trkpt lat=\"" + lat + "\" lon=\"" + lon + "\">\n");
				gpxwriter.write("        <time>" + time.format3339(false) + "</time>\n");
				gpxwriter.write(String.format("        <cmt>Accuracy: %.0f m; Speed: %.2f</cmt>\n", accuracy, speed));
				gpxwriter.write("      </trkpt>\n");
				gpxwriter.flush();
			} catch (IOException e) {
				Log.e("RaceTracing", "Could write to file " + e.getMessage());
			}
		}
	}

	public static void endGPSLog(OutputStreamWriter gpxwriter) {
		if (gpxwriter != null) {
			try {
				gpxwriter.write("    </trkseg>\n");
				gpxwriter.write("  </trk>\n");
				gpxwriter.write("</gpx>");
				gpxwriter.flush();
				gpxwriter.close();
			} catch (IOException e) {
				Log.e(TAG, "Could close file " + e.getMessage());
			}
		}
	}

}