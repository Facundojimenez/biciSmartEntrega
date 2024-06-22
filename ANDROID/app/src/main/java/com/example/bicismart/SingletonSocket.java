package com.example.bicismart;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.Handler;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.UUID;

public class SingletonSocket
{
    private static SingletonSocket instance;
    private BluetoothSocket btSocket = null;
    private boolean handleCreated;
    private Handler bluetoothIn;
    // SPP UUID service  - Funciona en la mayoria de los dispositivos
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @SuppressLint("MissingPermission")
    private SingletonSocket(String address, BluetoothManager bluetoothManager)
    {
        handleCreated = false;
        BluetoothAdapter btAdapter;
        if (Build.VERSION.SDK_INT >= 31)
        {
            btAdapter = bluetoothManager.getAdapter();
        } else
        {
            btAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try
        {
            btSocket = createBluetoothSocket(device);
        }
        catch (IOException e)
        {

        }
        // Establish the Bluetooth socket connection.
        try
        {
            btSocket.connect();
        }
        catch (IOException e)
        {
            try
            {
                btSocket.close();
            }
            catch (IOException e2)
            {
                //insert code to deal with this
            }
        }
    }

    public static SingletonSocket getInstance(String address, BluetoothManager bluetoothManager)
    {
        if(SingletonSocket.instance == null)
        {
            SingletonSocket.instance = new SingletonSocket(address, bluetoothManager);
        }
        return SingletonSocket.instance;
    }

    //Metodo que crea el socket bluethoot
    @SuppressLint("MissingPermission")
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException
    {
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    public BluetoothSocket getBtSocket()
    {
        return this.btSocket;
    }

    public void close()
    {
        try
        {
            this.btSocket.close();
            this.instance = null;
        } catch (IOException e)
        {

        }
    }

    public boolean handleWasCreated()
    {
        return this.handleCreated;
    }

    public void handleCreated()
    {
        this.handleCreated = true;
    }

    public void setHandle(Handler handler)
    {
        this.bluetoothIn = handler;
    }

    public Handler getHandler()
    {
        return this.bluetoothIn;
    }
}
