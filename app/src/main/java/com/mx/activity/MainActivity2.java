package com.mx.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mx.adapter.BleAdapter;
import com.mx.ble2until.R;
import com.mx.util.Ble2_0Util;
import com.mx.util.Ble4_0Util;
import com.mx.util.BleUtil;

import java.util.Timer;

public class MainActivity2 extends AppCompatActivity {
    public static Ble4_0Util ble4Util;
    private ListView listView;
    private static TextView flushText;
    private BleAdapter bleadapter;
    private static ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        ble4Util = new Ble4_0Util(this);
        progressBar = (ProgressBar)this.findViewById(R.id.progressBar);
        listView = (ListView)this.findViewById(R.id.listView1);
        flushText  = (TextView)this.findViewById(R.id.reflash_text);
        flushText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (flushText.getText().toString().equals("刷新中")){
                    ShowToast("正在刷新，请勿重复操作");
                }else {
                    ble4Util.startScan(new BluetoothAdapter.LeScanCallback() {
                        @Override
                        public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
                            bleadapter.addDevice(bluetoothDevice);
                        }
                    });
                }
            }
        });

        bleadapter = new BleAdapter(this);
        listView.setAdapter(bleadapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ble4Util.stopScan();
                ble4Util.connect(bleadapter.getDevice(i).getAddress(), new BleUtil.CallBack() {
                    @Override
                    public void StateChange(int state, int newState) {
                        String value = null;
                        if (newState == BluetoothGatt.STATE_CONNECTED){
                            value = "连接成功";
                        } else if (newState == BluetoothGatt.STATE_DISCONNECTED){
                            value = "连接失败";
                        } else if(newState == BluetoothGatt.STATE_CONNECTING){
                            value = "连接设备中";
                        } else if(newState == BluetoothGatt.STATE_DISCONNECTING){
                            value = "断开连接中";
                        }
                        if ( LinkActivity2.linkHandler != null && value != null){
                            //发送连接成功通知
                            Message message = new Message();
                            message.what = 99;
                            message.obj = value;
                            LinkActivity2.linkHandler.sendMessage(message);
                        }
                    }

                    @Override
                    public void ReadValue(String value) {
                        if ( LinkActivity2.linkHandler != null){
                            Message message = new Message();
                            message.what = 101;
                            message.obj = value;
                            LinkActivity2.linkHandler.sendMessage(message);
                        }
                    }
                });
                Intent intent = new Intent(MainActivity2.this,LinkActivity2.class);
                startActivity(intent);
            }
        });

        ble4Util.init();
    }


    private void ShowToast(String msg){
        Toast.makeText(MainActivity2.this,msg,Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ble4Util.startScan(new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] bytes) {
                if (bluetoothDevice.getName() != null && bluetoothDevice.getName().length() > 0){
                    BleAdapter.RSISIMAp.put(bluetoothDevice.getAddress(),rssi);
                    bleadapter.addDevice(bluetoothDevice);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ble4Util.close();
    }


}
