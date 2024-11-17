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
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cn.com.kp.ai.R;

public class BluetoothLeUtils {
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothLeScanner bluetoothLeScanner = null;
    private boolean scanning = false;
    private Context context = null;
    private Handler handler = new Handler();

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
            if ("00002a37-0000-1000-8000-00805f9b34fb".equalsIgnoreCase(characteristic.getUuid().toString())) {
                try {
                    if (characteristic == null) {
                        return;
                    }
                    byte[] raw = characteristic.getValue();
                    if (raw == null || raw.length == 0) {
                        return;
                    }

                    int v = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
                    RaiseNotifyUtils.raise("BluetoothLeUtils", new Integer(v).toString() + " bpm");
                } catch (Exception e) {
                    RaiseNotifyUtils.raise("BluetoothLeUtils", e.getMessage());
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value, int status) {
            super.onCharacteristicRead(gatt, characteristic, value, status);

            try {
                StringBuilder content = new StringBuilder();
                // 智能手表电池电量
                if (characteristic.getUuid().toString().equalsIgnoreCase("00002a19-0000-1000-8000-00805f9b34fb")) {
                    content.append("电池电量：" + RaiseNotifyUtils.getDecimalString(value) + "%");
                } else {
                    content.append(new String(value, "UTF-8"));
                }
                RaiseNotifyUtils.raise("BluetoothLeUtils", content.toString());
            } catch (UnsupportedEncodingException e) {
                RaiseNotifyUtils.raise("BluetoothLeUtils", e.getMessage());
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            List<BluetoothGattService> services = gatt.getServices();
            for (BluetoothGattService service : services) {
                List<BluetoothGattCharacteristic> css = service.getCharacteristics();
                servicesMap.put(service.getUuid().toString() , new ArrayList<>());
                if (css != null) {
                    for (BluetoothGattCharacteristic c : css) {
                        if ((c.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0) {
                            servicesMap.get(service.getUuid().toString()).add(c.getUuid().toString());
                        }
                    }
                }
            }

            RaiseNotifyUtils.raise("BluetoothLeUtils", servicesMap);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (BluetoothProfile.STATE_CONNECTED == newState) {
                RaiseNotifyUtils.raise("BluetoothLeUtils", "连接已经建立,(" + status + "===>" + newState + ")");

                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                }

                gatt.discoverServices();
            } else {
                RaiseNotifyUtils.raise("BluetoothLeUtils", "连接状态改变,(" + status + "===>" + newState + ")");
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

                RaiseNotifyUtils.raise("BluetoothLeUtils", sb.toString());

                if (device.getName() != null && device.getName().contains(keywords)) {
                    if (!device.createBond()) {
                        RaiseNotifyUtils.raise("BluetoothLeUtils", "配对失败");
                    } else {
                        matchedDevice = device;

                        scanning = false;
                        bluetoothLeScanner.stopScan(leScanCallback);

                        RaiseNotifyUtils.raise("BluetoothLeUtils", "配对成功");
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
            RaiseNotifyUtils.raise("BluetoothLeUtils", "蓝牙扫描失败");
        }
    };
    private static final long SCAN_PERIOD = 10000;

    public BluetoothLeUtils(Context context) {
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void enableNotifyFromDevice(String service , String ch ,boolean enable) {
        BluetoothGattService s = currentGatt.getService(UUID.fromString(service));

        if (s != null) {
            try {
                BluetoothGattCharacteristic c = s.getCharacteristic(UUID.fromString(ch));

                if (c != null) {
                    try {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        }

                        if (c.getUuid().toString().equalsIgnoreCase("00002a37-0000-1000-8000-00805f9b34fb")){
                            BluetoothGattDescriptor des = c.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                            if (des != null) {
                                if (enable) {
                                    des.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                    currentGatt.setCharacteristicNotification(c, true);
                                } else {
                                    des.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                                    currentGatt.setCharacteristicNotification(c, false);
                                }
                                currentGatt.writeDescriptor(des);
                            }
                        }
                    }catch (Exception e){
                        RaiseNotifyUtils.raise("BluetoothLeUtils", e.getMessage());
                    }
                } else {
                    RaiseNotifyUtils.raise("BluetoothLeUtils", "特征没找到");
                }
            } catch (Exception e) {
                RaiseNotifyUtils.raise("BluetoothLeUtils", e.getMessage());
            }
        } else {
            RaiseNotifyUtils.raise("BluetoothLeUtils", "服务没找到");
        }
    }

    public void readFromDevice(String service , String ch) {
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
                                RaiseNotifyUtils.raise("BluetoothLeUtils", "特征读取失败");
                            }
                        } else {
                            RaiseNotifyUtils.raise("BluetoothLeUtils", "特征不可读");
                        }
                    }catch (Exception e){
                        RaiseNotifyUtils.raise("BluetoothLeUtils", e.getMessage());
                    }
                } else {
                    RaiseNotifyUtils.raise("BluetoothLeUtils", "特征没找到");
                }
            } catch (Exception e) {
                RaiseNotifyUtils.raise("BluetoothLeUtils", e.getMessage());
            }
        } else {
            RaiseNotifyUtils.raise("BluetoothLeUtils", "服务没找到");
        }
    }

    public void createConnection() {
        if (matchedDevice == null) {
            RaiseNotifyUtils.raise("BluetoothLeUtils", "设备还未配对");

            return;
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
        }

        currentGatt = matchedDevice.connectGatt(context, true, mGattCallback, BluetoothDevice.TRANSPORT_LE);
    }

    public void scanLeDevice() {
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            RaiseNotifyUtils.raise("BluetoothLeUtils", "先打开蓝牙");

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
            RaiseNotifyUtils.raise("BluetoothLeUtils", "开始扫描蓝牙设备");
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
