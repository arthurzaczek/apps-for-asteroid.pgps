package net.zaczek.PGps.Data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import android.content.Context;
import android.database.Cursor;
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

	public static FileWriter openWrite(String name, boolean append)
			throws IOException {
		File root = Environment.getExternalStorageDirectory();
		File dir = new File(root, "PGps");
		dir.mkdir();
		File file = new File(dir, name);
		if (!file.exists()) {
			file.createNewFile();
		}
		return new FileWriter(file, append);
	}

	public static String readLine(BufferedReader in) throws IOException {
		String line = in.readLine();
		if (line == null)
			return null;
		line = line.trim();
		return line;
	}

	public static void exportTrips(Context context) {
		DatabaseManager db = new DatabaseManager(context);
		try {
		Cursor c = null;
		FileWriter w = null;
		try {
			Time now = new Time();
			now.setToNow();
			c = db.getExportableTrips();
			String name = "Trips-" + now.format("%Y%m%d-%H%M%S") + ".csv"; 
			w = openWrite(name, false);
			w.append("start date;start time;end date;end time;start location;end location;start address;end address;distance\n");
			while (c.moveToNext())
			{
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

				w.append(c.getString(DatabaseManager.COL_IDX_TRIPS_START_ADR) + ";");
				w.append(c.getString(DatabaseManager.COL_IDX_TRIPS_END_ADR) + ";");

				w.append(Float.toString(c.getFloat(DatabaseManager.COL_IDX_TRIPS_DISTANCE)));
				w.append("\n");
			}
			w.flush();
			//db.deleteExportedTrips();
			Toast.makeText(context, "Trips exported to SD Card", Toast.LENGTH_LONG).show();
		} finally {
			if (c != null)
				c.close();
			if(w != null)
				w.close();
		}
		} catch (Exception ex) {
			Log.e("PGps", "Unable to export Trips", ex);
			Toast.makeText(context, "Unable to export Trips", Toast.LENGTH_LONG).show();
		}
	}
}