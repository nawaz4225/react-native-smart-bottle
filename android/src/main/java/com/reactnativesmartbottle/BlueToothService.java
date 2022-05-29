package com.reactnativesmartbottle;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.List;
import java.util.UUID;

/**
 * Created by Administrator on 2016/11/7.
 */

public class BlueToothService extends Service{

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattService  TxService;

    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    private String mBluetoothDeviceAddress;
    private String TAG = "BlueToothService";

    public final static String ACTION_GATT_CONNECTED = "com.hg.skinanalyze.ACTION_GATT_CONNECTED";//连接成功
    public final static String ACTION_GATT_DISCONNECTED = "com.hg.skinanalyze.ACTION_GATT_DISCONNECTED";//连接断开
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.hg.skinanalyze.ACTION_GATT_SERVICES_DISCOVERED";//发现服务
    public final static String ACTION_DATA_AVAILABLE = "com.hg.skinanalyze.ACTION_DATA_AVAILABLE";//有效数据
    public final static String ACTION_DATA_Notification = "com.hg.skinanalyze.ACTION_DATA_Notification";//传送数据
    public final static String ACTION_DATA_WRITE_SUCCESS = "com.hg.skinanalyze.ACTION_DATA_WRITE_SUCCESS";//写入成功
    public final static String ACTION_DATA_WRITE_FAIT = "com.hg.skinanalyze.ACTION_DATA_WRITE_FAIT";//写入失败
    public final static String EXTRA_DATA = "com.hg.skinanalyze.EXTRA_DATA";//数据传递标示
    public final static String DEVICE_DOES_NOT_SUPPORT_UART = "com.hg.skinanalyze.DEVICE_DOES_NOT_SUPPORT_UART";//不支持连接

    /*皮肤*/
    public static final UUID TX_SERVICE_UUID = UUID.fromString("14839ac4-7d7e-415c-9a42-167340cf2339");//皮肤测试仪服务的UUID  具备测试服务
    public static final UUID RX_CHAR_UUID = UUID.fromString("0734594a-a8e7-4b1a-a6b1-cd5243059a57");//char property:NOTIFY     具备通知属性
    public static final UUID TX_CHAR_UUID = UUID.fromString("8b00ace7-eb0b-49b0-bbe9-9aee0a26e1a3");//char property:WRITE_NO_RESPONSE|WRITE|  具备写的属性

