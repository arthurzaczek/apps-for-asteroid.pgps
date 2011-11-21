package net.zaczek.PGps;

import java.io.IOException;

import net.zaczek.PGps.Data.DataManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class ExportTripsTask extends AsyncTask<Void, Void, Boolean> {
	private Context _context;
	private GpsService _service;
	private ProgressDialog dialog;
	
	public ExportTripsTask(Context context, GpsService service)
	{
		_context = context;
		_service = service;
		dialog = new ProgressDialog(context);
	}
	@Override
	protected Boolean doInBackground(Void... params) {
		if (_service != null)
			_service.stopTrip();
		try {
			DataManager.exportTrips(_context);
			return true;
		} catch (IOException e) {
			Log.e("PGps", "Unable to export Trips", e);
			return false;
		}
	}

	@Override
	protected void onPreExecute() {
		this.dialog.setMessage("Exporting trips");
        this.dialog.show();
		super.onPreExecute();
	}

	@Override
	protected void onPostExecute(Boolean result) {
		if (dialog.isShowing()) {
            dialog.dismiss();
        }
		if (result) {
			Toast.makeText(_context, "Trips exported to SD Card",
					Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(_context, "Unable to export Trips",
					Toast.LENGTH_LONG).show();
		}
		super.onPostExecute(result);
	}
}