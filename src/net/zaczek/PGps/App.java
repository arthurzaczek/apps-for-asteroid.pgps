package net.zaczek.PGps;

import net.zaczek.PGps.Data.DatabaseManager;
import android.app.Application;
import android.util.Log;

public class App extends Application {
	private final static String TAG = "PGps";

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		Log.i(TAG, "Application created");
	}
	
	@Override
	public void onTerminate() {
		Log.i(TAG, "Application terminated");
		DatabaseManager.terminate();
		super.onTerminate();
	}
}
