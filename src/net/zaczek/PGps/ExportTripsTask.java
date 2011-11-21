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
	private Context _context;
	private GpsService _service;
	private ProgressDialog progressDialog;

	public ExportTripsTask(Context context, GpsService service) {
		_context = context;
		_service = service;
		progressDialog = new ProgressDialog(context);
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
					.setPositiveButton(AlertDialog.BUTTON_POSITIVE,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									DatabaseManager db = new DatabaseManager(
											_context);
									try {
									db.deleteExportedTrips();
									} finally {
										db.close();
									}
								}
							})
					.setNegativeButton(AlertDialog.BUTTON_NEGATIVE,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							}).create();
			alertDialog.show();
		} else {
			Toast.makeText(_context, "Unable to export Trips",
					Toast.LENGTH_LONG).show();
		}
		super.onPostExecute(result);
	}
}