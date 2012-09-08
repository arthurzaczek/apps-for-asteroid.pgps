package net.zaczek.PGps.Data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper
{
	private final static String		DATABASE_NAME				= "PGps.db";
	private final static int		DATABASE_VERSION			= 3;
	
	public final static String		COL_ID						= "_id";
	public final static int			COL_IDX_ID					= 0;
	
	public static final String[]	ID_PROJECTION				= new String[] { COL_ID };

	/**
	 * @param context
	 */
	DatabaseHelper(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		createDb(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		if (oldVersion != newVersion)
		{
			Log.i("PGps", "Upgrading Database from " + oldVersion + " to " + newVersion);
			if(oldVersion == 1)
			{
				// Alter statements 
				db.execSQL("ALTER TABLE " + DatabaseManager.TRIPS_TABLE_NAME + " ADD " + DatabaseManager.COL_TRIPS_LAST_END_LOC_LAT  + " TEXT");
				db.execSQL("ALTER TABLE " + DatabaseManager.TRIPS_TABLE_NAME + " ADD " + DatabaseManager.COL_TRIPS_LAST_END_LOC_LON  + " TEXT");
				db.execSQL("ALTER TABLE " + DatabaseManager.TRIPS_TABLE_NAME + " ADD " + DatabaseManager.COL_TRIPS_LAST_END_ADR  + " TEXT");
				db.execSQL("ALTER TABLE " + DatabaseManager.TRIPS_TABLE_NAME + " ADD " + DatabaseManager.COL_TRIPS_DISTANCE_FROM_LAST  + " REAL");
				db.execSQL("ALTER TABLE " + DatabaseManager.TRIPS_TABLE_NAME + " ADD " + DatabaseManager.COL_TRIPS_START_POI  + " TEXT");
				db.execSQL("ALTER TABLE " + DatabaseManager.TRIPS_TABLE_NAME + " ADD " + DatabaseManager.COL_TRIPS_END_POI  + " TEXT");
				db.execSQL("ALTER TABLE " + DatabaseManager.TRIPS_TABLE_NAME + " ADD " + DatabaseManager.COL_TRIPS_LAST_END_POI  + " TEXT");
				
				// Next version
				oldVersion = 2;
			}
			if(oldVersion == 2)
			{
				// Alter statements 
				db.execSQL("ALTER TABLE " + DatabaseManager.TRIPS_TABLE_NAME + " ADD " + DatabaseManager.COL_TRIPS_POSTED  + " INTEGER");
				
				// Next version
				oldVersion = 3;				
			}
			if(oldVersion == 3)
			{
				// Alter statements 
				// ...
				
				// Next version
				oldVersion = 4;				
			}
		}
		else
		{
			Log.i("PGps", "No need to upgrade Database");			
		}
	}

	private void createDb(SQLiteDatabase db)
	{
		Log.i("PGps", "creating Database");		
		final String columns = TextUtils.join(", ", new String[] {
				COL_ID + " INTEGER PRIMARY KEY",
				DatabaseManager.COL_TRIPS_START + " INTEGER", 
				DatabaseManager.COL_TRIPS_END + " INTEGER",
				DatabaseManager.COL_TRIPS_START_LOC_LAT + " TEXT",
				DatabaseManager.COL_TRIPS_START_LOC_LON + " TEXT",
				DatabaseManager.COL_TRIPS_END_LOC_LAT + " TEXT",
				DatabaseManager.COL_TRIPS_END_LOC_LON + " TEXT",
				DatabaseManager.COL_TRIPS_START_ADR + " TEXT",
				DatabaseManager.COL_TRIPS_END_ADR + " TEXT",
				DatabaseManager.COL_TRIPS_DISTANCE + " REAL",
				DatabaseManager.COL_TRIPS_IS_RECORDING + " INTEGER",

				DatabaseManager.COL_TRIPS_LAST_END_LOC_LAT + " TEXT",
				DatabaseManager.COL_TRIPS_LAST_END_LOC_LON + " TEXT",
				DatabaseManager.COL_TRIPS_LAST_END_ADR + " TEXT",
				DatabaseManager.COL_TRIPS_DISTANCE_FROM_LAST + " REAL",
				DatabaseManager.COL_TRIPS_START_POI + " TEXT",
				DatabaseManager.COL_TRIPS_END_POI + " TEXT",
				DatabaseManager.COL_TRIPS_LAST_END_POI + " TEXT",
				
				DatabaseManager.COL_TRIPS_POSTED + " INTEGER",
		});

		db.execSQL("CREATE TABLE " + DatabaseManager.TRIPS_TABLE_NAME + " (" + columns
				+ ");");
	}

//	private void dropDb(SQLiteDatabase db)
//	{
//		db.execSQL("DROP TABLE IF EXISTS " + DatabaseManager.TRIPS_TABLE_NAME);
//	}

	public void cleanDb(SQLiteDatabase db)
	{
		db.execSQL("DELETE FROM " + DatabaseManager.TRIPS_TABLE_NAME);		
	}
	
	public void clearTripsTable(SQLiteDatabase db)
	{
		db.execSQL("DELETE FROM " + DatabaseManager.TRIPS_TABLE_NAME);
	}
}