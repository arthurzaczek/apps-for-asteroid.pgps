package net.zaczek.PGps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class GpsServiceManager extends BroadcastReceiver {
	private final static String TAG = "GpsServiceManager";

	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
		if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
			Log.e(TAG, "Received BOOT_COMPLETED " + intent.toString());
			GpsService.start(context);
		} else if ("com.parrot.asteroid.WakeUp".equals(action)) {
			Log.e(TAG, "Received WakeUp " + intent.toString());
			GpsService.start(context);
		} else if ("com.parrot.asteroid.StandBy".equals(action)) {
			Log.e(TAG, "Received StandBy " + intent.toString());
			GpsService.stop(context);
		} else {
			Log.e(TAG, "Received unexpected intent " + intent.toString());
		}
	}
}
