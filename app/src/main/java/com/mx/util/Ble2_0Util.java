package com.mx.util;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

public class Ble2_0Util {

    private BluetoothAdapter adapter = null;
    private BluetoothReceiver bluetoothReceiver = null;
    public static  final  int BLE_STATUS_DISCONNECT = 0;
    public static  final  int BLE_STATUS_CONNECT = 1;
    private Context  mContext;
    private Handler mHandler;
    private boolean isScanle = false;	//是否在扫描中
    private 	InputStream in = null;
    private OutputStream  out = null;
    private   BluetoothSocket socket = null;
    public static String BT_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    private onScanleDev	onScanleDev;
    private onDevValueChang onDevValueChang;
    private BluetoothDevice ble;
    private volatile boolean isConnect;

    private String  RecData = "";

    public Ble2_0Util(Context mContext, Handler mHandler) {
        this.mContext = mContext;
        this.mHandler = mHandler;
    }

    public void setUUID(String data){
        this.BT_UUID = data;
    }
    public String getUUID(){
        return this.BT_UUID;
    }
    public void setOnSCanleLisnter(onScanleDev on){
        this.onScanleDev = on;
    }
    public void setonDevValueChangLisnter(onDevValueChang on){
        this.onDevValueChang = on;
    }


    /**
     * 初始化蓝牙适配器
     */
    public void Init(){
        //获取蓝牙适配器
        adapter=BluetoothAdapter.getDefaultAdapter();
        if(adapter==null){
            sendMsg(322,"当前设备不支持蓝牙功能");
        }
        if(!adapter.isEnabled()){
	             /* Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	              startActivity(i);*/
            adapter.enable();

        }

        //动态注册广播

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        // 注册广播接收器，接收并处理搜索结果
        bluetoothReceiver=new BluetoothReceiver();
        mContext.registerReceiver(bluetoothReceiver, intentFilter);


        ServerThread serverThread = new ServerThread();
        serverThread.start();
        //开启被其它蓝牙设备发现的功能
//	          if (adapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
//	              Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//	              //设置为一直开启
//	              i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
//	              mContext.startActivity(i);
//	          }
    }
    /**
     * 关闭连接
     */
    public void disconnect(){
        if (socket != null) {
            try {
                canclePeidui(); //取消配对
                isConnect = false;
                socket.close();
                socket = null;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    private Boolean canclePeidui(){
        Boolean returnValue;
        try {
            if (ble.getBondState() == BluetoothDevice.BOND_BONDED){
                Method removeBondMethod = BluetoothDevice.class.getMethod("removeBond");
                returnValue = (Boolean) removeBondMethod.invoke(ble);

                return  returnValue;
            }

        }catch (Exception e){

        }
        return false;
    }
    /**
     * 关闭适配器
     * @return true - 已关闭 false - 未关闭
     */
    public boolean close(){
        BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();
        disconnect();
        if (adapter!=null) {
            if(adapter.isEnabled()){
                adapter.disable();
                Log.d("BruceZhang", "设备关闭中。。。");
                return true;
            } else {
                Log.d("BruceZhang", "设备已经关闭，不需再进行操作。。。");
                return true;
            }
        }
        else {
            Log.d("BruceZhang", "此设备不存在蓝牙设备。。。");
        }
        if (bluetoothReceiver != null) {
            mContext.unregisterReceiver(bluetoothReceiver);
        }

        return false;
    }

    /*
     * 蓝牙的可见性设置
     * 1.设置的本地设备的可见性，即能否被其他的蓝牙设备扫描到
     * 2.蓝牙可见的持续时间默认是120秒，这里修改为180秒，以作为参考
     */
    public void showBle(){
        Intent discoverableIntent = new  Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 180);
        mContext.startActivity(discoverableIntent);
    }
    /**
     * 本地蓝牙设备扫描远程蓝牙设备
     *   使用BluetoothAdapter的startDiscovery()方法来搜索蓝牙设备
     startDiscovery()方法是一个异步方法，调用后会立即返回。该方法会进行对其他蓝牙设备的搜索，该过程会持续12秒。
     该方法调用后，搜索过程实际上是在一个System Service中进行的，
     所以可以调用cancelDiscovery()方法来停止搜索（该方法可以在未执行discovery请求时调用）。
     请求Discovery后，系统开始搜索蓝牙设备，在这个过程中，系统会发送以下三个广播：
     ACTION_DISCOVERY_START：开始搜索
     ACTION_DISCOVERY_FINISHED：搜索结束
     ACTION_FOUND：找到设备，这个Intent中包含两个extra fields：
     EXTRA_DEVICE和EXTRA_CLASS，分别包含BluetooDevice和BluetoothClass。
     */
    public void startScanle(){
        if (adapter == null) {
            return;
        }
        isScanle = true;
        adapter.startDiscovery();
        mHandler.sendEmptyMessage(100);  //开始扫描
        if (mHandler != null) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isScanle = false;
                    mHandler.sendEmptyMessage(101);  //扫描结束
                }
            }, 12000);
        }
    }
    public void reconnect(){
        if (ble == null) {
            return;
        }
        connect(ble);
    }

    /**
     * 停止扫描
     */
    public void stopScanle(){
        if (isScanle) {
            sendMsg(322, "正在扫描中");
        }else{
            adapter.cancelDiscovery();
            sendMsg(322, "停止扫描");
        }
    }

    /**
     * 打开蓝牙设备
     */
    public void openBle(){
        if (adapter!=null) {
            if (!adapter.isEnabled()) {
                final Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                mContext.startActivity(intent);
                Log.d("BruceZhang", "蓝牙设备正在打开。。。");
                sendMsg(322, "打开蓝牙设备");
            }
        }
        else {
            Log.d("BruceZhang", "此设备不存在蓝牙。。。");
            sendMsg(322, "不存在蓝牙设备");
        }
    }

    /**
     * 连接设备  蓝牙对象
     * @param device
     */
    public void connect(BluetoothDevice device){
        ble = device;
        sendMsg(97,"连接中");
        if (adapter.isDiscovering()) {
            adapter.cancelDiscovery();
        }
        //连接设备

        //创建Socket
        clientThread clientThread = new clientThread();
        clientThread.start();

    }
    /**
     * 大宋数据给蓝牙
     */
    public void sendBleMsg(final String msg){
        byte[] bytes = msg.getBytes();

        if (out != null) {
            try {
                //发送数据
                out.write(bytes);
                out.flush();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void sendBleMsg(final byte[] msg){

        if (out != null) {
            try {
                //发送数据
                out.write(msg);
                out.flush();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    class  ServerThread extends  Thread{
        @Override
        public void run() {
            try {
                BluetoothServerSocket mserverSocket = adapter.listenUsingRfcommWithServiceRecord("btspp",
                        UUID.fromString(BT_UUID));
                show("服务端:等待连接");

                socket = mserverSocket.accept();
                show("服务端:连接成功");

                ConnectThread mreadThread = new ConnectThread(socket,true);
                mreadThread.start();
                show("服务端:启动接受数据");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public  void show(String msg){
        Log.e("error",msg);
    }

    class ConnectThread extends Thread{

        private BluetoothSocket socket;


        public ConnectThread(BluetoothSocket socket,boolean iscon) {
            this.socket = socket;
            isConnect = iscon;
        }

        @Override
        public void run() {
            if (socket == null) {
                isConnect = false;
                sendMsg(99,"连接失败");
            }else{
                try {
                    isConnect = true;
                    in = socket.getInputStream();
                    out = socket.getOutputStream();
                    if(in != null && out != null){
                        sendMsg(98,"已连接");
                        while (isConnect) {

                            byte[] data = new byte[1024];
                            int len = 0;
                            try{
                                len =  in.read(data);
                            }catch (Exception e){
                                Log.e("error",e.toString());
                            }
                            if (len > 0) {
                                final byte[] dataz = new byte[len];
                                System.arraycopy(data, 0, dataz, 0, len);
                                String strData = ASCIITOStr(dataz);
                                show(strData);
                                analyzeData(strData);
                            }
                            Thread.sleep(1000);
                        }
                    }else {
                        isConnect = false;
                        sendMsg(99,"连接失败");
                    }

                } catch (IOException e) {
                    sendMsg(99,"断开连接");
                    e.printStackTrace();
                    try {
                        in.close();
                        out.close();
                        socket.close();
                        in = null;
                        out = null;
                        socket = null;
                        isConnect = false;
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    sendMsg(99,"断开连接");
                    e.printStackTrace();
                    try {
                        in.close();
                        out.close();
                        socket.close();
                        in = null;
                        out = null;
                        socket = null;
                        isConnect = false;
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    }
    private synchronized void  analyzeData(String curData){
//        RecData += curData;
//        int start =  RecData.lastIndexOf("C=");
//        if (start >= 0){
//            RecData = RecData.substring(start);
//            int endPos = RecData.indexOf("D");
//            if (endPos >= 0){
//                String sendData = RecData.substring(0,endPos + 1);
//                RecData = RecData.substring(endPos + 1);
//                if (onDevValueChang != null) {
//                    onDevValueChang.getRep(sendData);
//                }
//            }
//        }
        RecData += curData;
        int start =  RecData.lastIndexOf("PH=");
        if (start >= 0){
            RecData = RecData.substring(start);
            int endPos = RecData.indexOf("ppm");
            if (endPos >= 0){
                String sendData = RecData.substring(0,endPos + 3);
                RecData = RecData.substring(endPos + 3);
                if (onDevValueChang != null) {
                    onDevValueChang.getRep(sendData);
                }
            }
        }
    }
    private String ASCIITOStr(byte[] data){
        StringBuffer stringBuffer = new StringBuffer();
        for (byte each:data){
            int value = each & 0xff;
            stringBuffer.append((char)value);
        }
        return stringBuffer.toString();
    }

    private void sendMsg(int sendCode,String sendStr){
        if (mHandler == null) {
            return;
        }
        Message message = new Message();
        message.what = sendCode;
        message.obj = sendStr;
        mHandler.sendMessage(message);
    }
    private void sendMsg(int sendCode,BluetoothDevice sendStr){
        if (mHandler == null) {
            return;
        }
        Message message = new Message();
        message.what = sendCode;
        message.obj = sendStr;
        mHandler.sendMessage(message);
    }
    //广播接收器，当远程蓝牙设备被发现时，回调函数onReceiver()会被执行
    private class BluetoothReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {

                BluetoothDevice device=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {

                    //信号强度。
                    short rssi = intent.getExtras().getShort(
                            BluetoothDevice.EXTRA_RSSI);
                    if (onScanleDev != null) {
                        onScanleDev.findDev(device,rssi);
                    }

                }

//                Log.d("BruceZhang", "扫描到可连接的蓝牙设备："+device.getAddress());
//                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
//
//                }

            }else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(intent.getAction())) {
                isScanle = false;
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
                isScanle = true;
            }else  if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())){
                // 获取蓝牙设备的连接状态
                int connectState = ble.getBondState();
                // 已配对
                if (connectState == BluetoothDevice.BOND_BONDED) {
                    try {
                        show("客户端:开始连接:");
                        clientThread clientConnectThread = new clientThread();
                        clientConnectThread.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }else {
                    bondBT();
                }
            }
        }
    }
    /**
     * 绑定蓝牙
     *
     * @param
     */
    private void bondBT() {
            show("客户端:配对蓝牙开始");
            // 搜索蓝牙设备的过程占用资源比较多，一旦找到需要连接的设备后需要及时关闭搜索
            adapter.cancelDiscovery();
            // 获取蓝牙设备的连接状态
            int connectState = ble.getBondState();

            switch (connectState) {
                // 未配对
                case BluetoothDevice.BOND_NONE:
                    show("客户端:开始配对");
                    try {
                        Method createBondMethod = BluetoothDevice.class.getMethod("createBond");
                        createBondMethod.invoke(ble);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                // 已配对
                case BluetoothDevice.BOND_BONDED:
                    try {
                        show("客户端:开始连接:");
                        clientThread clientConnectThread = new clientThread();
                        clientConnectThread.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
    }

    /**
     * 开启客户端
     */
    private class clientThread extends Thread {
        @Override
        public void run() {
            try {
                //创建一个Socket连接：只需要服务器在注册时的UUID号
                socket = ble.createRfcommSocketToServiceRecord(UUID.fromString(BT_UUID));
                //连接
                show("客户端:开始连接...");
                socket.connect();
                show("客户端:连接成功");
                //启动接受数据
                show("客户端:启动接受数据");
                ConnectThread mreadThread = new ConnectThread(socket,true);
                mreadThread.start();
            } catch (IOException e) {
                sendMsg(99,"连接失败");
                show("客户端:连接服务端异常！断开连接重新试一试");
                e.printStackTrace();
            }
        }
    }

    public  interface onScanleDev{
        void findDev(BluetoothDevice device,short rsisi);
    }
    public interface onDevValueChang{
        void getRep(String str1);
    }


}

