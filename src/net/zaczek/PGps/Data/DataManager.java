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
						updateAddress(geocoder, db, c, log_trip_id, true);
					}
					if (c.isNull(DatabaseManager.COL_IDX_TRIPS_END_ADR) && c.getInt(DatabaseManager.COL_IDX_TRIPS_IS_RECORDING) == 0) {
						updateAddress(geocoder, db, c, log_trip_id, false);
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

	private static void updateAddress(Geocoder geocoder, DatabaseManager db, Cursor c, long log_trip_id, boolean updateStart) throws IOException {
		final double lat = Location.convert(c.getString(updateStart ? DatabaseManager.COL_IDX_TRIPS_START_LOC_LAT : DatabaseManager.COL_IDX_TRIPS_END_LOC_LAT).replace(',', '.'));
		final double lng = Location.convert(c.getString(updateStart ? DatabaseManager.COL_IDX_TRIPS_START_LOC_LON : DatabaseManager.COL_IDX_TRIPS_END_LOC_LON).replace(',', '.'));
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
			db.updateTripAddress(log_trip_id, sb.toString(), updateStart);
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
			w.append("start date;start time;end date;end time;start location;end location;start address;end address;distance (km)\n");
			while (c.moveToNext()) {
				Time start = new Time();
				start.set(c.getLong(DatabaseManager.COL_IDX_TRIPS_START));
				Time end = new Time();
				end.set(c.getLong(DatabaseManager.COL_IDX_TRIPS_END));

				w.append(start.format("%Y-%m-%d") + ";");
				w.append(start.format("%H:%M:%S") + ";");
				w.append(end.format("%Y-%m-%d") + ";");
				w.append(end.format("%H:%M:%S") + ";");

				w.append(c.getString(DatabaseManager.COL_IDX_TRIPS_START_LOC_LAT) + "," + c.getString(DatabaseManager.COL_IDX_TRIPS_START_LOC_LON) + ";");
				w.append(c.getString(DatabaseManager.COL_IDX_TRIPS_END_LOC_LAT) + "," + c.getString(DatabaseManager.COL_IDX_TRIPS_END_LOC_LON) + ";");

				if (c.isNull(DatabaseManager.COL_IDX_TRIPS_START_ADR)) {
					w.append("-;");

				} else {
					w.append(c.getString(DatabaseManager.COL_IDX_TRIPS_START_ADR).replace("\"", "\"\"") + ";");
				}
				if (c.isNull(DatabaseManager.COL_IDX_TRIPS_END_ADR)) {
					w.append("-;");

				} else {
					w.append(c.getString(DatabaseManager.COL_IDX_TRIPS_END_ADR).replace("\"", "\"\"") + ";");
				}

				float dist = c.getFloat(DatabaseManager.COL_IDX_TRIPS_DISTANCE);
				w.append(String.format("%.2f", dist / 1000.0f).replace(decimalToReplace, decimal));
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
}