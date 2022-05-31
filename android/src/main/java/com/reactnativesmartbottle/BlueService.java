package com.reactnativesmartbottle;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
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
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import javax.sql.StatementEvent;

/**
 * Created by fff on 2017-3-15.
 */

public class BlueService extends Service {

    public final static String ACTION_GATT_CONNECTED = "com.app.water.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.app.water.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.app.water.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.app.water.ACTION_DATA_AVAILABLE";
    public final static String ACTION_DATA_Notification = "com.app.water.ACTION_DATA_Notification";
    public final static String EXTRA_DATA = "com.app.water.EXTRA_DATA";
    public final static String DEVICE_DOES_NOT_SUPPORT_UART = "com.app.water.DEVICE_DOES_NOT_SUPPORT_UART";
    public final static String DEVICE_WRITE_SUCCESS = "com.app.water.DEVICE_WRITE_SUCCESS";
    public final static String DEVICE_WRITE_FAIL = "com.app.water.DEVICE_WRITE_FAIL";
    public final static String IS_SUCCESS = "IS_SUCCESS";

    public int CONNECT_STATUS = -1;//连接状态 ---  默认断开连接

    public final static int CONNECT = 1;//连接成功
    public final static int DISCONNECT = -1;//断开连接
    public final static int CONNECTING = 0;//连接中

    private BluetoothManager manager;
    private BluetoothAdapter adapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt gatt;
    private BluetoothDevice device;

    public static final UUID RX_SERVICE_UUID = UUID.fromString("0000c470-0000-1000-8000-00805f9b34fb");//服务
    public static final UUID RX_CHAR_UUID = UUID.fromString("0000c471-0000-1000-8000-00805f9b34fb");//notify(通知)
    public static final UUID TX_CHAR_UUID = UUID.fromString("0000c472-0000-1000-8000-00805f9b34fb");//读写

    @SuppressLint("NewApi")
    private BluetoothGattCallback callback = new BluetoothGattCallback() {

        //连接状态的改变
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (status == 133) {//如果是上次连接直接断开
                close();
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {//连接成功
                Log.e(TAG, "连接成功");
                CONNECT_STATUS = CONNECT;
                intentAction = ACTION_GATT_CONNECTED;
                broadcastUpdate(intentAction);
                gatt.discoverServices();//该方法一定要被调用，要不然不会执行onServicesDiscovered方法
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {//连接断开
                CONNECT_STATUS = DISCONNECT;
                Log.e(TAG, "断开服务");
                intentAction = ACTION_GATT_DISCONNECTED;
                broadcastUpdate(intentAction);
                close();//把连接的都断开
            }
        }

        //执行完gatt.discoverServices();就会执行该方法
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.e(TAG, "发现服务");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                CONNECT_STATUS = CONNECTING;
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                enableTXNotification();
            }
        }

