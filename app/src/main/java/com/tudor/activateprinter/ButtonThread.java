package com.tudor.activateprinter;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Random;

public class ButtonThread extends Thread{
    private static final String COMMAND = "pm install -r -d ";
    private static final String SUCCESSFUL_COMMAND = "Success";
    private static final String LOG_ERROR_APP_REINSTALLATION_STRING = "AppReinstallation";



    private MainActivity context = null;
    private int deviceVendorId = -1;
    private int deviceProductId = -1;

    ButtonThread(MainActivity context, int deviceVendorId, int deviceProductId)
    {
        this.context = context;
        this.deviceVendorId = deviceVendorId;
        this.deviceProductId = deviceProductId;
    }

    public void showToast(String toShow, int toastLength)
    {
        context.runOnUiThread(() -> Toast.makeText(context, toShow, toastLength).show());
    }

    @Override
    public void run() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getResources().getString(R.string.prefs_file), Context.MODE_PRIVATE);

        boolean shouldReinstall = sharedPreferences.getBoolean(context.getResources().getString(R.string.reinstall_usb_app), context.getResources().getBoolean(R.bool.reinstall_usb_app_def_opt));
        if(shouldReinstall) reinstallApp(sharedPreferences.getString(context.getResources().getString(R.string.usb_app_location), context.getResources().getString(R.string.usb_app_location_def_opt)));

        sendSCSICommand();
    }

    public void sendSCSICommand()
    {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        UsbDevice device = null;

        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        for (UsbDevice d : deviceList.values()) {
            if (d.getVendorId() == deviceVendorId && d.getProductId() == deviceProductId) {  // Vendor ID and Product ID specific to your device
                device = d;
            }
        }

        if (device == null) {
            showToast("Error: device is not connected", Toast.LENGTH_SHORT);
            return;
        }

        if (!usbManager.hasPermission(device)) {
            showToast("Error: no permission for the device", Toast.LENGTH_SHORT);
            return;
        }

        UsbDeviceConnection connection = usbManager.openDevice(device);

        UsbInterface usbInterface = device.getInterface(0);
        UsbEndpoint endpointOut = null;
        UsbEndpoint endpointIn = null;

        // Find the appropriate endpoints
        for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
            UsbEndpoint endpoint = usbInterface.getEndpoint(i);
            if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                endpointOut = endpoint;
            } else if (endpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                endpointIn = endpoint;
            }
        }
        if (connection != null && connection.claimInterface(usbInterface, true)) {

            // Prepare CBW (Command Block Wrapper)
            byte[] cbw = new byte[31];
            ByteBuffer buffer = ByteBuffer.wrap(cbw);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(0x43425355); // dCBWSignature 'USBC'
            buffer.putInt(new Random().nextInt()); // dCBWTag (randomized unique identifier)
            buffer.putInt(0); // dCBWDataTransferLength (no data transfer for this command)
            buffer.put((byte) 0x00); // bmCBWFlags (OUT direction)
            buffer.put((byte) 0); // bCBWLUN (Logical Unit Number, usually 0)
            buffer.put((byte) 0x06); // bCBWCBLength (length of the SCSI CDB)

            // Prepare SCSI CDB (Command Descriptor Block)
            byte[] scsiCmd = new byte[6];
            scsiCmd[0] = (byte) 0xD0; // Opcode (custom/vendor-specific)
            scsiCmd[1] = (byte) 0x00; // LUN (Logical Unit Number)
            scsiCmd[2] = (byte) 0x00; // Parameter 1 (based on the command requirements)
            scsiCmd[3] = (byte) 0x00; // Parameter 2 (based on the command requirements)
            scsiCmd[4] = (byte) 0x00; // Parameter 3 (based on the command requirements)
            scsiCmd[5] = (byte) 0x00; // Parameter 4 (based on the command requirements)

            buffer.put(scsiCmd); // Add the 6-byte SCSI CDB to the CBW

            // Send the CBW
            int sentLength = connection.bulkTransfer(endpointOut, cbw, cbw.length, 1000);
            if (sentLength < 0) {
                showToast("Error sending the SCSI command", Toast.LENGTH_SHORT);
                return;
            }

            byte[] dataBuffer = new byte[cbw.length];
            int receivedLength = connection.bulkTransfer(endpointIn, dataBuffer, dataBuffer.length, 1000);
            if (receivedLength < 0){
                showToast("Error receiving response for SCSI command", Toast.LENGTH_SHORT);
                return;
            }

            showToast("Printer activated successfully", Toast.LENGTH_SHORT);
        }else{
            showToast("Error with connection or interface", Toast.LENGTH_SHORT);
        }
    }


    public void reinstallApp(String path)
    {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"su", "-c", COMMAND + "'" + path + "'"});
        } catch (IOException e) {
            showToast("App reinstallation failed", Toast.LENGTH_SHORT);
            return;
        }


        int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            showToast("App reinstallation failed", Toast.LENGTH_SHORT);
            return;
        }

        // Get the input streams to read the output and error streams of the process
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        // Read the output
        String line = null;
        StringBuilder output = new StringBuilder();
        while (true) {
            try {
                if ((line = reader.readLine()) == null) break;
                output.append(line).append("\n");
            } catch (IOException e) {
                Log.e(LOG_ERROR_APP_REINSTALLATION_STRING, "Error reading lines from command stdout: " + e.getMessage());
                break;
            }
        }

        if (output.length() > 0 && output.charAt(output.length() - 1) == '\n')
            output.deleteCharAt(output.length() - 1);


        if (output.toString().contains(SUCCESSFUL_COMMAND)) {
            showToast("App reinstallation successful", Toast.LENGTH_SHORT);
        } else {
            showToast("App reinstallation failed", Toast.LENGTH_SHORT);
        }

        try{
            reader.close();
        }catch (IOException e){
            Log.e(LOG_ERROR_APP_REINSTALLATION_STRING, "Error closing the reader for the stdout of the command: " + e.getMessage());
        }
    }
}
