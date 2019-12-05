package com.mx.util;

import android.bluetooth.BluetoothAdapter;

public interface BleUtil {


    boolean init();
    boolean connect(String blemac, CallBack callback);
    boolean disconnect();
    boolean close();
    boolean send(String str);
    boolean startScan(BluetoothAdapter.LeScanCallback mLeScanCallback);
    boolean stopScan();

    interface CallBack{
        void StateChange(int state, int newState);
        void ReadValue(String value);
    }
}
