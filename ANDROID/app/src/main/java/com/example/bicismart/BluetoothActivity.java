package com.example.bicismart;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import android.app.Activity;

import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class BluetoothActivity extends Activity
{
    private TextView tvStatus;
    private Button btnActivate;
    private Button btnPair;
    private Button btnSearch;

    private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<>();

    private BluetoothAdapter mBluetoothAdapter;
    BluetoothManager bluetoothManager;

    public static final int MULTIPLE_PERMISSIONS = 10;

    @SuppressLint("InlinedApi")
    String[] permissions= new String[]
            {
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
            };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bt_activity_bluetooth);

        tvStatus = findViewById(R.id.txtEstado);
        btnActivate = findViewById(R.id.btnActivar);
        btnPair = findViewById(R.id.btnEmparejar);
        btnSearch = findViewById(R.id.btnBuscar);

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (Build.VERSION.SDK_INT >= 31)
        {
            mBluetoothAdapter = bluetoothManager.getAdapter();
            checkPermissions_AFTER_SDK_31();
        } else
        {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            checkBTPermissions_PRE_SDK_31();
        }

        enableBluetoothComponent();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onPause()
    {
        super.onPause();
        if (mBluetoothAdapter != null)
        {
            if (mBluetoothAdapter.isDiscovering())
            {
                mBluetoothAdapter.cancelDiscovery();
            }
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(bluetoothBroadcastReceiver);
    }

    //Chequea permisos actuales y agrega los faltantes
    private void checkPermissions_AFTER_SDK_31()
    {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();

        for (String permission:permissions)
        {
            result = ContextCompat.checkSelfPermission(this,permission);
            if (result != PackageManager.PERMISSION_GRANTED)
            {
                listPermissionsNeeded.add(permission);
            }
        }
        if (!listPermissionsNeeded.isEmpty())
        {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]),MULTIPLE_PERMISSIONS );
        }
    }

    //Chequea permisos actuales y agrega los faltantes
    private void checkBTPermissions_PRE_SDK_31()
    {
        int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
        permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
        if (permissionCheck != 0)
        {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
        }
    }

    protected  void enableBluetoothComponent()
    {
        if (mBluetoothAdapter == null)
        {
            showUnsupported();
            return;
        }
        else
        {
            if (mBluetoothAdapter.isEnabled())
            {
                showEnabled();
            }
            else
            {
                showDisabled();
            }
        }

        //Suscripcion a eventos de Bluetooth
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetoothBroadcastReceiver, filter);
    }

    private final BroadcastReceiver bluetoothBroadcastReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();

                switch (Objects.requireNonNull(action)) {
                    case BluetoothAdapter.ACTION_STATE_CHANGED:
                        handleBluetoothStateChanged(intent);
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                        mDeviceList = new ArrayList<>();
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        launchDeviceListActivity();
                        break;
                    case BluetoothDevice.ACTION_FOUND:
                        handleDeviceFound(intent);
                        break;
                }
            } catch (NullPointerException e) {
                throw new RuntimeException(e);
            }
        }

        private void handleBluetoothStateChanged(Intent intent) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            if (state == BluetoothAdapter.STATE_ON) {
                showToast("Bluetooth Enabled");
                showEnabled();
            }
        }

        private void launchDeviceListActivity() {
            Intent newIntent = new Intent(BluetoothActivity.this, DeviceListActivity.class);
            newIntent.putParcelableArrayListExtra("device.list", mDeviceList);
            startActivity(newIntent);
        }

        @SuppressLint("MissingPermission")
        private void handleDeviceFound(Intent intent) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null && !mDeviceList.contains(device)) {
                mDeviceList.add(device);
                showToast("Device Found: " + device.getName());
            }
        }
    };

    public void btnGoToPairedDevicesScreenListener(View v)
    {
        @SuppressLint("MissingPermission")
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices == null || pairedDevices.isEmpty())
        {
            showToast("No se encontraron dispositivos emparejados");
        }
        else
        {
            ArrayList<BluetoothDevice> pairedBluetoothDeviceList = new ArrayList<>(pairedDevices);

            Intent intent = new Intent(BluetoothActivity.this,DeviceListActivity.class);
            intent.putParcelableArrayListExtra("device.list", pairedBluetoothDeviceList);
            startActivity(intent);
        }
    }

@SuppressLint("MissingPermission")
    public void btnScanDevicesListener(View v)
    {
        if(!isLocationEnabled(this))
        {
            showToast("Se necesita activar la Ubicacion para Buscar Dispositivos");
            return ;
        }
        showToast("Buscando Dispositivos");
        mBluetoothAdapter.startDiscovery();
    }

    @SuppressLint("MissingPermission")
    public void btnToggleBluetoothListener(View v)
    {
        if (mBluetoothAdapter.isEnabled())
        {
            mBluetoothAdapter.disable();
            showDisabled();
        }
        else
        {
            int bluetoothIntentCode = 1000;
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, bluetoothIntentCode);
        }
    }

    public static Boolean isLocationEnabled(Context context)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return lm.isLocationEnabled();
        }
        else
        {
            int mode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF);
            return (mode != Settings.Secure.LOCATION_MODE_OFF);
        }
    }

    private void showToast(String message)
    {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("SetTextI18n")
    private void showEnabled()
    {
        tvStatus.setText("Bluetooth Habilitar");
        tvStatus.setTextColor(Color.BLUE);

        btnActivate.setText("Desactivar");
        btnActivate.setEnabled(true);

        btnPair.setEnabled(true);
        btnSearch.setEnabled(true);
    }

    @SuppressLint("SetTextI18n")
    private void showDisabled()
    {
        tvStatus.setText("Bluetooth Deshabilitado");
        tvStatus.setTextColor(Color.RED);

        btnActivate.setText("Activar");
        btnActivate.setEnabled(true);

        btnPair.setEnabled(false);
        btnSearch.setEnabled(false);
    }

    @SuppressLint("SetTextI18n")
    private void showUnsupported()
    {
        tvStatus.setText("Bluetooth no es soportado por el dispositivo movil");

        btnActivate.setText("Activar");
        btnActivate.setEnabled(false);

        btnPair.setEnabled(false);
        btnSearch.setEnabled(false);
    }
}
