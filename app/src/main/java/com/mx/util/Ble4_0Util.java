package com.mx.util;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.List;


public class Ble4_0Util implements BleUtil {

    private String serviceUUid = "";		// 服务uuid
    private String readUUID = "";			// 读数据uuid
    private String writeUUID = "";	// 写数据uuid
    private String clientCharConfig = "";
    private static final int PERMISSION_REQUEST_CODE = 0x114; // 系统权限管理页面的参数
    public static  final  int READ_NOTIFY_CODE = 0x12;     //可读可通知
    public static  final  int WRITE_READ_CODE = 0x0e;     //可写可读
    public static  final  int WRITE_CODE = 0x04;     //可写可读
    public static  final  int READ_CODE = 0x02;     //可写可读
    public static  final  int WRITE_NOTIFY_CODE = 0x18;     //可写可通知

    private Activity context;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattService gattServiceMain;
    private BluetoothGattCharacteristic mDevWriteCharacteristic; // 写服务
    private BluetoothGattCharacteristic mDevReadCharacteristic; // 读服务
    private BluetoothAdapter.LeScanCallback leScanCallback;
    private BluetoothDevice curConnectDev;

    public Ble4_0Util(Activity context) {
        this.context = context;
    }

    public BluetoothDevice getCurConnectDev() {
        return curConnectDev;
    }

    @Override
    public boolean init() {
        //先检查权限 anroid 6.0以上 需要动态获取 位置权限
        checkBluetoothPermission();
        //不支持 蓝牙
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return false;
        }
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

        mBluetoothAdapter = bluetoothManager.getAdapter();
        //不支持 蓝牙
        if (mBluetoothAdapter == null) return false;

        //没有打开蓝牙
        if (!mBluetoothAdapter.isEnabled()) mBluetoothAdapter.enable();

