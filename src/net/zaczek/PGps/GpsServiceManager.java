package net.zaczek.PGps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class GpsServiceManager extends BroadcastReceiver {
	private final static String TAG = "GpsServiceManager";

	@Override
	public void onReceive(Context context, Intent intent) {
		if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
			Log.e(TAG, "Received BOOT_COMPLETED " + intent.toString());
			GpsService.start(context);
		} else {
			Log.e(TAG, "Received unexpected intent " + intent.toString());
		}
	}
}
