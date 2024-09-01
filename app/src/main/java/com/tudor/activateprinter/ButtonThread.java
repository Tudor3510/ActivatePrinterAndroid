package com.tudor.activateprinter;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ButtonThread extends Thread{
    private MainActivity context = null;

    private static final String COMMAND = "pm install -r -d ";
    private static final String SUCCESSFUL_COMMAND = "Success";
    private static final String LOG_ERROR_APP_REINSTALLATION_STRING = "app_reinstallation";

    ButtonThread(MainActivity context)
    {
        this.context = context;
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
                Log.e(LOG_ERROR_APP_REINSTALLATION_STRING, "Eroare la citirea liniilor din stdout-ul comenzii: " + e.getMessage());
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
            Log.e(LOG_ERROR_APP_REINSTALLATION_STRING, "Eroare la inchiderea reader-ului pt stdout-ul comenzii: " + e.getMessage());
        }
    }
}
