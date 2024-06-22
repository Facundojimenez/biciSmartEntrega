package com.example.bicismart;

import java.util.ArrayList;
import java.util.List;
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
    private TextView txtEstado;
    private Button btnActivar;
    private Button btnEmparejar;
    private Button btnBuscar;

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

        txtEstado = findViewById(R.id.txtEstado);
        btnActivar = findViewById(R.id.btnActivar);
        btnEmparejar = findViewById(R.id.btnEmparejar);
        btnBuscar = findViewById(R.id.btnBuscar);

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (Build.VERSION.SDK_INT >= 31)
        {
            mBluetoothAdapter = bluetoothManager.getAdapter();
            checkPermissions();
        } else
        {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            checkBTPermissions();
        }

        enableComponent();
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
        unregisterReceiver(mReceiver);
    }

    private void checkPermissions()
    {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();

        for (String p:permissions)
        {
            result = ContextCompat.checkSelfPermission(this,p);
            if (result != PackageManager.PERMISSION_GRANTED)
            {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty())
        {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]),MULTIPLE_PERMISSIONS );
        }
    }

    private void checkBTPermissions()
    {
        int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
        permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
        if (permissionCheck != 0)
        {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
        }
    }

    protected  void enableComponent()
    {
        if (mBluetoothAdapter == null)
        {
            showUnsupported();
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

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action))
            {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON)
                {
                    showToast("Activar");
                    showEnabled();
                }
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action))
            {
                mDeviceList = new ArrayList<>();
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                Intent newIntent = new Intent(BluetoothActivity.this, DeviceListActivity.class);
                newIntent.putParcelableArrayListExtra("device.list", mDeviceList);
                startActivity(newIntent);
            }
            else if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(!(mDeviceList.contains(device)))
                    mDeviceList.add(device);

                if (device != null)
                {
                    showToast("Dispositivo Encontrado:" + device.getName());
                }
            }
        }
    };


    public void btnEmparejarListener (View v)
    {
        @SuppressLint("MissingPermission")
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices == null || pairedDevices.isEmpty())
        {
            showToast("No se encontraron dispositivos emparejados");
        }
        else
        {
            ArrayList<BluetoothDevice> list = new ArrayList<>();
            list.addAll(pairedDevices);

            Intent intent = new Intent(BluetoothActivity.this,DeviceListActivity.class);
            intent.putParcelableArrayListExtra("device.list", list);
            startActivity(intent);
        }
    }

@SuppressLint("MissingPermission")
    public void btnBuscarListener (View v)
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
    public void btnActivarListener (View v)
    {
        if (mBluetoothAdapter.isEnabled())
        {
            mBluetoothAdapter.disable();
            showDisabled();
        }
        else
        {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, 1000);
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
        txtEstado.setText("Bluetooth Habilitar");
        txtEstado.setTextColor(Color.BLUE);

        btnActivar.setText("Desactivar");
        btnActivar.setEnabled(true);

        btnEmparejar.setEnabled(true);
        btnBuscar.setEnabled(true);
    }

    @SuppressLint("SetTextI18n")
    private void showDisabled()
    {
        txtEstado.setText("Bluetooth Deshabilitado");
        txtEstado.setTextColor(Color.RED);

        btnActivar.setText("Activar");
        btnActivar.setEnabled(true);

        btnEmparejar.setEnabled(false);
        btnBuscar.setEnabled(false);
    }

    @SuppressLint("SetTextI18n")
    private void showUnsupported()
    {
        txtEstado.setText("Bluetooth no es soportado por el dispositivo movil");

        btnActivar.setText("Activar");
        btnActivar.setEnabled(false);

        btnEmparejar.setEnabled(false);
        btnBuscar.setEnabled(false);
    }
}
