//Partially Copyrighted to  :
/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.mangrover.app;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Set;



public class bluetooth_room extends Activity {

    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    // Debugging
    private static final String TAG = "bluetooth_room";
    private static final boolean D = true;

    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // Member fields
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;

    //working button
    Button Back;
    Button scanButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_room);

        //List of Active Button
        scanButton = (Button) findViewById(R.id.button_scan);
        Back = (Button) findViewById(R.id.button_back);
        createListeners();

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        // Find and set up the ListView for paired devices
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            mPairedDevicesArrayAdapter.add(noDevices);
        }

        //Connecting to Device
        if (getConnectionState() == BluetoothSerialService.STATE_NONE) {
            // Launch the DeviceListActivity/bluetooth_room to see devices and do scan
            Intent serverIntent = new Intent(this, bluetooth_room.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
        }
        else
        if (getConnectionState() == BluetoothSerialService.STATE_CONNECTED) {
            mSerialService.stop();
            mSerialService.start();
        }

    }

    public int getConnectionState() {
        return mSerialService.getState();
    }

    private static BluetoothSerialService mSerialService = null;

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        if (D) Log.d(TAG, "doDiscovery()");

        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);

        // Turn on sub-title for new devices
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
    }

    // The on-click listener for all devices in the ListViews
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            String noDevicesPaired = getResources().getText(R.string.none_paired).toString();
            String noDevicesFound = getResources().getText(R.string.none_found).toString();


            // Cancel discovery because it's costly and we're about to connect
            mBtAdapter.cancelDiscovery();

            String info = ((TextView) v).getText().toString();

            if ( (!info.equals(noDevicesPaired))  &&  (!info.equals(noDevicesFound)) ){

                if (info.length() >= 17) {
                    // Get the device MAC address, which is the last 17 chars in the View
                    String address = info.substring(info.length() - 17);

                    // Create the result Intent and include the MAC address
                    Intent intent = new Intent();
                    intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

                    // Set result and finish this Activity
                    setResult(Activity.RESULT_OK, intent);
                }
                else {
                    setResult(Activity.RESULT_CANCELED);
                }

                finish();
            }
        }
    };

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_device);
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    mNewDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };

//-------------Button's Procedure---------------------------------------------------------------------
    //trigger Main Activity
    private void startMainActivity() {
        Intent intent = new Intent(this, MyActivity.class);
        startActivity(intent);
    }

    //Prosedur CreateListener()
    private void createListeners(){
        Back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                startMainActivity();
            }
        });

        // Initialize the button to perform device discovery
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                doDiscovery();
                view.setVisibility(View.GONE);
            }
        });

    }


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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(DEBUG) Log.d(LOG_TAG, "onActivityResult " + resultCode);
        switch (requestCode) {

           case REQUEST_CONNECT_DEVICE:

                // When DeviceListActivity/bluetooth_room returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras()
                            .getString(bluetooth_room.EXTRA_DEVICE_ADDRESS);
                    // Get the BLuetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    mSerialService.connect(device);
                }
                break;

            /*case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode != Activity.RESULT_OK) {
                    Log.d(LOG_TAG, "BT not enabled");

                    finishDialogNoBluetooth();
                }*/
        }
    }

//--------------------------------------------------------------------------------------------------
//Bagian tombol menu dan setting
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.bluetooth_room, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
//--------------------------------------------------------------------------------------------------

}