        //写操作的回调
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        //不设置 NotificationOfCharacteristic，不会调用下面方法
        //当读取设备时会回调该函数
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "接收到数据");
                broadcastUpdate(ACTION_DATA_AVAILABLE);
            } else {
                Log.e(TAG, "没有接收到数据");
            }
        }

        //设置 NotificationOfCharacteristic会调用下面这个方法
        //获取数据成功，并且必须是注册设备广播后，才会调用次方法
        StringBuffer sb = new StringBuffer();
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            String deviceValue = StrUntils.bytesToHexString(characteristic.getValue());
            sb.append(deviceValue);
            if (sb.toString().startsWith("230B04F1030001")){//获取某天的喝水数据记录
                List recordList = strToList(sb.toString());
                String countByteStr = "" + recordList.get(10) + recordList.get(11);
                int countByte = new BigInteger(countByteStr,16).intValue(); //有效数据字节
                if (countByte == 0){//暂无数据
                    Log.e(TAG, "接收的数据：" + sb);
                    broadcastUpdate(ACTION_DATA_Notification, sb.toString());
                    sb.delete(0,sb.length());
                }else{
                    String vailStr = sb.substring(28,sb.length());
                    List jxList = StrUntils.strToList(vailStr);
                    int calCount = 0;
                    for (int i = 0; i < jxList.size(); i++) {
                        List itemList = (List) jxList.get(i);
                        calCount += itemList.size();
                    }
                    if (countByte == calCount){
                        Log.e(TAG, "接收的数据：" + sb);
                        broadcastUpdate(ACTION_DATA_Notification, sb.toString());
                        sb.delete(0,sb.length());
                    }
                }
            }else{
                Log.e(TAG, "接收的数据：" + deviceValue);
                broadcastUpdate(ACTION_DATA_Notification, deviceValue);
                sb.delete(0,sb.length());
            }
        }
    };

    //发现服务
    @SuppressLint("NewApi")
    public void discoverService() {
        Log.e("TAG", "discoverServices被调用");
        if (gatt != null) {
            gatt.discoverServices();
        }
    }

    //发送有序广播
    public void broadcastUpdate(String aciton) {
        Intent intent = new Intent(aciton);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * 发送有序广播
     *
     * @param action
     * @param characteristic
     */
    @SuppressLint("NewApi")
    private void broadcastUpdate(final String action, final String characteristic) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA, characteristic);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    public class LocalBinder extends Binder {
        public BlueService getService() {
            return BlueService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onUnbind(Intent intent) {
        if (gatt != null) {
            gatt.close();
        }
        return super.onUnbind(intent);
    }

    //初始化判断是否支持蓝牙连接
    @SuppressLint("NewApi")
    public boolean initialize() {
        if (manager == null) {
            manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (manager == null) {
                return false;
            }
        }
        adapter = manager.getAdapter();
        if (adapter == null) {
            return false;
        }
        return true;
    }

    //通过设备的address进行连接设备
    @SuppressLint("NewApi")
    public void connection(String address) {
        Log.e("TAG", "开始连接");
        if (adapter == null || address == null) {
            return;
        }
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && gatt != null) {
            gatt.disconnect();
            gatt.close();
            gatt = null;
        }
        device = adapter.getRemoteDevice(address);
        gatt = device.connectGatt(this, false, callback);//false为自动连接
        mBluetoothDeviceAddress = address;
    }

    //关闭已经连接上的
    @SuppressLint("NewApi")
    public void close() {
        if (mBluetoothDeviceAddress == null) return;
        if (gatt != null) {
            mBluetoothDeviceAddress = null;
            gatt.disconnect();
            gatt.close();
            gatt = null;
        }
    }

    //设置设备广播通知
    @SuppressLint("NewApi")
    public void enableTXNotification() {
        if (gatt == null) {
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }

        BluetoothGattService TxService = gatt.getService(RX_SERVICE_UUID);
        if (TxService == null) {
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        BluetoothGattCharacteristic TxChar = TxService.getCharacteristic(RX_CHAR_UUID);
        if (TxChar == null) {
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }

        gatt.setCharacteristicNotification(TxChar, true);
        List<BluetoothGattDescriptor> bl = TxChar.getDescriptors();
        BluetoothGattDescriptor descriptor = bl.get(0);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(descriptor);
        Log.e("TAG", "通知广播注册成功");
    }

    private void send(byte[] data) {
      if (gatt == null) {
        Toast.makeText(this, "发送失败", Toast.LENGTH_SHORT).show();
        return;
      }
      BluetoothGattService gattService = gatt.getService(RX_SERVICE_UUID);
      if (gattService == null) {
        Toast.makeText(this, "发送失败", Toast.LENGTH_SHORT).show();
        return;
      }

      BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(TX_CHAR_UUID);

      characteristic.setValue(data);
      Log.e(TAG, "指令byte数组：" + StrUntils.bytesToString(data));
      boolean consequence = gatt.writeCharacteristic(characteristic);
      if (consequence) {
        Log.e("TAG", "发送成功");
      } else {
        Log.e("TAG", "发送失败");
      }
    }

    public void handShake() {
      byte[] data = new byte[7];
      data[0] = 0x23;
      data[1] = 0x04;
      data[2] = 0x08;
      data[3] = 0x01;
      data[4] = 0x55;
      data[5] = (byte) 0xAA;
      byte endByteWo = byteCheck(data, 6);
      String hexToWo = String.format("%04x", endByteWo);
      data[6] = hexString2Byte(hexToWo.substring(2, 4));
      send(data);
    }

    public void setTime(String year, int month, int day, int hour, int minute, int dayOfWeek) {
      String yearStr = year.substring(2, year.length());
      String monthStr = month > 10 ? "" + month : "0" + month;
      String dayStr = day > 10 ? "" + day : "0" + day;
      String hourStr = hour > 10 ? "" + hour : "0" + hour;
      String minuteStr = minute > 10 ? "" + minute : "0" + minute;
      String week = "0" + dayOfWeek;
      byte[] data = new byte[12];
      data[0] = (byte) 0x23;
      data[1] = (byte) 0x09;
      data[2] = (byte) 0x01;
      data[3] = (byte) 0x02;
      data[4] = hexString2Byte(yearStr);
      data[5] = hexString2Byte(monthStr);
      data[6] = hexString2Byte(dayStr);
      data[7] = hexString2Byte(hourStr);
      data[8] = hexString2Byte(minuteStr);
      data[9] = 0x00;
      data[10] = hexString2Byte(week);
      byte endByte = byteCheck(data, 11);
      String hexTo = String.format("%04x", endByte);
      data[11] = hexString2Byte(hexTo.substring(2, 4));
      send(data);
    }

    public void getTime() {
      byte[] data = new byte[5];
      data[0] = (byte) 0x23;
      data[1] = (byte) 0x02;
      data[2] = (byte) 0x02;
      data[3] = (byte) 0x02;
      data[4] = (byte) 0x27;
      send(data);
    }

    public void getBattery() {
      byte[] data = new byte[5];
      data[0] = (byte) 0x23;
      data[1] = (byte) 0x02;
      data[2] = (byte) 0x02;
      data[3] = (byte) 0x03;
      data[4] = (byte) 0x26;
      send(data);
    }

    public void setIntakeGoal(int drinkWater) {
      String hexDrink = String.format("%04X", drinkWater);
      byte[] data = new byte[7];
      data[0] = (byte) 0x23;
      data[1] = (byte) 0x04;
      data[2] = (byte) 0x01;
      data[3] = (byte) 0x04;
      data[4] = hexString2Byte(hexDrink.substring(0, 2));
      data[5] = hexString2Byte(hexDrink.substring(2, hexDrink.length()));
      byte endSetByte = byteCheck(data, 6);
      String hexToSet = String.format("%04x", endSetByte);
      data[6] = hexString2Byte(hexToSet.substring(2, 4));
      send(data);
    }

    public void getIntakeGoal() {
      byte[] data = new byte[5];
      data[0] = (byte) 0x23;
      data[1] = (byte) 0x02;
      data[2] = (byte) 0x02;
      data[3] = (byte) 0x04;
      data[4] = (byte) 0x2d;
      send(data);
    }

    public void getCurrentIntake() {
      byte[] data = new byte[5];
      data[0] = (byte) 0x23;
      data[1] = (byte) 0x02;
      data[2] = (byte) 0x02;
      data[3] = (byte) 0x05;
      data[4] = (byte) 0x2c;
      send(data);
    }

    public void getWaterDirectory(String year, int month, int day) {
      String yearStrNow = year.substring(2, year.length());
      String monthStrNow = month > 10 ? "" + month : "0" + month;
      String dayStrNow = day > 10 ? "" + day : "0" + day;

      byte[] data = new byte[11];
      data[0] = (byte) 0x23;
      data[1] = (byte) 0x08;
      data[2] = (byte) 0x02;
      data[3] = (byte) 0xF1;
      data[4] = (byte) 0x03;
      data[5] = (byte) 0x00;
      data[6] = (byte) 0x01;
      data[7] = hexString2Byte(yearStrNow);
      data[8] = hexString2Byte(monthStrNow);
      data[9] = hexString2Byte(dayStrNow);
      byte endSetByteCord = byteCheck(data, 10);
      String hexToCord = String.format("%04x", endSetByteCord);
      data[10] = hexString2Byte(hexToCord.substring(2, 4));
      send(data);
    }

    public void deleteWaterDirectory(String year, int month, int day) {
      String yearStrDel = year.substring(2, year.length());
      String monthStrDel = month > 10 ? "" + month : "0" + month;
      String dayStrDel = day > 10 ? "" + day : "0" + day;

      byte[] data = new byte[11];
      data[0] = (byte) 0x23;
      data[1] = (byte) 0x08;
      data[2] = (byte) 0x08;
      data[3] = (byte) 0xF1;
      data[4] = (byte) 0x04;
      data[5] = (byte) 0x00;
      data[6] = (byte) 0x01;
      data[7] = hexString2Byte(yearStrDel);
      data[8] = hexString2Byte(monthStrDel);
      data[9] = hexString2Byte(dayStrDel);
      byte endByteDel = byteCheck(data, 10);
      String hexToDel = String.format("%04x", endByteDel);
      data[10] = hexString2Byte(hexToDel.substring(2, 4));
      send(data);
    }

    public void setUserInformation(String name, String sex, int age) {

      byte[] byteName = name.getBytes();
      int cmdSize = 7 + byteName.length;
      byte[] data = new byte[cmdSize];
      data[0] = (byte) 0x23;
      data[1] = hexString2Byte(String.format("%04X", name.length() + 4));
      data[2] = (byte) 0x04;
      data[3] = (byte) 0x0A;
      data[4] = sex.equalsIgnoreCase("Male") ? (byte) 0x01 : (byte) 0x00; //01 Male  00 Female
      data[5] = hexString2Byte(String.format("%04X", age)); //年龄
      for (int i = 0; i < byteName.length; i++) {
        data[6 + i] = byteName[i];
      }
      for (int i = 0; i < data.length - 1; i++) {
        data[cmdSize - 1] += (data[i] ^ i);
      }
      data[cmdSize - 1] = (byte) (data[cmdSize - 1] & 0xFF);
      send(data);
    }

    public void getUserInformation() {
      byte[] data = new byte[5];
      data[0] = (byte) 0x23;
      data[1] = (byte) 0x02;
      data[2] = (byte) 0x02;
      data[3] = (byte) 0x0A;
      data[4] = (byte) 0x2F;
      send(data);
    }

    /**
     * 十六进制转字节
     * @param src
     * @return
     */
    public byte hexString2Byte(String src) {
        int l = src.length() / 2;
        byte[] ret = new byte[l];
        for (int i = 0; i < l; i++) {
            ret[i] = (byte) Integer
                    .valueOf(src.substring(i * 2, i * 2 + 2), 16).byteValue();
        }
        if(ret.length > 1) {
           return ret[1];
        } else {
            return ret[0];
        }
    }

    /**
     * 求校验
     * @param bytes
     * @return
     */
    public byte byteCheck(byte[] bytes,int size){
        byte count = 0;
        for (int i = 0; i < size; i++) {
            count += (bytes[i]^i);
        }
        count = (byte) (count & 0xFF);
        return count;
    }

    private List<String> strToList(String dataValue){
        List dataList = new ArrayList<ArrayList<String>>();
        int itemJinanGe = 0;
        do {
            dataList.add(dataValue.substring(itemJinanGe,itemJinanGe + 2));
            itemJinanGe += 2;
        }while (itemJinanGe < dataValue.length());
        return dataList;
    }

}
