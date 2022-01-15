package com.example.smartwatchuiver20;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

// ** MAIN SCREEN OPENING SCREEN **
public class MainActivity extends Activity {

    int currentapiVersion = android.os.Build.VERSION.SDK_INT;

    TextView startedTextView;
    Button findDeviceButton, hoodButton, doorsButton, liftGateButton;
    private Locale currentLocal = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("API Level of Device: " + currentapiVersion);

        setContentView(R.layout.device_list_prototype1);

        startedTextView = (TextView) findViewById(R.id.gettingStartedView);
        hoodButton = (Button) findViewById(R.id.hoodButton);
        doorsButton = (Button) findViewById(R.id.doorsButton);
        liftGateButton = (Button) findViewById(R.id.liftGateButton);
        findDeviceButton = (Button) findViewById(R.id.deviceSetupButton);
        currentLocal = Locale.getDefault();

        hoodButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(SetupActivity.btDevice == null){
                    Toast toast = Toast.makeText(getBaseContext(),getString(R.string.toast_msg), Toast.LENGTH_SHORT);
                    toast.show();
                }
                else {
                    OpenSliderActivity(R.drawable.hood_button, "hoodPreferences");
                }
            }
        });

        doorsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(SetupActivity.btDevice == null){
                    Toast toast = Toast.makeText(getBaseContext(),getString(R.string.toast_msg), Toast.LENGTH_SHORT);
                    toast.show();
                }
                else {
                    OpenSliderActivity(R.drawable.doors_button, "doorsPreferences");
                }
            }
        });
        liftGateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(SetupActivity.btDevice == null){
                    Toast toast = Toast.makeText(getBaseContext(),getString(R.string.toast_msg), Toast.LENGTH_SHORT);
                    toast.show();
                }
                else {
                    OpenSliderActivity(R.drawable.liftgate_button, "liftgatePreferences");
                }
            }
        });
        findDeviceButton.setOnClickListener(v -> OpenSettingsActivity());
    }

    @Override
    protected void onResume() {
        if (currentLocal.getLanguage() != Locale.getDefault().getLanguage()) {
            currentLocal = Locale.getDefault();
            recreate();
        }
        currentLocal = getResources().getConfiguration().locale;
        System.out.println(currentLocal);
        super.onResume();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        SharedPreferences doorpref = getSharedPreferences("doorsPreferences", MODE_PRIVATE);
        SharedPreferences liftgatepref = getSharedPreferences("liftgatePreferences", MODE_PRIVATE);
        SharedPreferences hoodpref = getSharedPreferences("hoodPreferences", MODE_PRIVATE);

        doorpref.edit().clear().commit();
        liftgatepref.edit().clear().commit();
        hoodpref.edit().clear().commit();
    }

    public void OpenSliderActivity(int SliderMenuLogo, String sharedPrefName){

        Intent intent = new Intent(this, SliderActivity.class);
        intent.putExtra("HeaderIMG",SliderMenuLogo);
        intent.putExtra("SHAREDPREFNAME", sharedPrefName);

        startActivity(intent);
    }

    public void OpenSettingsActivity(){
        Intent intent = new Intent(this, SetupActivity.class);
        startActivity(intent);
    }
}

