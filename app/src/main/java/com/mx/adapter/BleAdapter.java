package com.mx.adapter;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.mx.ble2until.R;

import java.util.ArrayList;
import java.util.HashMap;

public class BleAdapter extends BaseAdapter {

    private ArrayList<BluetoothDevice> mLeDevices;
    private LayoutInflater mInflator;
    private Activity mContext;
    private int								mSelected;		// 当前选中的item index
    public static HashMap<String, Integer> RSISIMAp = new HashMap<String, Integer>();
    public static  HashMap<String, BluetoothDevice> DevMap = new HashMap<String, BluetoothDevice>();

    public BleAdapter(Activity c) {
        super();
        mContext = c;
        mLeDevices = new ArrayList<BluetoothDevice>();
        mInflator = mContext.getLayoutInflater();
        mSelected = -1;
    }

    public void addDevice(BluetoothDevice device) {
        if (!mLeDevices.contains(device)) {
            mLeDevices.add(device);
            notifyDataSetChanged();
        }
    }

    public BluetoothDevice getDevice(int position) {
        return mLeDevices.get(position);
    }

    public void clear() {
        mLeDevices.clear();
    }

    @Override
    public int getCount() {
        return mLeDevices.size();
    }

    @Override
    public Object getItem(int i) {
        return mLeDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public int getSelected() {
        return mSelected;
    }

    public void setSelected(int nSelected) {
        this.mSelected = nSelected;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        // General ListView optimization code.
        if (view == null) {
            view = mInflator.inflate(R.layout.item_dev, null);
            viewHolder = new ViewHolder();
            viewHolder.deviceAddress = (TextView) view
                    .findViewById(R.id.device_address);
            viewHolder.deviceName = (TextView) view
                    .findViewById(R.id.device_name);
            viewHolder.Rsisi = (TextView)view.findViewById(R.id.device_rsisi);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        BluetoothDevice device = mLeDevices.get(i);
        final String deviceName = device.getName();
        if (deviceName != null && deviceName.length() > 0){
            viewHolder.deviceName.setText(deviceName);
        }
        else {
            viewHolder.deviceName.setText("未知设备");
        }
        if (RSISIMAp.get(device.getAddress()) != null) {
            viewHolder.Rsisi.setText(RSISIMAp.get(device.getAddress())+"");
        }
        viewHolder.deviceAddress.setText(device.getAddress());


        return view;
    }

    class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView Rsisi;
    }
}
