package com.tudor.activateprinter;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Iterator;
import java.io.File;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (!new File(this.getApplicationInfo().dataDir + "/shared_prefs/" + getResources().getString(R.string.prefs_file) + ".xml").exists())
        {
            SharedPreferences sharedPreferences = getSharedPreferences(getResources().getString(R.string.prefs_file), MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(getResources().getString(R.string.reinstall_usb_app), getResources().getBoolean(R.bool.reinstall_usb_app_def_opt));
            editor.putString(getResources().getString(R.string.usb_app_location), getResources().getString(R.string.usb_app_location_def_opt));
            editor.apply();
        }
    }


    // This method must have this exact signature
    public void onButtonClick(View view) {
        // Code to be executed when the button is clicked

        ButtonThread buttonThread = new ButtonThread(this);
        buttonThread.start();
//
//        UsbManager usbManager = (UsbManager) getSystemService(USB_SERVICE);
//        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
//        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
//
//        StringBuilder deviceNames = new StringBuilder();
//
//        while (deviceIterator.hasNext()) {
//            UsbDevice device = deviceIterator.next();
//            deviceNames.append(device.getDeviceName()).append("\n");
//        }
//
//        if (deviceNames.length() == 0) {
//            Toast.makeText(this, "No USB devices connected", Toast.LENGTH_SHORT).show();
//        } else {
//            Toast.makeText(this, "Connected USB devices:\n" + deviceNames.toString(), Toast.LENGTH_LONG).show();
//        }
    }
}