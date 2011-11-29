package net.zaczek.PGps.BroadcastReceiver;

import net.zaczek.PGps.GpsService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class WakeUp extends BroadcastReceiver {
	private final static String TAG = "PGps";

	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
		Log.i(TAG, "receiving broadcast intent: " + action);
		if ("com.parrot.asteroid.WakeUp".equals(action)) {
			Log.e(TAG, "Received WakeUp " + intent.toString());
			GpsService.start(context);
		} else {
			Log.e(TAG, "Received unexpected intent " + intent.toString());
		}
	}
}
