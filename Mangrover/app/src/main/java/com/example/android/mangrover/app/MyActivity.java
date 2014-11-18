package com.example.android.mangrover.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.Set;


public class MyActivity extends Activity {

    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private Set<BluetoothDevice> pairedDevices;
    private boolean mEnablingBT;


//    private static TextView mTitle;

    // Name of the connected device
    private String mConnectedDeviceName = null;

    /**
     * Set to true to add debugging code and logging.
     */
    public static final boolean DEBUG = true;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    /**
     * The tag we use when logging, so that our messages can be distinguished
     * from other messages in the log. Public because it's used by several
     * classes.
     */
    public static final String LOG_TAG = "Mangrover";

    // Message types sent from the BluetoothReadService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    private static BluetoothSerialService mSerialService = null;


    //working button
    Button Devices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

            if (!mEnablingBT) { // If we are turning on the BT we cannot check if it's enable
                if ( (mBluetoothAdapter != null)  && (!mBluetoothAdapter.isEnabled()) ) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(R.string.alert_dialog_turn_on_bt)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle(R.string.alert_dialog_warning_title)
                            .setCancelable( false )
                            .setPositiveButton(R.string.alert_dialog_yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    mEnablingBT = true;
                                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                                }
                            })
                            .setNegativeButton(R.string.alert_dialog_no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    finishDialogNoBluetooth();
                                }
                            });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
                else{
                    Toast.makeText(getApplicationContext(),"Bluetooth is ready",
                            Toast.LENGTH_LONG).show();
                }
            }

        Devices = (Button) findViewById(R.id.device_list);
        createListeners();


        /*
        // Set up the window layout
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_my);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            finishDialogNoBluetooth();
            return;
        }
          */
    }

    public void finishDialogNoBluetooth() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.alert_dialog_no_bt)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(R.string.app_name)
                .setCancelable( false )
                .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(DEBUG) Log.d(LOG_TAG, "onActivityResult " + resultCode);
        switch (requestCode) {

           case REQUEST_CONNECT_DEVICE:

                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BluetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    mSerialService.connect(device);
                }
                break;

            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode != Activity.RESULT_OK) {
                    Log.d(LOG_TAG, "BT not enabled");

                    finishDialogNoBluetooth();
                }
        }
    }

    private void DeviceListActivity() {
        Intent intent = new Intent(this, DeviceListActivity.class);
        startActivity(intent);
    }

    private void createListeners() {
        Devices.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                DeviceListActivity();
            }
        });

    }

    public int getConnectionState() {
        return mSerialService.getState();
    }


//-------------------------------------------------------------------------------------------------
// bagian tombol menu dan setting
//    private MenuItem mMenuItemConnect;                    //Never Used
//    private MenuItem mMenuItemStartStopRecording;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
//        mMenuItemConnect = menu.getItem(0);
//        mMenuItemStartStopRecording = menu.getItem(3);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.connect:

//                if (getConnectionState() == BluetoothSerialService.STATE_NONE) {
                    // Launch the DeviceListActivity to see devices and do scan
                    Intent serverIntent = new Intent(this, DeviceListActivity.class);
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
//                } else if (getConnectionState() == BluetoothSerialService.STATE_CONNECTED) {
//                    mSerialService.stop();
//                    mSerialService.start();
//                }
                return true;
            case R.id.action_settings:
                return true;
        }
        return false;
    }
//--------------------------------------------------------------------------------------------------

}
