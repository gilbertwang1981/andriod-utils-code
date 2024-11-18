package cn.com.kp.ai.utils;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import cn.com.kp.ai.R;
import cn.com.kp.ai.consts.NotifyMessage;

public class BluetoothLeUtils {
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothLeScanner bluetoothLeScanner = null;
    private boolean scanning = false;
    private Context context = null;
    private Handler handler = new Handler();

    private NotifyUtils notifyUtils = null;

    private BluetoothGatt currentGatt = null;

    private BluetoothDevice matchedDevice = null;

    private Map<String, List<String>> servicesMap = new HashMap<>();

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
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

        @Override
        public void onServiceChanged(BluetoothGatt gatt) {
            super.onServiceChanged(gatt);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyRead(gatt, txPhy, rxPhy, status);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status, byte[] value) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
            super.onCharacteristicChanged(gatt, characteristic, value);

            // 智能手表心率监控
            if (NotifyMessage.UUID_C_HEART_RATE.equalsIgnoreCase(characteristic.getUuid().toString())) {
                try {
                    if (characteristic == null) {
                        return;
                    }
                    byte[] raw = characteristic.getValue();
                    if (raw == null || raw.length == 0) {
                        return;
                    }

                    int v = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
                    Log.i("BluetoothLeUtils", new Integer(v).toString() + " bpm");
                    notifyUtils.updateUIContent(new Integer(v).toString() + " bpm");
                } catch (Exception e) {
                    Log.e("BluetoothLeUtils", e.getMessage());
                }
            } else if (NotifyMessage.UUID_C_WEIGHT.equalsIgnoreCase(characteristic.getUuid().toString())) {
                Log.i("BluetoothLeUtils" , new String(characteristic.getValue()));

                notifyUtils.updateUIContent("重量为：" + new String(characteristic.getValue()) + "公斤");
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value, int status) {
            super.onCharacteristicRead(gatt, characteristic, value, status);

            try {
                StringBuilder content = new StringBuilder();
                if (characteristic.getUuid().toString().equalsIgnoreCase(NotifyMessage.UUID_C_BATTERY)) {
                    content.append("电池电量：" + BluetoothCoderUtils.getDecimalString(value) + "%");
                } else {
                    content.append(new String(value, "UTF-8"));
                }
                notifyUtils.updateUIContent(content.toString());

                Log.i("BluetoothLeUtils", content.toString());
            } catch (UnsupportedEncodingException e) {
                Log.e("BluetoothLeUtils", e.getMessage());
            }
        }

