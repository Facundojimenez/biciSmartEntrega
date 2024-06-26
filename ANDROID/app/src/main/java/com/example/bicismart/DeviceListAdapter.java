package com.example.bicismart;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class DeviceListAdapter extends BaseAdapter
{
    private final LayoutInflater mInflater;
    private List<BluetoothDevice> bluetoothDeviceList;
    private OnPairButtonClickListener mListener;

    public DeviceListAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
    }
    public void setData(List<BluetoothDevice> data) {
        bluetoothDeviceList = data;
    }
    public void setListener(OnPairButtonClickListener listener) {
        mListener = listener;
    }
    public int getCount() {
        return (bluetoothDeviceList == null) ? 0 : bluetoothDeviceList.size();
    }
    public Object getItem(int position) {
        return bluetoothDeviceList.get(position);
    }
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint({"MissingPermission", "InflateParams"})
    public View getView(final int position, View convertView, ViewGroup parent)
    {
        ViewHolder holder;

        if (convertView == null)
        {
            convertView	= mInflater.inflate(R.layout.bt_list_item_device, null);
            holder = new ViewHolder();
            holder.nameTv = convertView.findViewById(R.id.tv_name);
            holder.addressTv = convertView.findViewById(R.id.tv_address);
            holder.pairBtn = convertView.findViewById(R.id.btn_pair);
            convertView.setTag(holder);
        }
        else
        {
            holder = (ViewHolder) convertView.getTag();
        }

        BluetoothDevice device	= bluetoothDeviceList.get(position);

        holder.nameTv.setText(device.getName());
        holder.addressTv.setText(device.getAddress());
        holder.pairBtn.setText((device.getBondState() == BluetoothDevice.BOND_BONDED) ? "Connect" : "Pair");

        holder.pairBtn.setOnClickListener(v -> {
            if (mListener != null)
            {
                mListener.onPairButtonClick(position);
            }
        });

        return convertView;
    }

    static class ViewHolder
    {
        TextView nameTv;
        TextView addressTv;
        TextView pairBtn;
    }

    public interface OnPairButtonClickListener
    {
        void onPairButtonClick(int position);
    }
}