        return true;
    }

    public boolean isConnect(){
        return mBluetoothGatt != null;
    }

    public String getServiceUUid() {
        return serviceUUid;
    }

    public void setServiceUUid(String serviceUUid) {
        this.serviceUUid = serviceUUid;
    }

    public String getReadUUID() {
        return readUUID;
    }

    public void setReadUUID(String readUUID) {
        this.readUUID = readUUID;
    }

    public String getWriteUUID() {
        return writeUUID;
    }

    public void setWriteUUID(String writeUUID) {
        this.writeUUID = writeUUID;
    }

    @Override
    public boolean connect(String blemac, final CallBack callback) {

        curConnectDev = mBluetoothAdapter.getRemoteDevice(blemac);
        if (curConnectDev == null) {
            Log.e("BLE","蓝牙" + blemac + "未找到");
           return  false;
        }
        //已连接 先断开连接
        if (null != mBluetoothGatt) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        //不能拿到 名称和 蓝牙mac的按假连接处理
        if (null == curConnectDev.getName() && null == curConnectDev.getAddress()) {
            return  false;
        }
        //连接蓝牙

        mBluetoothGatt = curConnectDev.connectGatt(context, false,new BluetoothGattCallback() {
            @Override
            public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
                super.onPhyUpdate(gatt, txPhy, rxPhy, status);
            }

            @Override
            public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
                super.onPhyRead(gatt, txPhy, rxPhy, status);
            }

            @Override
            public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (newState == BluetoothGatt.STATE_CONNECTED){
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        gatt.discoverServices();
                }
                if(newState == BluetoothGatt.STATE_DISCONNECTED){
                    disconnect();
                }
                callback.StateChange(status, newState);
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                List<BluetoothGattService> serviceList = gatt.getServices();
                for (BluetoothGattService gattService : serviceList) {
                    if (gattService.getUuid().toString().indexOf(serviceUUid) < 0) {
                        continue;
                    }
                    gattServiceMain = gattService;
                    List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                    for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                        if (mDevReadCharacteristic == null &&  gattCharacteristic.getProperties() == BluetoothGattCharacteristic.PROPERTY_NOTIFY
                        && gattCharacteristic.getUuid().toString().indexOf(readUUID) >= 0){
                            mDevReadCharacteristic = gattCharacteristic;
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(500);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    mBluetoothGatt.readCharacteristic(mDevReadCharacteristic);
                                }
                            }).start();
                            mBluetoothGatt.setCharacteristicNotification(gattCharacteristic, true);

                            List<BluetoothGattDescriptor> descriptorlist = gattCharacteristic.getDescriptors();

                            lp: for ( BluetoothGattDescriptor descriptor: descriptorlist) {
                                if (descriptor.getUuid().toString().indexOf(clientCharConfig) >= 0){
                                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                    mBluetoothGatt.writeDescriptor(descriptor);
                                    break lp;
                                }
                            }
                        }
                        if (mDevWriteCharacteristic == null && (gattCharacteristic.getProperties() ==  BluetoothGattCharacteristic.PROPERTY_WRITE
                        || gattCharacteristic.getProperties() ==  BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
                                || gattCharacteristic.getProperties() ==  WRITE_CODE)
                                && gattCharacteristic.getUuid().toString().indexOf(readUUID) >= 0){
                            mDevWriteCharacteristic = gattCharacteristic;
                        }
                    }
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                // 处理解释反馈指令
                callback.ReadValue(new String(characteristic.getValue()));
            }


            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorRead(gatt, descriptor, status);
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorWrite(gatt, descriptor, status);
            }

            @Override
            public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                super.onReliableWriteCompleted(gatt, status);
            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                super.onReadRemoteRssi(gatt, rssi, status);
            }

            @Override
            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                super.onMtuChanged(gatt, mtu, status);
            }
        });

        return true;
    }

    @Override
    public boolean disconnect() {
        if (null != mBluetoothGatt) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        mDevWriteCharacteristic = null;
        return true;
    }

    @Override
    public boolean close() {
         mBluetoothAdapter = null;
        return true;
    }

    @Override
    public boolean send(String str) {
        if (mDevWriteCharacteristic == null){
             getService();
        }
        return  sendStrToDev(str);
    }

    public boolean send(byte[] byteCmd) {
        if (mDevWriteCharacteristic == null){
            getService();
        }
        return  sendByteToDev(byteCmd);
    }

    private boolean getService() {
        if (null == mBluetoothGatt) {
            return false;
        }
        if (mDevWriteCharacteristic != null){
           return  true;
        }
        if (gattServiceMain == null) return false;

        //获取写的特征值
        List<BluetoothGattCharacteristic> characteristicList = gattServiceMain.getCharacteristics();
        for (BluetoothGattCharacteristic characteristic : characteristicList){
            if ( (characteristic.getProperties() ==  BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE
                    || characteristic.getProperties() ==  BluetoothGattCharacteristic.PROPERTY_WRITE
                    || characteristic.getProperties() ==  BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) &&
                    characteristic.getUuid().toString().indexOf(writeUUID) >= 0){
                mDevWriteCharacteristic = characteristic;
                break;
            }
        }

        return mDevWriteCharacteristic == null;
    }


    private boolean sendStrToDev(String str){
        if (mDevWriteCharacteristic == null){
            return false;
        }
        byte[] value = new byte[20];
        value[0] = (byte) 0x00;
        mDevWriteCharacteristic.setValue(value[0], BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        mDevWriteCharacteristic.setValue(str);
        return  mBluetoothGatt.writeCharacteristic(mDevWriteCharacteristic);
    }

    private boolean sendByteToDev(byte[] byteCmd){
        if (mDevWriteCharacteristic == null){
            return false;
        }
        mDevWriteCharacteristic.setValue(byteCmd);
        return  mBluetoothGatt.writeCharacteristic(mDevWriteCharacteristic);
    }

    @Override
    public boolean startScan(BluetoothAdapter.LeScanCallback callBack) {

        if (mBluetoothAdapter == null){
            return false;
        }
        if (mBluetoothAdapter.isDiscovering()) {
            return false;
        }

        mBluetoothAdapter.startLeScan(callBack);
        this.leScanCallback = callBack;
        return  true;
    }

    @Override
    public boolean stopScan() {
        if (this.leScanCallback == null){
            return  false;
        }
        mBluetoothAdapter.stopLeScan(this.leScanCallback);
        this.leScanCallback = null;
        return  true;
    }

    /*
    校验蓝牙权限
   */
    private void checkBluetoothPermission() {
        if (Build.VERSION.SDK_INT  < 23) {
          return;
        }
        //校验是否已具有模糊定位权限
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},PERMISSION_REQUEST_CODE );
        }

    }
}
