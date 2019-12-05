
## 安卓蓝牙4.0、蓝牙2.0实现（附Demo）
楼主最近在整理 安卓蓝牙 4.0 和蓝牙2.0的代码，之前由于还不大了解具体流程，就写的有些乱，现在重新整理了下，也封装了，使用起来就比之前好多了,就顺便分享给大家把，下面我大概讲一下具体的逻辑和使用流程吧；
所需权限：

```java
    <uses-permission android:name="android.permission.BLUETOOTH"/>
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```
需要注意的时，android 6.0以上需要获取到设备的位置信息权限才可以，使用蓝牙去扫描，否则你就扫描不到蓝牙设备，你以后发现扫描不出设备且附近有蓝牙设备时就看下自己是不是没有申请位置信息权限且打开位置信息，还有就是 android6.0以上的设备需要动态申请位置权限才可以，申请代码如下：

```java
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
```

### 1.蓝牙4.0
先使用 上下文中的 包管理器去检测当前设备是否支持蓝牙，并拿到对应的蓝牙适配器对象，检测当前设备蓝牙是否打开，若未打开，申请打开蓝牙：

```java
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
```
拿到正常的蓝牙适配器对象后，就可以去实现扫描设备了，扫描就比较简单了，用蓝牙的适配器的scan方法即可，

```java
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
```
然后就是取消扫描

```java
    @Override
    public boolean stopScan() {
        if (this.leScanCallback == null){
            return  false;
        }
        mBluetoothAdapter.stopLeScan(this.leScanCallback);
        this.leScanCallback = null;
        return  true;
    }
```
扫描到设备后的下一步就是去连接对应的设备，楼主 这里做了下简化，先找到该蓝牙对象，然后断开当前连接，再去连接新的对象，连接成功，需要延时500ms左右去获取这个蓝牙对象的 相关服务，拿到您所需要的服务后再去 使能对应的 特征值，并记录下可写 、可读特征值，等下我们下发数据的时候就需要用到这个可写
，楼主这里为了兼容大多数的设备服务，这里就是用的for循环去模糊匹配我设置好所需获取的片段id，若你有完整的 服务UUID 和 读写特征值UUID 您可以指定代码去精确匹配并返回，连接代码如下：

```java
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
                        //连接成功 调用发现 设备的所有服务，每发现一个就会回调当前的onServicesDiscovered方法
                        gatt.discoverServices();
                }
                if(newState == BluetoothGatt.STATE_DISCONNECTED){
                	//将当前对象标记为已断开连接 也可增加相关业务代码
                    disconnect();
                }
                callback.StateChange(status, newState);
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                //获此次发现的所有 服务对象
                List<BluetoothGattService> serviceList = gatt.getServices();
                //遍历该对象
                for (BluetoothGattService gattService : serviceList) {
                    if (gattService.getUuid().toString().indexOf(serviceUUid) < 0) {
                        continue;
                    }
                    //获取到 所需要的服务对象 并记录
                    gattServiceMain = gattService;
                    //获取该对象中的所有特征值
                    List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                    //遍历所有特征值
                    for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                        //获取到 可通知的特征值 并使能它，不然你将无法收到 蓝牙设备发送给你的消息
                        if (mDevReadCharacteristic == null &&  gattCharacteristic.getProperties() == BluetoothGattCharacteristic.PROPERTY_NOTIFY
                        && gattCharacteristic.getUuid().toString().indexOf(readUUID) >= 0){
                        	//记录该特征值
                            mDevReadCharacteristic = gattCharacteristic;
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(500);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    //将该特征值 设置为当前回调的可读特征值
                                    mBluetoothGatt.readCharacteristic(mDevReadCharacteristic);
                                }
                            }).start();
                            //使能该通知
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
                        //获取并记录可写特征值 
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
                //这边是接收到蓝牙信息，我这边就直接用一个回调，转发出去了
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
```
设备连接成功后就可以去向蓝牙设备发送信息了,我这边展示了
个方法，一个是直接发送字符串，一个是发送字节组，调用连接的回调对象 mBluetoothGatt去设置后 write 就可以了
```java
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
```
然后就是一般的断开连接和关闭了

```java
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
```
### 2.蓝牙2.0
蓝牙2.0的通讯方式 有点像socket 和 服务端的通讯，手机端就是充当服务端的角色，当然这里的socket 并不是 我们建立tcp连接时候的那个socket，有专门提供的蓝牙socket  BluetoothSocket，下面来看下具体使用流程吧：
先是一如既往的初始化：

```java
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
```
然后就是扫描设备，蓝牙2.0的扫描设备方法和蓝牙4.0的就完全不一样了，蓝牙2.0的需要注册一个 广播（bordercast） 去接受 信息，每扫描到一个蓝牙设备对象时，就会将这个对象 以广播的形式发出，我们动态注册广播后去截取对应的广播就可以了
```java
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
```
蓝牙2.0和蓝牙4.0不一样之处，就是在连接之前需要去发起配对，配对成功后才可以

```
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
```

然后需要像Tcp 服务端那样，新建一个线程 去等待 客户端连接

```
 ServerThread serverThread = new ServerThread();
 serverThread.start();
 class  ServerThread extends  Thread{
        @Override
        public void run() {
            try {
                BluetoothServerSocket mserverSocket = adapter.listenUsingRfcommWithServiceRecord("btspp",
                        UUID.fromString(BT_UUID));
                show("服务端:等待连接");

                socket = mserverSocket.accept();  //同样会阻塞
                show("服务端:连接成功");
				//启动另一个处理该 socket
                ConnectThread mreadThread = new ConnectThread(socket,true);
                mreadThread.start();
                show("服务端:启动接受数据");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
```
客户端连接成功会，新建另一个线程去处理 接收到的数据

```
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
```
当然你也可以将当前设备作为客户端，硬件作为服务端去建立连接，下面是 安卓充当客户端去连接的示例

```
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
    class clientThread extends Thread {
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
```
最后就是发送消息，其实也和tcp协议一样，类似于用流去write，

```
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
```
好了，差不多就是这些了
