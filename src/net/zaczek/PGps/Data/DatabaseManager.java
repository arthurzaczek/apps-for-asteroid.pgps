package net.zaczek.PGps.Data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.location.Location;
import android.text.format.Time;

public class DatabaseManager {
	public final static String TRIPS_TABLE_NAME = "Trips";

	public final static String COL_TRIPS_START = "start";
	public final static String COL_TRIPS_END = "end";
	public final static String COL_TRIPS_START_LOC_LAT = "startloclat";
	public final static String COL_TRIPS_START_LOC_LON = "startloclon";
	public final static String COL_TRIPS_END_LOC_LAT = "endloclat";
	public final static String COL_TRIPS_END_LOC_LON = "endloclon";
	public final static String COL_TRIPS_START_ADR = "startadr";
	public final static String COL_TRIPS_END_ADR = "endadr";
	public final static String COL_TRIPS_DISTANCE = "distance";
	public final static String COL_TRIPS_IS_RECORDING = "isrecording";

	public final static int COL_IDX_TRIPS_START = 1;
	public final static int COL_IDX_TRIPS_END = 2;
	public final static int COL_IDX_TRIPS_START_LOC_LAT = 3;
	public final static int COL_IDX_TRIPS_START_LOC_LON = 4;
	public final static int COL_IDX_TRIPS_END_LOC_LAT = 5;
	public final static int COL_IDX_TRIPS_END_LOC_LON = 6;
	public final static int COL_IDX_TRIPS_START_ADR = 7;
	public final static int COL_IDX_TRIPS_END_ADR = 8;
	public final static int COL_IDX_TRIPS_DISTANCE = 9;
	public final static int COL_IDX_TRIPS_IS_RECORDING = 10;

	public static final String[] DEFAULT_PROJECTION = new String[] {
			DatabaseHelper.COL_ID, // 0
			COL_TRIPS_START, // 1
			COL_TRIPS_END, // 2
			COL_TRIPS_START_LOC_LAT, // 3
			COL_TRIPS_START_LOC_LON, // 4
			COL_TRIPS_END_LOC_LAT, // 5
			COL_TRIPS_END_LOC_LON, // 6
			COL_TRIPS_START_ADR, // 7
			COL_TRIPS_END_ADR, // 8
			COL_TRIPS_DISTANCE, // 9
			COL_TRIPS_IS_RECORDING, // 10
	};

	private DatabaseHelper dbHelper;

	public DatabaseManager(Context ctx) {
		dbHelper = new DatabaseHelper(ctx);
	}

	public long newTripEntry(Time start, Location loc) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		try {
			ContentValues vals = new ContentValues();
			vals.put(COL_TRIPS_IS_RECORDING, 0);
			db.update(TRIPS_TABLE_NAME, vals,
					COL_TRIPS_IS_RECORDING + " = 1", null);
			
			vals = new ContentValues();
			vals.put(COL_TRIPS_START, start.toMillis(true));
			vals.put(COL_TRIPS_START_LOC_LAT, Location.convert(loc.getLatitude(), Location.FORMAT_DEGREES));
			vals.put(COL_TRIPS_START_LOC_LON, Location.convert(loc.getLongitude(), Location.FORMAT_DEGREES));
			vals.put(COL_TRIPS_IS_RECORDING, 1);
			long rowId = db.insertOrThrow(TRIPS_TABLE_NAME, null,
					vals);
			return rowId;

		} finally {
			if (db != null)
				db.close();
		}
	}

	public void updateTripEntry(long log_trip_id, Time end, Location loc, float distance,
			boolean endLogging) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		try {
			ContentValues vals = new ContentValues();
			vals.put(COL_TRIPS_END, end.toMillis(true));
			vals.put(COL_TRIPS_END_LOC_LAT, Location.convert(loc.getLatitude(), Location.FORMAT_DEGREES));
			vals.put(COL_TRIPS_END_LOC_LON, Location.convert(loc.getLongitude(), Location.FORMAT_DEGREES));
			vals.put(COL_TRIPS_DISTANCE, distance);
			if(endLogging == true)
				vals.put(COL_TRIPS_IS_RECORDING, 0);
			db.update(TRIPS_TABLE_NAME, vals,
					DatabaseHelper.COL_ID + " = " + log_trip_id, null);	
		} finally {
			if (db != null)
				db.close();
		}
	}

	public Cursor getExportableTrips() {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		return getCursor(db, TRIPS_TABLE_NAME, DEFAULT_PROJECTION, 
				COL_TRIPS_IS_RECORDING + "=0", null, null, null,
				COL_TRIPS_START + " ASC");		
	}
	
	public void deleteExportedTrips() {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		try {
			db.delete(TRIPS_TABLE_NAME, COL_TRIPS_IS_RECORDING + "=0", null);	
		} finally {
			if (db != null)
				db.close();
		}
	}
	
	private Cursor getCursor(SQLiteDatabase db, String table, String[] projectionIn,
			String selection, String[] selectionArgs, String groupBy,
			String having, String sortOrder)
	{
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(table);
		return qb.query(db, projectionIn, selection, selectionArgs, groupBy,
				having, sortOrder);
	}
}
