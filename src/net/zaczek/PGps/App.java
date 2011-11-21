package net.zaczek.PGps;

import net.zaczek.PGps.Data.DatabaseManager;
import android.app.Application;

public class App extends Application {
	
	@Override
	public void onTerminate() {
		DatabaseManager.terminate();
		super.onTerminate();
	}
}
