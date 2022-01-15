package com.example.smartwatchuiver20;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SetupActivity extends Activity {
    TextView languageAbrev;
    Button backButton, connectionButton, languageButton;
    BluetoothAdapter adapter;
    BluetoothManager manager;
    Set<BluetoothDevice> pairedDevices;

    static BluetoothDevice btDevice;
    BluetoothInterface btHandler = new BluetoothInterface();

    Dialog popUpDialog;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
   //     overridePendingTransition(R.anim.fade, R.anim.fadeout);
        setContentView(R.layout.setup_activity);

        backButton = (Button) findViewById(R.id.backButtonSetup);
        connectionButton = (Button) findViewById(R.id.connectionButton);
        if (btDevice == null){}
        else{
            connectionButton.setText(btDevice.getName());
        }
        languageButton = (Button) findViewById(R.id.languageButton);
        languageAbrev = (TextView) findViewById(R.id.languageAbr);

        manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        adapter = manager.getAdapter();
        adapter.enable();
        pairedDevices = adapter.getBondedDevices();

        ListView listOfDevices = new ListView(this);
        listOfDevices.setX(0);
        listOfDevices.setY(0);
        listOfDevices.setPadding(0,0,0,0);
        List<String> data = new ArrayList<>();
        data.add(getString(R.string.none));

        for(BluetoothDevice bt : pairedDevices){
            data.add(bt.getName());
        }
        ArrayAdapter<String> arrAdapter = new ArrayAdapter<>(this, R.layout.devicepopup, data);
        listOfDevices.setAdapter(arrAdapter);


        AlertDialog.Builder builder = new AlertDialog.Builder(SetupActivity.this,R.style.magnaDialog);
        builder.setCancelable(true);
        builder.setView(listOfDevices);
        final AlertDialog dialog = builder.create();
        dialog.getWindow().getAttributes().windowAnimations = R.style.magnaDialog;
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.requestWindowFeature(Window.FEATURE_ACTION_BAR);
        dialog.setTitle("                List Of Devices");



        popUpDialog = new Dialog(this);
        popUpDialog.setCanceledOnTouchOutside(true);

        System.out.println(pairedDevices);

        backButton.setOnClickListener(v -> finish());

        connectionButton.setOnClickListener(v -> {
            dialog.show();
            dialog.getWindow().setGravity(Gravity.TOP);
        });

        languageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChangeLanguageDialogue();
            }
        });
        listOfDevices.setOnItemClickListener((parent, view, position, id) -> {
            connectionButton.setText(arrAdapter.getItem(position));
            /*if(position != 0){
                btDevice = pairedDevices.
            }*/
            for (BluetoothDevice bt: pairedDevices){
                if(connectionButton.getText() == getString(R.string.none)){btDevice = null;}
                if(connectionButton.getText().equals(bt.getName())){
                    btDevice = bt;
                    try {
                        btHandler.btConnect();
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                else{
                }
            }
            dialog.dismiss();
        });
    }

    private void showChangeLanguageDialogue(){
        final String[] listItems = {"English", "German", "Italian"};
        AlertDialog.Builder mbuilder = new AlertDialog.Builder(SetupActivity.this);
        mbuilder.setTitle(getString(R.string.choose_language));
        mbuilder.setSingleChoiceItems(listItems, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0){
                    setLocale("en");
                    recreate();
                }
                else if (which == 1){
                    setLocale("de");
                    recreate();
                }
                else if (which == 2){
                    setLocale("it");
                    recreate();
                }
                dialog.dismiss();
            }
        });
        AlertDialog mDialog = mbuilder.create();
        mDialog.show();
    }

    public void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());

    }

}
