package net.zaczek.PGps;

import java.io.IOException;
import net.zaczek.PGps.Data.DataManager;
import net.zaczek.PGps.Data.DatabaseManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class ExportTripsTask extends AsyncTask<Void, Void, Boolean> {
	private final static String TAG = "PGps";

	private Context _context;
	private GpsService _service;
	private ProgressDialog progressDialog;
	private DatabaseManager _db;
	private Callback _listener;
	
	public interface Callback
	{
		void run();
	}

	public ExportTripsTask(Context context, GpsService service, DatabaseManager db) {
		_context = context;
		_service = service;
		_db = db;
		progressDialog = new ProgressDialog(context);
	}
	
	public ExportTripsTask setPostListener(Callback listener) {
		_listener = listener;
		return this;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		if (_service != null)
			_service.stopTrip();
		try {
			DataManager.exportTrips(_context, _db);
			return true;
		} catch (IOException e) {
			Log.e(TAG, "Unable to export Trips", e);
			return false;
		}
	}

	@Override
	protected void onPreExecute() {
		this.progressDialog.setMessage("Exporting trips");
		this.progressDialog.show();
		super.onPreExecute();
	}

	@Override
	protected void onPostExecute(Boolean result) {
		if (progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
		if (result) {
			// Toast.makeText(_context, "Trips exported to SD Card",
			// Toast.LENGTH_LONG).show();
			AlertDialog alertDialog = new AlertDialog.Builder(_context)
					.setTitle("Trips exported to SD Card")
					.setMessage("Clear local Trip Database?")
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {									
									_db.deleteExportedTrips();
									if(_listener != null) _listener.run();								}
							})
					.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
									if(_listener != null) _listener.run();								}
							}).create();
			alertDialog.show();
		} else {
			Toast.makeText(_context, "Unable to export Trips",
					Toast.LENGTH_LONG).show();
		}
		if(_listener != null) _listener.run();
		super.onPostExecute(result);
	}
}