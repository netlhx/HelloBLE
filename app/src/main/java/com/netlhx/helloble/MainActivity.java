package com.netlhx.helloble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION_LOCATION = 111;
    private static final int REQUEST_ENABLE_BT = 222;
    private static final int REQUEST_LOCATION_SERVICE = 333;
    private static final String TAG = MainActivity.class.getSimpleName();
    private boolean mBleEnvReady = true;

    private Button mStartButton;
    private Handler mHandler = new Handler();

    private BleService mBleService;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            //got BleService Instance
            mBleService = ((BleService.LocalBinder) service).getService();
            Log.d(TAG, "onServiceConnected: ");
            checkBleStatus();
            checkBlePermission();
            //checkLocationService();
            if(!mBleService.initialize()) {
                Log.d(TAG, "onServiceConnected: " + "failed");
            }

           
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBleService = null;
            Log.d(TAG, "onServiceDisconnected: ");
        }
    };

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BleService.ACTION_DEVICE_FOUND:
                    break;

                case BleService.ACTION_GATT_CONNECTED:
                    break;

                case BleService.ACTION_GATT_DISCONNECTED:
                    break;

                case BleService.ACTION_GATT_SERVICES_DISCOVERED:
                    break;

                case BleService.ACTION_DATA_AVAILABLE:
                    break;

                default:
                    break;
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStartButton = (Button) findViewById(R.id.start_button);

        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mBleService != null) {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mBleService.stopScan();
                        }
                    }, 3000);

                    mBleService.startScan();
                }
            }
        });

        Intent intent = new Intent(this, BleService.class);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);

    }

    private void checkBlePermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_LOCATION);
            }

        }

    }

    private void checkBleStatus() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if(!bluetoothAdapter.isEnabled()) {
            Intent intent= new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BT);
        }

    }


    private void checkLocationService() {

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if(locationManager.getProviders(true).size() > 0) {
            mBleEnvReady = true;

        } else {
            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.enable_location_service_title)
                    .setMessage(R.string.enable_location_service_message)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivityForResult(intent, REQUEST_LOCATION_SERVICE);
                        }
                    })
                    .create();
            alertDialog.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_ENABLE_BT) {
            if(resultCode == RESULT_OK) {
                mBleEnvReady = true;

            } else {
                Toast.makeText(this, R.string.enable_bt, Toast.LENGTH_SHORT).show();
            }
        }

        if(requestCode == REQUEST_LOCATION_SERVICE) {

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mBroadcastReceiver, makeGattUpdateIntentFilter());

        if(mBleService != null) {
            mBleService.connect(null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_PERMISSION_LOCATION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, R.string.enable_bt_permission, Toast.LENGTH_SHORT).show();
                }
            } else {
                mBleEnvReady = true;
            }
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBleService = null;
        super.onPause();

    }
}
