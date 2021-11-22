package com.controlanything.NCDTCPRelay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.ResourceBundle;

public class CommandReceiver extends BroadcastReceiver {
    ControlPanel cPanel;
    public CommandReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving

        if(intent.hasExtra("MACRO")){
            cPanel = ((ControlPanel)context.getApplicationContext());
            String macro = intent.getStringExtra("MACRO");


        }
    }
}
