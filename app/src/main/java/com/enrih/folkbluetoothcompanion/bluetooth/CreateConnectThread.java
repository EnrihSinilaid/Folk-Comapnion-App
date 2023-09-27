package com.enrih.folkbluetoothcompanion.bluetooth;

import static android.content.ContentValues.TAG;

import static com.enrih.folkbluetoothcompanion.constants.Statuses.CONNECTING_STATUS;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.enrih.folkbluetoothcompanion.activity.MainActivity;

import java.io.IOException;
import java.util.UUID;

public class CreateConnectThread extends Thread {

    public static ConnectedThread connectedThread;
    public static BluetoothSocket mmSocket;
    public Handler mmHandler;

    @SuppressLint("MissingPermission")
    public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address, Handler handler) {
            /*

            Use a temporary object that is later assigned to mmSocket
            because mmSocket is final.
             */
        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
        BluetoothSocket tmp = null;
        UUID uuid = bluetoothDevice.getUuids()[0].getUuid();
        mmHandler = handler;

        try {
                /*
                Get a BluetoothSocket to connect with the given BluetoothDevice.
                Due to Android device varieties,the method below may not work fo different devices.
                You should try using other methods i.e. :
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                 */
            tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);

        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
        mmSocket = tmp;
    }

    @SuppressLint("MissingPermission")
    public void run() {
        // Cancel discovery because it otherwise slows down the connection.
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.cancelDiscovery();
        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            mmSocket.connect();

            Log.e("Status", "Device connected");
            mmHandler.obtainMessage(CONNECTING_STATUS.getCode(), 1, -1).sendToTarget();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            try {
                mmSocket.close();
                Log.e("Status", "Cannot connect to device");
                mmHandler.obtainMessage(CONNECTING_STATUS.getCode(), -1, -1).sendToTarget();
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
            return;
        }

        // The connection attempt succeeded. Perform work associated with
        // the connection in a separate thread.
        connectedThread = new ConnectedThread(mmSocket, mmHandler);
        connectedThread.start();
    }

    // Closes the client socket and causes the thread to finish.
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }
}
