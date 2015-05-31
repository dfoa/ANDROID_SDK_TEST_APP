
package com.ad.intromitestapplication;



import java.util.ArrayList;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.support.v4.content.LocalBroadcastManager;
import com.ad.intromi.*;


public class MainActivity extends ListActivity {

    /**request the user to enable bluetooth**/
	private final int REQUEST_ENABLE_BT_DISCOVERY = 1;
	/**flag to indicate GUI  if scanning or not**/
	public boolean mScanning;
	/**preparing the log to be more clear**/
	protected static final String TAG = "MainActivity";
	protected static final Boolean D = true;

	public   BluetoothDevice  	 device ; 
     /**initiate IntroMi Framework**/
		
	ServiceManager  m;
	
	boolean mBound = false;
	/** application context**/
	private static Context mContext;
	/**array  to hold names in the list**/
	private ArrayList<String> namesList;
	/**adapter of the list**/
	ArrayAdapter<String> adapter;	
	private Menu  currentMenu;
	/**the name to register that represent this device**/
	String nameToRegister;
	@Override
	protected void onStart() {
		super.onStart();

	}

	@Override
	protected void onStop() {
		super.onStop();

		//To stop the service when application is stopped		
				m.stop();

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		//To stop the service when application is destroyed.		
		m.stop();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		namesList = new ArrayList<String>();
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, namesList);
		setListAdapter(adapter);  //Provided by the ListActivity extension
		mContext = getApplicationContext();
		mScanning = false;

        //register local broadcasts 
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(ServiceManager.ERRORS));
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(ServiceManager.MESSAGE));
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(ServiceManager.BT_DISCOVERY_FINISHED));
		

		m = ServiceManager.getInstance(mContext);
		m.setLog(true);

	}





	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

		@Override

		public void onReceive(Context context, Intent intent) {


			if (ServiceManager.MESSAGE.equals(intent.getAction())){
				Profile p = new Profile();
//				intent.getParcelableExtra("Profile");
				Bundle data = new Bundle();
				data = intent.getExtras();
				p = data.getParcelable("Profile");
				namesList.add(p.getName());
				// adding to the UI have to happen in UI thread
				runOnUiThread(new Runnable() {
					@Override
					public void run() {

						adapter.notifyDataSetChanged();
					}
				});


			}
			else 
				if (ServiceManager.BT_DISCOVERY_FINISHED.equals(intent.getAction())){
					
					currentMenu.findItem(R.id.scanning_indicator).setVisible(false); 
					currentMenu.findItem(R.id.scanning_stop).setVisible(false);
					currentMenu.findItem(R.id.scanning_start).setVisible(true);
					m.stop();


					mScanning = false;

				}

				else 
					if(ServiceManager.ERRORS.equals(intent.getAction()))
				{
					switch (intent.getIntExtra("Error", -1)) {
					case Errors.ERR_BT_IS_NOT_DISCOVERABLE:
					{
						Log.v("BT is not discoverable", null);
						break;
					}
					case Errors.ERR_BTV2_IS_NOT_SUPPORTED:
					{
						Log.v("BT is not supported", null);
						finish();
						break;
					}


					}

				}
		}

	};    


	public static Context getContext() {
		return mContext;
	}



	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.scanning, menu);

		if (mScanning) {
			menu.findItem(R.id.scanning_start).setVisible(false);
			menu.findItem(R.id.scanning_stop).setVisible(true);
			menu.findItem(R.id.scanning_indicator)
			.setActionView(R.layout.progress_indicator);

		} else {
			menu.findItem(R.id.scanning_start).setVisible(true);
			menu.findItem(R.id.scanning_stop).setVisible(false);
			menu.findItem(R.id.scanning_indicator).setActionView(null);
		}
		currentMenu = menu;
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.scanning_start:
			/**some tests to make sure BT  is supported and discoverable*/
			/************************************************************/
			if (isBtSupported()){
			   BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();			
			   mBluetoothAdapter.enable();			
			  if (isBtDiscoverable()) {
			     mScanning = true;
			     m.startManualScan();
			  }else 
			  {
				  Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
				    intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
				    startActivityForResult(intent,REQUEST_ENABLE_BT_DISCOVERY); 
			  }
			}
			break;
		case R.id.scanning_stop:
			mScanning = false;
			m.stop();
			mScanning = false;
			break;
		case R.id.register:

			final EditText name = new EditText(this);

			// Set the default text to a link of the Queen


			new AlertDialog.Builder(this)
			.setTitle("Register to service")
			.setMessage("Pls enter your name")
			.setView(name)
			.setPositiveButton("Save", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					nameToRegister = name.getText().toString();
					System.out.println(nameToRegister);
					Register register = Register.getInstance();
					register.doRegistration(getApplicationContext(), "ThisIsmiID",nameToRegister);
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
			})
			.show(); 

			break;
		}

		invalidateOptionsMenu();
		return true;
	}



	
	
private boolean isBtSupported() {
	
	BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
	/*
	 * check if  bluetooth is supported on this device. 
	 */
	
	if (mBtAdapter == null) {
	    // Device does not support Bluetooth
		System.out.println("BT is not supported on this device");
		 return false;
	}
	
	return true;		
}
	

private boolean isBtDiscoverable() {
	
	BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
	if(mBtAdapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
    
     return true;
	
}
	return false;
	
}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode==REQUEST_ENABLE_BT_DISCOVERY && resultCode==Activity.RESULT_OK) {	                 
			 System.out.println("Discovery is now enabled");
		     mScanning = true;
		     m.startManualScan();
			  
	}


	}
}