    //所有的回调方法事件
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private BluetoothGattCallback mGattCallBack = new BluetoothGattCallback() {

        //发现新服务端  第二步  发现服务的回调
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            /*List<BluetoothGattService> services = gatt.getServices();*/
            Log.e(TAG,"onServicesDiscovered 运行了");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG,"onServicesDiscovered ------GATT_SUCCESS------");
                broadcastUpate(ACTION_GATT_SERVICES_DISCOVERED);
                enableTXNotification();
            }
        }

        //当连接状态发生改变
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.e(TAG,"onConnectionStateChange 运行了");
            String action;
            if (newState == BluetoothProfile.STATE_CONNECTED){//当蓝牙设备已经连接  第一步
                //连接成功后，我们就要去寻找我们所需要的服务，这里需要先启动服务发现
                mBluetoothGatt.discoverServices();
                Log.e(TAG,"onConnectionStateChange ------STATE_CONNECTED------");
                action = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpate(action);
            }else if(newState == BluetoothProfile.STATE_DISCONNECTED){//当设备连接断开
                Log.e(TAG,"onConnectionStateChange ------STATE_DISCONNECTED------");
                action = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                broadcastUpate(action);
                if (mBluetoothGatt != null){
                    mBluetoothGatt.disconnect();
                    mBluetoothGatt = null;
                }
            }
        }

        //第三步  向特征中写入数据
        //接收到返回数据的前提是我们设置了该特征具有Notification功能，
        // 所以完整的写操作代码应该是这样的（注意设置特征Notification的代码要放在最前）
        // 即enableTXNotification()方法，该方法要在write(String)方法前面运行，才会回调onCharacteristicWrite方法
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.e(TAG,"onCharacteristicWrite 运行了");
            Log.e(TAG,"onCharacteristicWrite ------write------");
            if (BluetoothGatt.GATT_SUCCESS == status){
                byte[] write = characteristic.getValue();
                Log.e(TAG,"onCharacteristicWrite ------"+ bytesToHexString(write) +"------");
            }
        }


        //以下数据接收的两种方式：读取和通知。读取的方式速度较慢，但结果更稳定，通知的方式相反。
        // 在实际的使用过程中多用通知的方式。下面先介绍读取的方式，读取方式需要读取的Characteristic具有读取权限

        //读取的方式   //从特征中读取数据
        //读写特性(如果对一个特性启用通知,当远程蓝牙设备特性发送变化，回调函数onCharacteristicChanged( ))被触发。)
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            mBluetoothGatt.readCharacteristic(characteristic);
            Log.e(TAG,"onCharacteristicRead 运行了");
            Log.e(TAG,"onCharacteristicRead ------red------");
            if (status == BluetoothGatt.GATT_SUCCESS){
                broadcastUpate(ACTION_DATA_AVAILABLE,characteristic);
            }
        }

        //通知的方式(接下来是通知的方式，其中通知方式的设置函数即是上文的通知函数，在设置成功之后，通过通知回调函数获取结果)
        //获得数据  第五步  //从特征中读取数据
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.e(TAG,"onCharacteristicChangedRead 运行了");
            Log.e(TAG,"onCharacteristicChanged ------Changed red------");
            Log.e(TAG,"onCharacteristicRead ------"+ bytesToHexString(characteristic.getValue()) +"------");
            //读取到值，在这里读数据
            broadcastUpate(ACTION_DATA_Notification,characteristic);
        }

        //远程设备信号强度
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        }

    };

    /**
     * 发送广播  不携带数据
     * @param action
     */
    private void broadcastUpate(String action){
        Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    /**
     * 发送广播  携带数据
     * @param aciton
     * @param characteristic
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void broadcastUpate(String aciton,BluetoothGattCharacteristic characteristic){
        Intent intent = new Intent(aciton);
        if (RX_CHAR_UUID.equals(characteristic.getUuid())){//这是心率测量配置文件（通知uuid等同才可通过）
            byte[] data = characteristic.getValue();
            intent.putExtra(EXTRA_DATA, data);
        }else{
            Log.e(TAG,"失败");
        }
        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    private Binder mBinder = new myBinder();

    /**
     * 外部调用  返回BlueToothService
     */
    public  class myBinder extends Binder{
        public BlueToothService getService(){
            return BlueToothService.this;
        }
    }

    /**
     * 初始化 BlueTooth相关
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public boolean initalize(){
        Log.e(TAG,"initalize 运行了");
        if (mBluetoothManager == null){
            mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
            if (mBluetoothManager == null){
                Log.e(TAG,"mBluetoothManager ------null------");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null){
            Log.e(TAG,"mBluetoothAdapter ------null------");
            return false;
        }
        return true;
    }

    /**
     * 获取写的权限（就是能够通过蓝牙发送指令给设置）
     * 通常BLEapp需要被通知，如果BLE设备的特性发生了改变。
     * 使用setCharacteristicNotification方法为一个特性设置通知
     * 一旦启动了属性通知（ notifications for acharacteristic）,如果在远程设备上characteristic 发生改变，onCharacteristicChanged() 回调函数将被启动
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void enableTXNotification(){
        Log.e(TAG,"enableTXNotification 运行了");
        if (mBluetoothGatt == null){
            Log.e(TAG,"mBluetoothGatt ------null------");
            broadcastUpate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }

        TxService = mBluetoothGatt.getService(TX_SERVICE_UUID);
        if (TxService == null){
            Log.e(TAG,"BluetoothGattService ------null------");
            broadcastUpate(DEVICE_DOES_NOT_SUPPORT_UART);
        }

        BluetoothGattCharacteristic TxChar = TxService.getCharacteristic(RX_CHAR_UUID);//通知
        if (TxChar == null){
            Log.e(TAG,"BluetoothGattCharacteristic ------null------");
            return;
        }

        //接受Characteristic被写的通知,收到蓝牙模块的数据后会触发mOnDataAvailable.onCharacteristicWrite()
        // 参数 enable 就是打开还是关闭， characteristic就是你想让具有通知功能的BluetoothGattCharacteristic
        mBluetoothGatt.setCharacteristicNotification(TxChar,true);
        List<BluetoothGattDescriptor> bl = TxChar.getDescriptors();
        BluetoothGattDescriptor descriptor = bl.get(0);
        //一个descriptor可以规定一个可读的描述，或者一个characteristic变量可接受的范围，或者一个characteristic变量特定的测量单位
        //设置数据内容（将指令放置进特征中）
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        //往蓝牙模块写入数据（开始写数据）
        boolean flag = mBluetoothGatt.writeDescriptor(descriptor);
        //如果返回true  一旦设备那边notify 数据给你，你会在回调里收到onCharacteristicChanged
        //你会在回调里收到onCharacteristicChanged收到设备notify值 （设备上报值），根据 characteristic.getUUID()来判断是谁发送值给你，根据 characteristic.getValue()来获取这个值
    }

    /**
     * 结束和BLE设备的通讯后，需要释放资源
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void close(){
        if (mBluetoothGatt == null){
            return;
        }
        Log.e(TAG,"mBluetoothGatt ------close------");
        mBluetoothDeviceAddress = null;
        mBluetoothGatt.disconnect();
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public boolean connect(String address){
        Log.e(TAG,"connect 运行了");
        if (mBluetoothAdapter == null || address == null){
            Log.e(TAG,"mBluetoothAdapter null  ------or------  address  null");
            return false;
        }
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null){
            mBluetoothGatt.close();
        }
        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
        if (mBluetoothDevice == null){
            Log.e(TAG,"mBluetoothDevice ------null------");
            return false;
        }
        //三个参数，第一个参数是上下文对象，第二个参数是是否自动连接，这里设置为false，第三个参数就是上面的回调方法
        mBluetoothGatt = mBluetoothDevice.connectGatt(this,false,mGattCallBack);//这里设置false为自动连接
        mBluetoothGatt.connect();
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void write(String str){
        Log.e(TAG,"write 运行了");
        if (mBluetoothGatt == null){
            Log.e(TAG,"mBluetoothGatt ------null------");
            broadcastUpate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }

        //这个操作有误，这步操作重新连接，把之前的强制给断开，重新进行连接，导致后面获取为空null
        /*if (!mBluetoothGatt.connect()){
            Log.e(TAG,"mBluetoothGatt ------close------");
            broadcastUpate(ACTION_GATT_DISCONNECTED);
            return;
        }*/

        //有时要做一次啊延迟操作，android蓝牙连接并不是很好
//        TxService = mBluetoothGatt.getService(TX_SERVICE_UUID);
        if (TxService == null){
            Log.e(TAG,"BluetoothGattService ------null------");
            broadcastUpate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }

        //有时要做一次啊延迟操作，android蓝牙连接并不是很好
        BluetoothGattCharacteristic TxChar = TxService.getCharacteristic(TX_CHAR_UUID);//写
        if (TxChar == null){
            Log.e(TAG,"BluetoothGattCharacteristic ------null------");
            broadcastUpate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        byte[] temp = new byte[2];
        int h = str.equals("0xF3") ? 0xf3 : 0xf4;
        if (h == 0xf3){
            temp[0] = (byte) 0xF3;
            temp[1] = (byte) 0xD8;
        }else{
            temp[0] = (byte) 0xF4;
            temp[1] = (byte) 0x9A;
        }
        TxChar.setValue(temp);
        boolean status = mBluetoothGatt.writeCharacteristic(TxChar);//向蓝牙中写入数据（命令）
        if (status) {//第四步
            Log.e(TAG,"writeCharacteristic ------ACTION_DATA_WRITE_SUCCESS------");
            broadcastUpate(ACTION_DATA_WRITE_SUCCESS);
        }else{
            Log.w(TAG,"writeCharacteristic ------ACTION_DATA_WRITE_FAIT------");
            broadcastUpate(ACTION_DATA_WRITE_FAIT);
        }
    }

    /**
     * byte[] to String
     * @param src
     * @return
     */
    private  String bytesToHexString(byte[] src){
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }
}
