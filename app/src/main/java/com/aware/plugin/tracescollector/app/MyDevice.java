package com.aware.plugin.tracescollector.app;

import android.bluetooth.BluetoothDevice;

/**
 * Created by researcher on 02/06/15.
 */
public class MyDevice {

    private BluetoothDevice device;

    public MyDevice(BluetoothDevice nDevice)
    {
        device = nDevice;
    }

    @Override
    public String toString() {
        return device.getName() + "\n" + device.getAddress();
    }

    public BluetoothDevice getDevice()
    {
        return device;
    }
}
