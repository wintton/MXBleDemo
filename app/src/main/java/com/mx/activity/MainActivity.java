package com.mx.activity;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mx.adapter.BleAdapter;
import com.mx.ble2until.R;
import com.mx.util.Ble2_0Util;
import com.mx.util.BleStatus;

import java.util.Timer;

import static com.mx.util.Ble2_0Util.BLE_STATUS_CONNECT;
import static com.mx.util.Ble2_0Util.BLE_STATUS_DISCONNECT;

public class MainActivity extends AppCompatActivity implements BleStatus {
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION" };
    private ListView listView;
    private TextView flushText;
    private BleAdapter bleadapter;
    public static Ble2_0Util ble2until;
    private Timer  mTimer = new Timer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int permission = ActivityCompat.checkSelfPermission(MainActivity.this,
                "android.permission.ACCESS_FINE_LOCATION");
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // 没有写的权限，去申请写的权限，会弹出对话框
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_STORAGE,2);
        }
        listView = (ListView)this.findViewById(R.id.listView1) ;
        flushText  = (TextView)this.findViewById(R.id.reflash_text) ;
        flushText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (flushText.getText().toString().equals("刷新中")){
                    ShowToast("正在刷新，请勿重复操作");
                }else {
                    ble2until.startScanle();
                }
            }
        });
        bleadapter = new BleAdapter(this);
        listView.setAdapter(bleadapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(MainActivity.this,LinkActivity.class);
                startActivity(intent);
               ble2until.connect(bleadapter.getDevice(i));

            }
        });
        ble2until = new Ble2_0Util(MainActivity.this,new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                switch (message.what){
                    case 98:{
                        LinkActivity.linkHandler.sendEmptyMessage(99);
                        ShowToast("已连接");
                    }
                    break;
                    case 99:{
                        LinkActivity.linkHandler.sendEmptyMessage(100);
                        ShowToast("已断开连接");
                    }
                    break;
                    case 100:{
                        flushText.setText("刷新中");
                    }
                    break;
                    case 101:{
                        flushText.setText("刷新");
                    }
                    break;
                    case 102:{
                      ble2until.disconnect();
                    }
                    break;
                    case 322:{
                        String data = message.obj.toString();
                        ShowToast(data);
                    }
                }
                return false;
            }
        }));
        ble2until.Init();
        ble2until.setOnSCanleLisnter(new Ble2_0Util.onScanleDev() {
            @Override
            public void findDev(BluetoothDevice device, short rsisi) {
                if(device.getName() != null && device.getName().length() > 0){
                    bleadapter.RSISIMAp.put(device.getAddress(),(int)rsisi);
                    bleadapter.addDevice(device);
                }
            }
        });
        ble2until.setonDevValueChangLisnter(new Ble2_0Util.onDevValueChang() {
            @Override
            public void getRep(String data) {
                if (LinkActivity.linkHandler != null) {
                    Message message = new Message();
                    message.what = 101;
                    message.obj = data;
                    LinkActivity.linkHandler.sendMessage(message);
                }
            }
        });
        ble2until.startScanle();
    }

    private void ShowToast(String msg){
        Toast.makeText(MainActivity.this,msg,Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ble2until.close();
    }

    @Override
    public void getBleStatus(String data, int type) {
        if (LinkActivity.linkHandler != null){
            switch (type){
                case BLE_STATUS_CONNECT:{
                    LinkActivity.linkHandler.sendEmptyMessage(99);
                }
                break;
                case BLE_STATUS_DISCONNECT:{
                    LinkActivity.linkHandler.sendEmptyMessage(100);
                }
                break;
            }
        }
    }
}
