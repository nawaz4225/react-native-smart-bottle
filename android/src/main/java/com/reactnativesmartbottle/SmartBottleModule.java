package com.reactnativesmartbottle;

import static com.facebook.react.bridge.UiThreadUtil.runOnUiThread;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.functions.Consumer;

@ReactModule(name = SmartBottleModule.NAME)
public class SmartBottleModule extends ReactContextBaseJavaModule {
  public static final String NAME = "SmartBottle";
  private static final String TAG = "SmartBottle";

  private BlueService service;
  private BluetoothAdapter blueAdapter;

  public SmartBottleModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    @NonNull
    public String getName() {
        return NAME;
    }

    @ReactMethod
    public void requestRequiredPermissions() {
    WritableMap map = Arguments.createMap();
      map.putString("value", "Requesting Permissions");
      sendEvent("Event", map);
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        new RxPermissions(getCurrentActivity())
          .request(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.LOCATION_HARDWARE, Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_SETTINGS, Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_CONTACTS
          ).subscribe(new Consumer<Boolean>() {
          @Override
          public void accept(Boolean aBoolean) throws Exception {
            if (!aBoolean) {
//                    showDialogTipUserGoToAppSettting();
            } else {
              initializeBLE();
            }
          }

        });
      }
    });

    }

  //==============????????????????????????===============
  public static IntentFilter makeGattUpdateIntentFilter() {
    final IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(BlueService.ACTION_GATT_CONNECTED);
    intentFilter.addAction(BlueService.ACTION_GATT_DISCONNECTED);
    intentFilter.addAction(BlueService.ACTION_GATT_SERVICES_DISCOVERED);
    intentFilter.addAction(BlueService.ACTION_DATA_Notification);
    intentFilter.addAction(BlueService.ACTION_DATA_AVAILABLE);
    intentFilter.addAction(BlueService.DEVICE_DOES_NOT_SUPPORT_UART);
    intentFilter.addAction(BlueService.IS_SUCCESS);
    return intentFilter;
  }

    @ReactMethod
    public void initializeBLE() {
      /**????????????*/
      LocalBroadcastManager.getInstance(getCurrentActivity().getApplicationContext()).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());

      //??????BluetoothAdapter
      BluetoothManager manager = (BluetoothManager) getCurrentActivity().getSystemService(Context.BLUETOOTH_SERVICE);
      blueAdapter = manager.getAdapter();
      //??????????????????
      if (blueAdapter != null && !blueAdapter.isEnabled()) {
        blueAdapter.enable();
      }
      //????????????BlueService
      Intent intent = new Intent(getCurrentActivity(), BlueService.class);
      getCurrentActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }
  /**
   * ????????????
   */
  private BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      WritableMap map = Arguments.createMap();
      /*GATT ????????????*/
      if (action.equals(BlueService.ACTION_GATT_CONNECTED)) {
//        tvStatus.setText("ble????????????");
        map.putString("value", "ble connection is successful");
        sendEvent("Event", map);
      } else if (action.equals(BlueService.ACTION_GATT_DISCONNECTED)) {//?????????????????? /*GATT ????????????*/
//        tvStatus.setText("ble????????????");
        map.putString("value", "ble connection disconnected");
        sendEvent("Event", map);
      } else if (action.equals(BlueService.ACTION_GATT_SERVICES_DISCOVERED)) {/*GATT ????????????*/
//        tvStatus.setText("gatt????????????");
        map.putString("value", "gatt discovery service");
        sendEvent("Event", map);
      } else if (action.equals(BlueService.ACTION_DATA_Notification)) {  //?????????????????????
        /*GATT ???????????????????????????????????????????????????*/
        String deviceValue = intent.getStringExtra(BlueService.EXTRA_DATA);
        if (deviceValue.length() == 14 && deviceValue.startsWith("23041003000043")){
          Log.e(TAG, "??????????????????");
//          tvResult.setText("??????????????????");
          map.putString("value", "The device is connected successfully");
          sendEvent("Event", map);
        }else if (deviceValue.length() == 16 && deviceValue.startsWith("230504010100003F")){
          Log.e(TAG, "????????????");
//          tvResult.setText("????????????");
          map.putString("value", "handshake success");
          sendEvent("Event", map);
        }else if (deviceValue.length() == 16 && deviceValue.startsWith("2305040102")){
          List timeList = strToList(deviceValue);
          if (timeList.get(4).equals("02") && timeList.get(5).equals("00")){
            Log.e(TAG, "??????????????????");
//            tvResult.setText("??????????????????");
            map.putString("value", "Set time successfully");
            sendEvent("Event", map);
          }else{
            Log.e(TAG, "??????????????????");
//            tvResult.setText("??????????????????");
            map.putString("value", "Failed to set time");
            sendEvent("Event", map);
          }
        }else if (deviceValue.length() == 22 && deviceValue.startsWith("23080402")){
          List timeList = strToList(deviceValue);
          String year = (String) timeList.get(4);
          String month = (String) timeList.get(5);
          String day = (String) timeList.get(6);
          String hour = (String) timeList.get(7);
          String minute = (String) timeList.get(8);
          String second = (String) timeList.get(9);
          Log.e(TAG, "??????????????????");
//          tvResult.setText(String.format("??????????????????:%s-%s-%s %s:%s:%s",year,month,day,hour,minute,second));
          map.putString("value", String.format("get time successfully:%s-%s-%s %s:%s:%s",year,month,day,hour,minute,second));
          sendEvent("Event", map);
        }else if (deviceValue.length() == 14 && deviceValue.startsWith("23040403")){
          List infoList = strToList(deviceValue);
          StringBuffer sb = new StringBuffer();
          String powerStr = (String) infoList.get(4);//??????
          String status = (String) infoList.get(5);//????????????
          int power = new BigInteger(powerStr,16).intValue();
          sb.append(power + "%");
          if (status.equals("00")){
            sb.append("not charged");
          }else if (status.equals("01")){
            sb.append("Charging");
          }else{
            sb.append("full");
          }
          Log.e(TAG, "??????????????????");
//          tvResult.setText("??????????????????:" + sb);
          map.putString("value", "The battery information is successful:" + sb);
          sendEvent("Event", map);
        }else if (deviceValue.length() == 16 && deviceValue.startsWith("2305040104")){
          List timeList = strToList(deviceValue);
          if (timeList.get(5).equals("00") && timeList.get(6).equals("00")){
            Log.e(TAG, "????????????????????????");
//            tvResult.setText("????????????????????????");
            map.putString("value", "Set a water drinking goal success");
            sendEvent("Event", map);
          }else{
            Log.e(TAG, "????????????????????????");
//            tvResult.setText("????????????????????????");
            map.putString("value", "Failed to set drinking water goal");
            sendEvent("Event", map);
          }
        }else if (deviceValue.length() == 14 && deviceValue.startsWith("23040404")){
          List timeList = strToList(deviceValue);
          String hexDrink = "" + timeList.get(4) + timeList.get(5);
          int drinkWater = new BigInteger(hexDrink,16).intValue();
          Log.e(TAG, "????????????????????????");
//          tvResult.setText("????????????????????????:" + drinkWater);
          map.putString("value", "Acquire water goal success: " + drinkWater);
          sendEvent("Event", map);
        }else if (deviceValue.length() == 18 && deviceValue.startsWith("23060405")){
          List timeList = strToList(deviceValue);
          String hexCountDrink = "" + timeList.get(4) + timeList.get(5);
          String hexEndDrink = "" + timeList.get(6) + timeList.get(7);
          int drinkCountWater = new BigInteger(hexCountDrink,16).intValue();
          int drinkEndWater = new BigInteger(hexEndDrink,16).intValue();
          Log.e(TAG, "???????????????????????????");
//          tvResult.setText("???????????????:" + drinkCountWater + "  ??????????????????????????????" + drinkEndWater);
          map.putString("value", "water intake for the day:" + drinkCountWater + "  Last water intake of the day???" + drinkEndWater);
          sendEvent("Event", map);
        }else if (deviceValue.length() >= 14 && deviceValue.startsWith("230B04F1030001")){
          Log.e(TAG, "????????????????????????");
          List recordList = strToList(deviceValue);
          String status = (String) recordList.get(12);
          if (status.equals("00")){
            Log.e(TAG, "????????????????????????");
            if (deviceValue.length() == 28){
//              tvResult.setText("????????????????????????:????????????");
              map.putString("value", "Successfully obtained the data of the day: no data yet");
              sendEvent("Event", map);
              return;
            }
            String vailStr = deviceValue.substring(28,deviceValue.length());
            List jxList = StrUntils.strToList(vailStr);
            List endList = new ArrayList<ArrayList<String>>();
            for (int i = 0; i < jxList.size(); i++) {
              List newItemList = new ArrayList<String>();
              List itemList = (List) jxList.get(i);
              String hexHourStr = (String) itemList.get(0);
              String hexMinuteStr = (String) itemList.get(1);
              String hexDrinkStr = "" + itemList.get(2) + itemList.get(3);

              int hour = new BigInteger(hexHourStr,16).intValue();
              int minute = new BigInteger(hexMinuteStr,16).intValue();
              newItemList.add(hour + ":" + minute);

              int drinkWater = new BigInteger(hexDrinkStr,16).intValue();
              newItemList.add("" + drinkWater);

              endList.add(newItemList);
            }
            Log.e(TAG, "16to10??????:" + endList);
//            tvResult.setText("????????????????????????:" + endList.toString());
            map.putString("value", "Get the data of the day successfully:" + endList.toString());
            sendEvent("Event", map);
          }else if (status.equals("01")){
            Log.e(TAG, "????????????????????????(??????????????????)");
//            tvResult.setText("????????????????????????(??????????????????)");
            map.putString("value", "Failed to get current day data (invalid data type)");
            sendEvent("Event", map);
          }else if (status.equals("02")){
            Log.e(TAG, "????????????????????????(??????????????????)");
//            tvResult.setText("????????????????????????(??????????????????)");
            map.putString("value", "Failed to get the data of the current day (invalid record serial number)");
            sendEvent("Event", map);
          }else if (status.equals("03")){
            Log.e(TAG, "????????????????????????(?????????UTC)");
//            tvResult.setText("????????????????????????(?????????UTC)");
            map.putString("value", "Failed to get current day data (invalid UTC)");
            sendEvent("Event", map);
          }else{
            Log.e(TAG, "????????????????????????(???)");
//            tvResult.setText("????????????????????????(???)");
            map.putString("value", "Failed to get the data of the current day (none)");
            sendEvent("Event", map);
          }
        }else if (deviceValue.length() == 24 && deviceValue.startsWith("230910F1040001")){
          List timeList = strToList(deviceValue);
          String year = (String) timeList.get(7);
          String month = (String) timeList.get(8);
          String day = (String) timeList.get(9);
          String delStatus = (String) timeList.get(10);
          if (delStatus.equals("00")){
            Log.e(TAG, "????????????????????????");
//            tvResult.setText(String.format("????????????????????????:%s-%s-%s",year,month,day));
            map.putString("value", String.format("Successfully deleted data on a certain day:%s-%s-%s",year,month,day));
            sendEvent("Event", map);
          }else{
            Log.e(TAG, "????????????????????????");
//            tvResult.setText(String.format("????????????????????????:%s-%s-%s",year,month,day));
            map.putString("value", String.format("Failed to delete data on a certain day:%s-%s-%s",year,month,day));
            sendEvent("Event", map);
          }
        }else if (deviceValue.length() == 16 && deviceValue.startsWith("230504010A00")){//230504010A000048
          List recordList = strToList(deviceValue);
          String status = (String) recordList.get(6);
          if (status.equals("00")){
            Log.e(TAG, "????????????????????????");
//            tvResult.setText("????????????????????????");
            map.putString("value", "Set personal information successfully");
            sendEvent("Event", map);
          }else{
            Log.e(TAG, "????????????????????????");
//            tvResult.setText("????????????????????????");
            map.putString("value", "Failed to set personal information");
            sendEvent("Event", map);
          }
        }else if (deviceValue.startsWith("230B040A")){//2308040A011E4A6F696ED7
          //08?????????????????????????????????+4 ????????????  ???????????????????????????
          Log.e(TAG, "????????????????????????");
          List recordList = strToList(deviceValue);
          String sexByte = (String) recordList.get(4);//??????
          String ageByte = (String) recordList.get(5);//??????
          StringBuffer sb = new StringBuffer();
          for (int i = 6; i < recordList.size() - 1; i++) {
            String singleStr = (String) recordList.get(i);
            sb.append(singleStr);
          }
          String sex = sexByte.equals("01") ? "Male" : "Female";
          int age = new BigInteger(ageByte,16).intValue();
          String name = hexStringToString(sb.toString());
//          tvResult.setText(String.format("?????????%s  ?????????%s  ?????????%s",name,sex,age));
          map.putString("value", String.format("Name???%s  Sex???%s  Age???%s",name,sex,age));
          sendEvent("Event", map);
        }else if (deviceValue.equals("2302020A2F")){//??????
          //Modify the pairing process (long press to display BLUETOOTH, the slave gets the username (02 0A), and after the host returns the username,
          // it will initiate the pairing process)
          Log.e(TAG, "??????????????????");
//          tvResult.setText("??????????????????");
          map.putString("value", "The board sends out a pairing");
          sendEvent("Event", map);
        }

      }
    }
  };

  /**
   * 16??????????????????string???????????????
   * @param s
   * @return
   */
  public String hexStringToString(String s) {
    if (s == null || s.equals("")) {
      return null;
    }
    s = s.replace(" ", "");
    byte[] baKeyword = new byte[s.length() / 2];
    for (int i = 0; i < baKeyword.length; i++) {
      try {
        baKeyword[i] = (byte) (0xff & Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    try {
      s = new String(baKeyword, "UTF-8");
      new String();
    } catch (Exception e1) {
      e1.printStackTrace();
    }
    return s;
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

  private void showTip(final String tip) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(getReactApplicationContext(), tip, Toast.LENGTH_SHORT).show();
      }
    });
  }

  private Timer timer = new Timer();

  private TimerTask task = new TimerTask() {
    @Override
    public void run() {
      Toast.makeText(getReactApplicationContext(), "?????????????????????", Toast.LENGTH_SHORT).show();
    }
  };

  private void closeTip() {
    timer.schedule(task, 4000);
  }

  //????????????????????????
  private ServiceConnection connection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
      service = ((BlueService.LocalBinder) iBinder).getService();
      if (!service.initialize()) {
//        finish();
      }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
      service = null;
    }
  };

  //??????????????????
  @ReactMethod
  private void Scanbluetooth() {
    if (service != null && blueAdapter != null) {
      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {//21??????????????????
        blueAdapter.getBluetoothLeScanner().startScan(scancallback);
      } else {//21??????????????????
        blueAdapter.startLeScan(lescancallback);
      }
    }
  }

  private ScanCallback scancallback = new ScanCallback() {
    @Override
    public void onScanResult(int callbackType, final ScanResult result) {
      super.onScanResult(callbackType, result);
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          String name = result.getDevice().getName();
          Log.e(TAG, "????????????: " + name);
          if (!TextUtils.isEmpty(name) && name.equals("GW-A26B")) {
            String address = result.getDevice().getAddress();
            if (!TextUtils.isEmpty(address)) {
              blueAdapter.getBluetoothLeScanner().stopScan(scancallback);
              service.connection(address);
            }
          }
        }
      });
    }
  };

  private BluetoothAdapter.LeScanCallback lescancallback = new BluetoothAdapter.LeScanCallback() {
    @Override
    public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
      String name = bluetoothDevice.getName();
      Log.e(TAG, "????????????: " + name);
      if (!TextUtils.isEmpty(name) && name.equals("GW-A26B")) {
        String address = bluetoothDevice.getAddress();
        if (!TextUtils.isEmpty(address)) {
          blueAdapter.stopLeScan(this);
          service.connection(address);
        }
      }
    }
  };

    @ReactMethod
    public void handShake() {
      if (service != null) {
        service.handShake();
      }
    }

  @ReactMethod
  public void setTime(String year, int month, int day, int hour, int minute, int dayOfWeek) {
    if (service != null) {
      service.setTime(year, month, day, hour, minute, dayOfWeek);
    }
  }

  @ReactMethod
  public void getTime() {
    if (service != null) {
      service.getTime();
    }
  }

  @ReactMethod
  public void getBattery() {
    if (service != null) {
      service.getBattery();
    }
  }

  @ReactMethod
  public void setIntakeGoal(int drinkWater) {
    if (service != null) {
      service.setIntakeGoal(drinkWater);
    }
  }

  @ReactMethod
  public void getIntakeGoal() {
    if (service != null) {
      service.getIntakeGoal();
    }
  }

  @ReactMethod
  public void getCurrentIntake() {
    if (service != null) {
      service.getCurrentIntake();
    }
  }

  @ReactMethod
  public void getWaterDirectory(String year, int month, int day) {
    if (service != null) {
      service.getWaterDirectory(year, month, day);
    }
  }

  @ReactMethod
  public void deleteWaterDirectory(String year, int month, int day) {
    if (service != null) {
      service.deleteWaterDirectory(year, month, day);
    }
  }

  @ReactMethod
  public void setUserInformation(String name, String sex, int age) {
    if (service != null) {
      service.setUserInformation(name, sex, age);
    }
  }

  @ReactMethod
  public void getUserInformation() {
    if (service != null) {
      service.getUserInformation();
    }
  }



    private void sendEvent(String eventName,
                         @Nullable WritableMap params) {
    getReactApplicationContext()
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
      .emit(eventName, params);
  }

}