        private void dump(Map<String , List<String>> data) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String , List<String>> entry : data.entrySet()) {
                sb.append("服务：" + entry.getKey() + "\n");
                List<String> charas = entry.getValue();
                for (String chara : charas) {
                    sb.append("特征：" + chara + "\n");
                }
                sb.append("\n");
            }

            Log.i("BluetoothLeUtils" , sb.toString());
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            List<BluetoothGattService> services = gatt.getServices();
            for (BluetoothGattService service : services) {
                List<BluetoothGattCharacteristic> css = service.getCharacteristics();
                servicesMap.put(service.getUuid().toString(), new ArrayList<>());
                if (css != null) {
                    for (BluetoothGattCharacteristic c : css) {
                        if ((c.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0) {
                            servicesMap.get(service.getUuid().toString()).add(c.getUuid().toString());
                        }
                    }
                }
            }

            dump(servicesMap);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (BluetoothProfile.STATE_CONNECTED == newState) {
                Log.i("BluetoothLeUtils", "连接已经建立,(" + status + "===>" + newState + ")");

                notifyUtils.updateUIContent("连接已经建立,(" + status + "===>" + newState + ")");

                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                }

                gatt.discoverServices();
            } else {
                notifyUtils.updateUIContent("连接状态改变,(" + status + "===>" + newState + ")");

                Log.i("BluetoothLeUtils", "连接状态改变,(" + status + "===>" + newState + ")");
            }
        }
    };

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            if (device.getAddress() != null) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                }

                Activity ctx = (Activity) context;
                TextView tv = ctx.findViewById(R.id.btKeywords);
                String keywords = tv.getText().toString();

                StringBuilder sb = new StringBuilder();
                sb.append("设备名：" + device.getName());
                sb.append("\t设备地址：" + device.getAddress());

                Log.i("BluetoothLeUtils", sb.toString());
                notifyUtils.updateUIContent(sb.toString());

                if (device.getName() != null && device.getName().contains(keywords)) {
                    if (!device.createBond()) {
                        Log.e("BluetoothLeUtils", "配对失败");
                    } else {
                        matchedDevice = device;

                        scanning = false;
                        bluetoothLeScanner.stopScan(leScanCallback);

                        notifyUtils.updateUIContent("配对成功");

                        Log.i("BluetoothLeUtils", "配对成功");
                    }
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e("BluetoothLeUtils", "蓝牙扫描失败");
            notifyUtils.updateUIContent("蓝牙扫描失败");
        }
    };
    private static final long SCAN_PERIOD = 10000;

    public BluetoothLeUtils(Context context, Handler uiHandler) {
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.notifyUtils = new NotifyUtils(uiHandler);
    }

    public void enableNotifyFromDevice(String service, String ch, boolean enable) {
        BluetoothGattService s = currentGatt.getService(UUID.fromString(service));

        if (s != null) {
            try {
                BluetoothGattCharacteristic c = s.getCharacteristic(UUID.fromString(ch));

                if (c != null) {
                    try {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        }

                        if (c.getUuid().toString().equalsIgnoreCase(NotifyMessage.UUID_C_HEART_RATE)) {
                            BluetoothGattDescriptor des = c.getDescriptor(UUID.fromString(NotifyMessage.UUID_D_HEART_RATE_NOTIFY));
                            if (des != null) {
                                if (enable) {
                                    des.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                    currentGatt.setCharacteristicNotification(c, true);
                                } else {
                                    des.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                                    currentGatt.setCharacteristicNotification(c, false);
                                }
                                currentGatt.writeDescriptor(des);
                            } else {
                                Log.e("BluetoothLeUtils" ,"找不到描述符");
                            }
                        } else if (c.getUuid().toString().equalsIgnoreCase(NotifyMessage.UUID_C_WEIGHT)) {
                            BluetoothGattDescriptor des = c.getDescriptor(UUID.fromString(NotifyMessage.UUID_D_WEIGHT_NOTIFY));
                            if (des != null) {
                                if (enable) {
                                    des.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                    currentGatt.setCharacteristicNotification(c, true);
                                } else {
                                    des.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                                    currentGatt.setCharacteristicNotification(c, false);
                                }
                                currentGatt.writeDescriptor(des);
                            } else {
                                Log.e("BluetoothLeUtils" ,"找不到描述符");
                            }
                        }
                    } catch (Exception e) {
                        Log.e("BluetoothLeUtils", e.getMessage());
                    }
                } else {
                    notifyUtils.updateUIContent("特征没找到");
                    Log.e("BluetoothLeUtils", "特征没找到");
                }
            } catch (Exception e) {
                Log.e("BluetoothLeUtils", e.getMessage());
            }
        } else {
            Log.e("BluetoothLeUtils", "服务没找到");
        }
    }

    public void readFromDevice(String service, String ch) {
        BluetoothGattService s = currentGatt.getService(UUID.fromString(service));

        if (s != null) {
            try {
                BluetoothGattCharacteristic c = s.getCharacteristic(UUID.fromString(ch));

                if (c != null) {
                    try {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        }

                        if ((c.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0) {
                            if (!currentGatt.readCharacteristic(c)) {
                                Log.i("BluetoothLeUtils", "特征读取失败");
                            }
                        } else {
                            Log.i("BluetoothLeUtils", "特征不可读");
                        }
                    } catch (Exception e) {
                        Log.e("BluetoothLeUtils", e.getMessage());
                    }
                } else {
                    Log.i("BluetoothLeUtils", "特征没找到");
                }
            } catch (Exception e) {
                Log.e("BluetoothLeUtils", e.getMessage());
            }
        } else {
            Log.i("BluetoothLeUtils", "服务没找到");
        }
    }

    public void createConnection() {
        if (matchedDevice == null) {
            Log.e("BluetoothLeUtils", "设备还未配对");

            return;
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
        }

        currentGatt = matchedDevice.connectGatt(context, true, mGattCallback, BluetoothDevice.TRANSPORT_LE);
    }

    public void readDeviceDataFromSocket() throws IOException {
        if (!bluetoothAdapter.isEnabled()) {
            Log.e("BluetoothLeUtils", "请启用蓝牙");

            return;
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                if (deviceName.contains("HC")) {
                    BluetoothSocket socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));

                    Log.i("BluetoothLeUtils", "远程设备：" + device.getAddress());

                    socket.connect();

                    Log.i("BluetoothLeUtils", "设备连接成功");

                    InputStream inputStream = socket.getInputStream();
                    OutputStream outputStream = socket.getOutputStream();

                    String command = "GET_WEIGHT\r\n";
                    outputStream.write(command.getBytes());
                    outputStream.flush();

                    StringBuilder result = new StringBuilder();
                    byte[] buffer = new byte[1024];
                    int length = 0;
                    while ((length = inputStream.read(buffer)) != -1) {
                        result.append(new String(buffer, 0, length));
                        socket.close();
                        inputStream.close();
                        outputStream.close();

                        Log.i("BluetoothLeUtils", new String(buffer, 0, length));
                        notifyUtils.updateUIContent("当前重量为："+ new String(buffer, 0, length) + " KG");
                        return;
                    }

                    break;
                }
            }
        }
    }

    public void scanLeDevice() {
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            Log.e("BluetoothLeUtils", "先打开蓝牙");

            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            }
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            context.startActivity(enableBtIntent);
        }

        ActivityCompat.requestPermissions((Activity) context,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_ADVERTISE,
                        Manifest.permission.BLUETOOTH_PRIVILEGED}, 1);

        if (!scanning) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            }
            Log.i("BluetoothLeUtils", "开始扫描蓝牙设备");
            notifyUtils.updateUIContent("开始扫描蓝牙设备");
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    }
                    scanning = false;
                    bluetoothLeScanner.stopScan(leScanCallback);
                }
            }, SCAN_PERIOD);

            scanning = true;
            bluetoothLeScanner.startScan(leScanCallback);
        } else {
            scanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }
}
