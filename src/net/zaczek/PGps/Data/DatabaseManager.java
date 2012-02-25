package net.zaczek.PGps.Data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.location.Location;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;

public class DatabaseManager {
	private final static String TAG = "PGps";

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
	// V2
	public final static String COL_TRIPS_LAST_END_LOC_LAT = "lastendloclat";
	public final static String COL_TRIPS_LAST_END_LOC_LON = "lastendloclon";
	public final static String COL_TRIPS_LAST_END_ADR = "lastendadr";
	public final static String COL_TRIPS_DISTANCE_FROM_LAST = "distancefromlast";
	public final static String COL_TRIPS_START_POI = "startpoi";
	public final static String COL_TRIPS_END_POI = "endpoi";
	public final static String COL_TRIPS_LAST_END_POI = "lastendpoi";

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
	// V2
	public final static int COL_IDX_TRIPS_LAST_END_LOC_LAT = 11;
	public final static int COL_IDX_TRIPS_LAST_END_LOC_LON = 12;
	public final static int COL_IDX_TRIPS_LAST_END_ADR = 13;
	public final static int COL_IDX_TRIPS_DISTANCE_FROM_LAST = 14;
	public final static int COL_IDX_TRIPS_START_POI = 15;
	public final static int COL_IDX_TRIPS_END_POI = 16;
	public final static int COL_IDX_TRIPS_LAST_END_POI = 17;

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
			COL_TRIPS_LAST_END_LOC_LAT, // 11
			COL_TRIPS_LAST_END_LOC_LON, // 12
			COL_TRIPS_LAST_END_ADR, // 13
			COL_TRIPS_DISTANCE_FROM_LAST, // 14
			COL_TRIPS_START_POI, // 15
			COL_TRIPS_END_POI, // 16
			COL_TRIPS_LAST_END_POI, // 17
	};

	private DatabaseHelper dbHelper;
	private SQLiteDatabase db;

	private DatabaseManager(Context ctx) {
		dbHelper = new DatabaseHelper(ctx);
		db = dbHelper.getWritableDatabase();
	}

	private static DatabaseManager _single;

	public static synchronized DatabaseManager getInstance(Context ctx) {
		if (_single == null) {
			_single = new DatabaseManager(ctx);
		}
		return _single;
	}

	public static synchronized void terminate() {
		if (_single != null)
			_single.db.close();
	}

	private int update(String table, ContentValues values, String whereClause,
			String[] whereArgs) {
		int result = db.update(table, values, whereClause, whereArgs);
		Log.d(TAG, "Update " + table + "; WHERE: " + whereClause
				+ "; result = " + result);
		return result;
	}

	private int delete(String table, String whereClause, String[] whereArgs) {
		int result = db.delete(table, whereClause, whereArgs);
		Log.d(TAG, "Delete from " + table + "; WHERE: " + whereClause
				+ "; result = " + result);
		return result;
	}

	public long newTripEntry(Time start, Location loc, int merge_trips) {
		stopAllTrips();
		deleteShortTrips();

		Log.d(TAG, "getting last trip.");
		long last_end = 0;
		String last_end_lat = null;
		String last_end_lon = null;
		String last_end_adr = null;
		String last_end_poi = null;
		Location last_end_loc = null;
		final Cursor cur = getCursor(db, TRIPS_TABLE_NAME, DEFAULT_PROJECTION, null,
				null, null, null, COL_TRIPS_END + " DESC");		
		try {
			if (cur.moveToFirst()) {
				if (!cur.isNull(COL_IDX_TRIPS_END))
					last_end = cur.getLong(COL_IDX_TRIPS_END);
				last_end_lat = cur.getString(COL_IDX_TRIPS_END_LOC_LAT);
				last_end_lon = cur.getString(COL_IDX_TRIPS_END_LOC_LON);
				last_end_adr = cur.getString(COL_IDX_TRIPS_END_ADR);
				last_end_poi = cur.getString(COL_IDX_TRIPS_END_POI);
				if(!TextUtils.isEmpty(last_end_lat) && !TextUtils.isEmpty(last_end_lon)) {
					final double lat = Location.convert(last_end_lat.replace(',', '.'));
					final double lon = Location.convert(last_end_lon.replace(',', '.'));
					last_end_loc = new Location("");
					last_end_loc.setLatitude(lat);
					last_end_loc.setLongitude(lon);
				}
			}
		} finally {
			cur.close();
		}

		if (merge_trips > 0) {
			Log.d(TAG, "Checking for trips to merge");
			if (last_end > 0) {
				final long timeDiff = (start.toMillis(true) - last_end) / 1000 / 60;
				Log.d(TAG, "merge: timediff = " + timeDiff);
				if (timeDiff < merge_trips) {
					long rowId = cur.getLong(DatabaseHelper.COL_IDX_ID);
					ContentValues vals = new ContentValues();
					vals.put(COL_TRIPS_IS_RECORDING, 1);
					update(TRIPS_TABLE_NAME, vals, DatabaseHelper.COL_ID
							+ " = " + rowId, null);
					Log.i(TAG, "Merge trip, id = " + rowId);
					return rowId;
				}
			}
		}

		ContentValues vals = new ContentValues();
		vals.put(COL_TRIPS_START, start.toMillis(true));
		vals.put(COL_TRIPS_START_LOC_LAT,
				Location.convert(loc.getLatitude(), Location.FORMAT_DEGREES));
		vals.put(COL_TRIPS_START_LOC_LON,
				Location.convert(loc.getLongitude(), Location.FORMAT_DEGREES));
		if(last_end > 0 && last_end_loc != null) {
			vals.put(COL_TRIPS_LAST_END_LOC_LAT, last_end_lat);
			vals.put(COL_TRIPS_LAST_END_LOC_LON, last_end_lon);
			vals.put(COL_TRIPS_LAST_END_ADR, last_end_adr);
			vals.put(COL_TRIPS_LAST_END_POI, last_end_poi);
			vals.put(COL_TRIPS_DISTANCE_FROM_LAST, loc.distanceTo(last_end_loc));
		}
		vals.put(COL_TRIPS_IS_RECORDING, 1);
		final long rowId = db.insertOrThrow(TRIPS_TABLE_NAME, null, vals);
		Log.i(TAG, "Starting new trip, id = " + rowId);
		return rowId;
	}

	private void stopAllTrips() {
		ContentValues vals = new ContentValues();
		vals.put(COL_TRIPS_IS_RECORDING, 0);
		update(TRIPS_TABLE_NAME, vals, COL_TRIPS_IS_RECORDING + "=1", null);
	}

	public void updateTripEntry(long log_trip_id, Time end, Location loc,
			float distance, boolean endLogging) {
		ContentValues vals = new ContentValues();
		vals.put(COL_TRIPS_END, end.toMillis(true));
		vals.put(COL_TRIPS_END_LOC_LAT,
				Location.convert(loc.getLatitude(), Location.FORMAT_DEGREES));
		vals.put(COL_TRIPS_END_LOC_LON,
				Location.convert(loc.getLongitude(), Location.FORMAT_DEGREES));
		vals.put(COL_TRIPS_DISTANCE, distance);
		if (endLogging == true)
			vals.put(COL_TRIPS_IS_RECORDING, 0);
		update(TRIPS_TABLE_NAME, vals, DatabaseHelper.COL_ID + " = "
				+ log_trip_id, null);
	}

	public void updateTripAddress(long log_trip_id, String address,
			String adrCol) {
		ContentValues vals = new ContentValues();
			vals.put(adrCol, address);
		update(TRIPS_TABLE_NAME, vals, DatabaseHelper.COL_ID + " = "
				+ log_trip_id, null);
	}

	public Cursor getExportableTrips() {
		return getCursor(db, TRIPS_TABLE_NAME, DEFAULT_PROJECTION,
				COL_TRIPS_IS_RECORDING + "=0", null, null, null,
				COL_TRIPS_START + " ASC");
	}

	public Cursor getAllTrips() {
		return getCursor(db, TRIPS_TABLE_NAME, DEFAULT_PROJECTION, null, null,
				null, null, COL_TRIPS_START + " DESC");
	}

	public Cursor getTripsToGeocode() {
		return getCursor(db, TRIPS_TABLE_NAME, DEFAULT_PROJECTION,
				COL_TRIPS_START_ADR + " is null OR " + COL_TRIPS_END_ADR
						+ " IS NULL", null, null, null, null);
	}

	public void deleteExportedTrips() {
		delete(TRIPS_TABLE_NAME, COL_TRIPS_IS_RECORDING + "=0", null);
	}

	public void deleteShortTrips() {
		delete(TRIPS_TABLE_NAME, COL_TRIPS_DISTANCE + "<1", null);
		delete(TRIPS_TABLE_NAME, COL_TRIPS_DISTANCE + " IS NULL", null);
		delete(TRIPS_TABLE_NAME, COL_TRIPS_END + " IS NULL", null);
	}

	private Cursor getCursor(SQLiteDatabase db, String table,
			String[] projectionIn, String selection, String[] selectionArgs,
			String groupBy, String having, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(table);
		return qb.query(db, projectionIn, selection, selectionArgs, groupBy,
				having, sortOrder);
	}
}
