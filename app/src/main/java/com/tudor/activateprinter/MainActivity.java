package com.tudor.activateprinter;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final String ACTION_USB_PERMISSION = "com.tudor.activateprinter.USB_PERMISSION";
    private UsbManager usbManager;
    private int[] deviceId;
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null && device.getVendorId() == deviceId[0] && device.getProductId() == deviceId[1] && !usbManager.hasPermission(device)) {
                    PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, permissionIntent);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceId = parseDeviceFilter(this);
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        registerReceiver(usbReceiver, filter);

        UsbDevice device = null;
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        for(UsbDevice d : deviceList.values())
        {
            if (d.getVendorId() == deviceId[0] && d.getProductId() == deviceId[1])
            {
                device = d;
                break;
            }
        }

        if (device != null && !usbManager.hasPermission(device)) {
            PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
            usbManager.requestPermission(device, permissionIntent);
        }

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
        ButtonThread buttonThread = new ButtonThread(this, deviceId[0], deviceId[1]);
        buttonThread.start();
    }

    public int[] parseDeviceFilter(Context context) {
        int vendorId = -1;
        int productId = -1;

        XmlResourceParser parser = context.getResources().getXml(R.xml.device_filter);
        try {
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && "usb-device".equals(parser.getName())) {
                    vendorId = Integer.parseInt(parser.getAttributeValue(null, "vendor-id").replace("0x", ""), 16);
                    productId = Integer.parseInt(parser.getAttributeValue(null, "product-id").replace("0x", ""), 16);
                    break; // Assuming only one device is specified, break after finding it
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException | IOException e) {
            Log.e("DeviceFilterParsing", "Error parsing device_filter.xml", e);
        } finally {
            parser.close();
        }

        return new int[]{vendorId, productId};
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(usbReceiver);
    }

}