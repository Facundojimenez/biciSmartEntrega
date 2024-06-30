package com.example.bicismart;


import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Objects;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

public class DeviceListActivity extends Activity
{
    ListView mListView;
    private DeviceListAdapter mBluetoothDeviceListAdapter;
    private ArrayList<BluetoothDevice> mBluetoothDeviceList;
    private int positionListBluetooth;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bt_activity_paired_devices);

        mListView = findViewById(R.id.lv_paired);
        try {
            mBluetoothDeviceList = Objects.requireNonNull(getIntent().getExtras()).getParcelableArrayList("device.list");
        } catch (NullPointerException e) {
            Log.e("OnCreateBDL: ",  e.getMessage());

            throw new RuntimeException(e);
        }

        mBluetoothDeviceListAdapter = new DeviceListAdapter(this);
        mBluetoothDeviceListAdapter.setData(mBluetoothDeviceList);
        mBluetoothDeviceListAdapter.setListener(pairButtonListener);
        mListView.setAdapter(mBluetoothDeviceListAdapter);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBluetoothPairBroadcastReceiver, filter);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(mBluetoothPairBroadcastReceiver);
    }

    private void pairDevice(BluetoothDevice device)
    {
        try
        {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private final DeviceListAdapter.OnPairButtonClickListener pairButtonListener = new DeviceListAdapter.OnPairButtonClickListener()
    {
        @SuppressLint("MissingPermission")
        @Override
        public void onPairButtonClick(int position)
        {
            BluetoothDevice device = mBluetoothDeviceList.get(position);
            if (device.getBondState() == BluetoothDevice.BOND_BONDED)
            {
                positionListBluetooth = position;
                BluetoothDevice bluetoothDevice = (BluetoothDevice) mBluetoothDeviceListAdapter.getItem(positionListBluetooth);

                //se inicia el Activity de comunicacion con el bluetooth, para transferir los datos.
                //Para eso se le envia como parametro la direccion(MAC) del bluethoot Arduino
                String bluetoothAddress = bluetoothDevice.getAddress();
                Intent i = new Intent(DeviceListActivity.this, PreTrainingActivity.class);
                i.putExtra("Direccion_Bluetooth", bluetoothAddress);
                startActivity(i);
                finish();
            }
            else
            {
                showToast("Emparejando");
                positionListBluetooth = position;
                pairDevice(device);
            }
        }
    };

    private final BroadcastReceiver mBluetoothPairBroadcastReceiver = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action))
            {
                //Obtengo los parametro, aplicando un Bundle, que me indica el estado del Bluethoot
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                //se analiza si se puedo emparejar o no
                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING)
                {
                    //Si se detecto que se puedo emparejar el bluethoot
                    showToast("Emparejado");
                    BluetoothDevice device = (BluetoothDevice) mBluetoothDeviceListAdapter.getItem(positionListBluetooth);

                    //se inicia el Activity de comunicacion con el bluethoot, para transferir los datos.
                    //Para eso se le envia como parametro la direccion(MAC) del bluethoot Arduino
                    String bluetoothAddress = device.getAddress();
                    Intent i = new Intent(DeviceListActivity.this, PreTrainingActivity.class);
                    i.putExtra("Direccion_Bluetooth", bluetoothAddress);
                    startActivity(i);
                    finish();
                }  //si se detrecto un desaemparejamiento
                else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED)
                {
                    showToast("No emparejado");
                }

                mBluetoothDeviceListAdapter.notifyDataSetChanged();
            }
        }
    };

    private void showToast(String message)
    {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}


