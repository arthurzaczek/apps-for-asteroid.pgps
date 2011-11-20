package net.zaczek.PGps.Data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Environment;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

public class DataManager {
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

	public static OutputStreamWriter openWrite(String name, boolean append)
			throws IOException {
		File root = Environment.getExternalStorageDirectory();
		File dir = new File(root, "PGps");
		dir.mkdir();
		File file = new File(dir, name);
		if (!file.exists()) {
			file.createNewFile();
		}
		OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file, append), "UTF-8");
		if(append == false)
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

	public static void updateTripsGeoLocations(Context context) {
		Geocoder geocoder = new Geocoder(context, Locale.getDefault());
		DatabaseManager db = new DatabaseManager(context);
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
		} catch (Exception ex) {
			Log.e("PGps", "Unable to update geo locations", ex);
		}
	}

	private static void updateAddress(Geocoder geocoder, DatabaseManager db,
			Cursor c, long log_trip_id, boolean updateStart) throws IOException {
		final double lat = Location
				.convert(c
						.getString(updateStart ? DatabaseManager.COL_IDX_TRIPS_START_LOC_LAT : DatabaseManager.COL_IDX_TRIPS_END_LOC_LAT).replace(',', '.'));
		final double lng = Location
				.convert(c
						.getString(updateStart ? DatabaseManager.COL_IDX_TRIPS_START_LOC_LON : DatabaseManager.COL_IDX_TRIPS_END_LOC_LON).replace(',', '.'));
		final List<Address> addresses = geocoder.getFromLocation(lat,
				lng, 1);
		if(addresses != null && addresses.size() > 0) {
			final StringBuilder sb = new StringBuilder();
			final Address adr = addresses.get(0);
			for(int i=0;i<=adr.getMaxAddressLineIndex();i++) {
				sb.append(adr.getAddressLine(i) + ", ");
			}
			if(sb.length() > 2) sb.delete(sb.length() - 2, sb.length());
			db.updateTripAddress(log_trip_id, sb.toString(), updateStart);
		}
	}

	public static void exportTrips(Context context) {
		updateTripsGeoLocations(context);
		DatabaseManager db = new DatabaseManager(context);
		try {
			Cursor c = null;
			OutputStreamWriter w = null;
			try {
				Time now = new Time();
				now.setToNow();
				c = db.getExportableTrips();
				String name = "Trips-" + now.format("%Y%m%d-%H%M%S") + ".csv";
				w = openWrite(name, false);
				w.append("start date;start time;end date;end time;start location;end location;start address;end address;distance\n");
				while (c.moveToNext()) {
					Time start = new Time();
					start.set(c.getLong(DatabaseManager.COL_IDX_TRIPS_START));
					Time end = new Time();
					end.set(c.getLong(DatabaseManager.COL_IDX_TRIPS_END));

					w.append(start.format("%Y-%m-%d") + ";");
					w.append(start.format("%H:%M:%S") + ";");
					w.append(end.format("%Y-%m-%d") + ";");
					w.append(end.format("%H:%M:%S") + ";");

					w.append(c
							.getString(DatabaseManager.COL_IDX_TRIPS_START_LOC_LAT)
							+ ","
							+ c.getString(DatabaseManager.COL_IDX_TRIPS_START_LOC_LON)
							+ ";");
					w.append(c
							.getString(DatabaseManager.COL_IDX_TRIPS_END_LOC_LAT)
							+ ","
							+ c.getString(DatabaseManager.COL_IDX_TRIPS_END_LOC_LON)
							+ ";");

					w.append(c
							.getString(DatabaseManager.COL_IDX_TRIPS_START_ADR)
							+ ";");
					w.append(c.getString(DatabaseManager.COL_IDX_TRIPS_END_ADR)
							+ ";");

					w.append(Float.toString(c
							.getFloat(DatabaseManager.COL_IDX_TRIPS_DISTANCE)));
					w.append("\n");
				}
				w.flush();
				// db.deleteExportedTrips();
				Toast.makeText(context, "Trips exported to SD Card",
						Toast.LENGTH_LONG).show();
			} finally {
				if (c != null)
					c.close();
				if (w != null)
					w.close();
			}
		} catch (Exception ex) {
			Log.e("PGps", "Unable to export Trips", ex);
			Toast.makeText(context, "Unable to export Trips", Toast.LENGTH_LONG)
					.show();
		}
	}
}