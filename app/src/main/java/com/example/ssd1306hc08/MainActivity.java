package com.example.ssd1306hc08;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.example.ssd1306hc08.R;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = null;
    private Thread thread;
    private BluetoothGatt mBluetoothGatt;
    private boolean plotData = true;
    static final UUID mUUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    private static final UUID BLUETOOTH_LE_CC254X_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID BLUETOOTH_LE_CC254X_CHAR_RW = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    private static final UUID readorwrite = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID readorwrite2 = UUID.fromString("00002901-0000-1000-8000-00805f9b34fb");
    private BluetoothGattCharacteristic writeCharacteristic;
    boolean flag = false;
    private static final UUID BLUETOOTH_LE_NRF_SERVICE    = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    private LeDeviceListAdapter mLeDeviceListAdapter;
    ArrayList<Integer> intArray = new ArrayList<>();
    int currentXPos = 0;
    private Button openBitmapView;
    private PaintView paintView;

    byte screenBuffer[] = new byte[128 * 64 / 8];

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_2);


        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = bluetoothManager.getAdapter();


        BluetoothDevice hc08 = btAdapter.getRemoteDevice("F8:30:02:27:FE:EC");
        System.out.println(hc08.getName());

        BluetoothSocket btSocket = null;
        BluetoothServerSocket btSocketServer = null;
        BluetoothGatt btGatt = null;

        connectToDevice(hc08);

        //List<BluetoothGattCharacteristic> btchr = mBluetoothGatt.getService(BLUETOOTH_LE_CC254X_SERVICE).getCharacteristics();
        BluetoothGattCharacteristic read = mBluetoothGatt.getService(BLUETOOTH_LE_CC254X_SERVICE).getCharacteristic(BLUETOOTH_LE_CC254X_CHAR_RW);
        mBluetoothGatt.setCharacteristicNotification(read, true);

        BluetoothGattDescriptor descriptor = read.getDescriptor(
                readorwrite);

        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        int permissions = read.getPermissions();
        if (permissions == BluetoothGattDescriptor.PERMISSION_READ){
            int i = 0;
            i++;
        }
        mBluetoothGatt.writeDescriptor(descriptor);
        mBluetoothGatt.readDescriptor(descriptor);

        writeCharacteristic = read;//mBluetoothGatt.getService(BLUETOOTH_LE_CC254X_SERVICE).getCharacteristic(BLUETOOTH_LE_CC254X_CHAR_RW);

        openBitmapView = findViewById(R.id.sendBitmap);
        openBitmapView.setOnClickListener(v -> {
            try {
                onClick(v);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        paintView = findViewById(R.id.paint);
        DisplayMetrics metrics = new DisplayMetrics();

        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int height = metrics.heightPixels;
        int width = metrics.widthPixels;
        paintView.init(800, width);
    }



    public void onClick(View v) throws InterruptedException {
        if (v.getId() == R.id.sendBitmap) {
            Bitmap current = paintView.getmBitmap();

            Bitmap resized = this.getResizedBitmap(current, 128,64);

                /*int size = current.getRowBytes() * current.getHeight();
                ByteBuffer byteBuffer = ByteBuffer.allocate(size);
                current.copyPixelsToBuffer(byteBuffer);

                byte[] byteArray = new byte[128 * 64];
                byteArray = byteBuffer.array();*/

            int width = current.getWidth();
            int height = current.getHeight();
;
            int[] pixels = new int[128 * 64];
            resized.getPixels(pixels, 0, 128, 0, 0, 128, 64);

            int i = 0;

            for (int y = 0; y < 64; y++) {
                for (int x = 0; x < 128; x++) {

                    if(pixels[i] < -1){
                        ssd1306_InsertPixelInBuffer(x,y);
                    }
                    i++;
                }
            }

            byte [] sendBytes1 = {0x12,          0x42,            0x04};
            send(sendBytes1);

//            for (int j = 0; j < 1024; j+=8) {
//                byte[] sendBytes = {screenBuffer[j],
//                        screenBuffer[j + 1],
//                        screenBuffer[j + 2],
//                        screenBuffer[j + 3],
//                        screenBuffer[j + 4],
//                        screenBuffer[j + 5],
//                        screenBuffer[j + 6],
//                        screenBuffer[j + 7]};
//
//
//                send(sendBytes);
//            }




        }

    }

    private void ssd1306_InsertPixelInBuffer(int x, int y){

        if(x >= 128 || y >= 64) {
            return;
        }

        //As SSD1306 takes byte as input to RAM, we need to shift any other pixels around
        //You cannot access the memory in SSD1306 by pixel only so screen buffer is needed
        screenBuffer[x + (y / 8) * 128] |= 1 << (y % 8);

    }

    private void send(byte [] bytes) {



            writeCharacteristic.setValue(bytes);
            mBluetoothGatt.writeCharacteristic(writeCharacteristic);

    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    public int unsignedByteToInt(byte b) {
        return b & 0xFF;
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, "Name: " + device.getName() + " (" + device.getAddress() + ")");
                            String deviceAddress = device.getAddress();
                            if (deviceAddress.equals("F8:30:02:27:FE:EC")) {
                                connectToDevice(device);
                            }
                        }
                    });
                }
            };

    public void connectToDevice(BluetoothDevice device) {
        if (mBluetoothGatt == null) {

            for (int i = 0; i < 5; i++) {
                try {
                    Log.i(TAG, "Attempting to connect to device " + device.getName() + " (" + device.getAddress() + ")");
                    mBluetoothGatt = device.connectGatt(this, true, gattCallback);
                    //scanLeDevice(false);// will stop after first device detection
                } catch (NullPointerException e) {
                    System.out.println("Null error");
                }
            }
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i(TAG, "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i(TAG, "STATE_CONNECTED");
                    //BluetoothDevice device = gatt.getDevice(); // Get device
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e(TAG, "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e(TAG, "STATE_OTHER");
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
            BluetoothGattCharacteristic characteristic =
                    gatt.getService(BLUETOOTH_LE_CC254X_SERVICE)
                            .getCharacteristic(BLUETOOTH_LE_CC254X_CHAR_RW);
            //characteristic.setValue(new byte[]{0x01});


            gatt.writeCharacteristic(characteristic);
        }

        @Override
        public synchronized void onCharacteristicChanged(BluetoothGatt gatt,
                                                         BluetoothGattCharacteristic characteristic) {

            final byte[] dataInput = characteristic.getValue();

            if(unsignedByteToInt(dataInput[0]) == 0x06){
                    byte[] sendBytes = {screenBuffer[currentXPos],
                            screenBuffer[currentXPos + 1],
                            screenBuffer[currentXPos + 2],
                            screenBuffer[currentXPos + 3],
                            screenBuffer[currentXPos + 4],
                            screenBuffer[currentXPos + 5],
                            screenBuffer[currentXPos + 6],
                            screenBuffer[currentXPos + 7]};

                    currentXPos+=8;
                    send(sendBytes);

                    if(currentXPos >= 1024){
                        paintView.clear();

                        currentXPos = 0;
                    }

/*                //send(screenBuffer);
                for(int i = 0; i < screenBuffer.length; i++){

                    byte[] byteToSend = {screenBuffer[i]};
                    send(byteToSend);
                }*/

            }

        }


    };



    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = MainActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                //view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                //viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                //viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                //viewHolder.deviceName.setText(R.string.unknown_device);
                viewHolder.deviceAddress.setText(device.getAddress());
            return view;
        }


        class ViewHolder {
            TextView deviceName;
            TextView deviceAddress;
        }


    }
}