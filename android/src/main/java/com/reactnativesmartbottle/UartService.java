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
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class UartService extends Service {

    private final static String TAG = UartService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothDevice device;

    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED = "com.hg.skinanalyze.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.hg.skinanalyze.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.hg.skinanalyze.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.hg.skinanalyze.ACTION_DATA_AVAILABLE";

    public final static String ACTION_DATA_Notification = "com.hg.skinanalyze.ACTION_DATA_Notification";
    public final static String ACTION_DATA_WRITE_SUCCESS = "com.hg.skinanalyze.ACTION_DATA_WRITE_SUCCESS";
    public final static String ACTION_DATA_WRITE_FAIT = "com.hg.skinanalyze.ACTION_DATA_WRITE_FAIT";
    public final static String EXTRA_DATA = "com.hg.skinanalyze.EXTRA_DATA";
    public final static String DEVICE_DOES_NOT_SUPPORT_UART = "com.hg.skinanalyze.DEVICE_DOES_NOT_SUPPORT_UART";
    public final static String IS_SUCCESS = "IS_SUCCESS";

    public static final UUID TX_SERVICE_UUID = UUID.fromString("e0340900-01ce-4518-8f11-3b840905b4fe");
    public static final UUID RX_CHAR_UUID = UUID.fromString("e0341bc0-01ce-4518-8f11-3b840905b4fe");//read notify indicate(读，通知，..)
    public static final UUID TX_CHAR_UUID = UUID.fromString("e0340ab1-01ce-4518-8f11-3b840905b4fe");


    /**
     * 连接状态改变
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "mBluetoothGatt = " + mBluetoothGatt);
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            Log.e("maxbeauty", "xxxxxxxssssssssssssssssss");
            if (status == 133) {
                if (mBluetoothGatt != null)
                    close();
//                    mBluetoothGatt.close();
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.e("maxbeauty", "xxxxxxxxxxxxxxxxxxxxxxxxx");
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
                if (mBluetoothGatt != null)
                    mBluetoothGatt.disconnect();
                mBluetoothGatt = null;
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
//            dnf.datanotification(characteristic);
            Log.e("蓝牙测试读取", "结果是：" + characteristic.getValue().toString());
            broadcastUpdate(ACTION_DATA_Notification, characteristic);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            // TODO Auto-generated method stub
            super.onReadRemoteRssi(gatt, rssi, status);
        }

    };


    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /*private void broadcastUpdate(final String action, boolean boo) {
        final Intent intent = new Intent(action);
        intent.putExtra(IS_SUCCESS, boo);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }*/

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml

        if (RX_CHAR_UUID.equals(characteristic.getUuid())) {
            byte[] data = characteristic.getValue();
            //Log.d(TAG, String.format("Received TX: %d",data));
            intent.putExtra(EXTRA_DATA, data);
        } else {

        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

//    private DataNotificationLinener dnf;
//
//    public void setDataNotification(DataNotificationLinener dnf) {
//        this.dnf = dnf;
//    }

    //server
    public class LocalBinder extends Binder {
        public UartService getService() {
            return UartService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }


    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            mBluetoothGatt.close();
        }

        device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        mBluetoothGatt.connect();
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        Log.w(TAG, "mBluetoothGatt closed");
        mBluetoothDeviceAddress = null;
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void write(String str) {
        if (mBluetoothGatt == null) {
            showMessage("mBluetoothGatt null" + mBluetoothGatt);
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        if (!mBluetoothGatt.connect()) {
            showMessage("mBluetoothGatt Connect break" + mBluetoothGatt);
            broadcastUpdate(ACTION_GATT_DISCONNECTED);
        }
        BluetoothGattService TxService = mBluetoothGatt.getService(TX_SERVICE_UUID);
        if (TxService == null) {
            showMessage("Tx GattService not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        BluetoothGattCharacteristic TxChar = TxService.getCharacteristic(TX_CHAR_UUID);
        if (TxChar == null) {
            showMessage("Tx charateristic not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        byte[] temp = new byte[2];
        int h = str.equals("0xF3") ? 0xf3 : 0xf4;
        if (h == 0xf3) {
            temp[0] = (byte) 0xF3;
            temp[1] = (byte) 0xD8;
        } else {
            temp[0] = (byte) 0xF4;
            temp[1] = (byte) 0x9A;
        }
        TxChar.setValue(temp);
        boolean status = mBluetoothGatt.writeCharacteristic(TxChar);
        Log.e("蓝牙写入", "结果是：" + status);
        if (status) {
            broadcastUpdate(ACTION_DATA_WRITE_SUCCESS);
        } else {
            broadcastUpdate(ACTION_DATA_WRITE_FAIT);
        }
    }

    /**
     * Enable TXNotification
     *
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void enableTXNotification() {

        if (mBluetoothGatt == null) {
            showMessage("mBluetoothGatt null" + mBluetoothGatt);
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }

        BluetoothGattService TxService = mBluetoothGatt.getService(TX_SERVICE_UUID);
        if (TxService == null) {
            showMessage("Tx GattService not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        BluetoothGattCharacteristic TxChar = TxService.getCharacteristic(RX_CHAR_UUID);
        if (TxChar == null) {
            showMessage("Tx charateristic not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }

        mBluetoothGatt.setCharacteristicNotification(TxChar, true);
        List<BluetoothGattDescriptor> bl = TxChar.getDescriptors();/*��֪��Ϊ�Σ�getDescriptor(TX_CHAR_UUID)����Ϊnull*/
        BluetoothGattDescriptor descriptor = bl.get(0);//descriptor = TxChar.getDescriptor(TX_CHAR_UUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);


    }

    private void showMessage(String msg) {
        Log.e(TAG, msg);
    }
}
