package com.controlanything.NCDTCPRelay;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;


//TODO add functional code lol
public class SliderActivity extends Activity{

    ControlPanel cPanel;
    String deviceMacAddressSlider;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String relayName = intent.getStringExtra("RelayName");
        System.out.println("**Slider Activity **"+"\n"+relayName);

        deviceMacAddressSlider = intent.getStringExtra("MAC");

        setContentView(R.layout.slider_activity);

        //cPanel = ((ControlPanel)getApplicationContext());
        //cPanel.connect(deviceMacAddressSlider); // TODO

        // get the relay name textview
        final TextView relayNameTextView = (TextView) findViewById(R.id.relayName);
        final ImageButton cancelButton = (ImageButton) findViewById(R.id.BackButton);
        //change name to relayName
        relayNameTextView.setText(relayName);

        cancelButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                Intent relayControl = new Intent(getApplicationContext(), RelayControlActivity.class);
                startActivity(relayControl);
                finish();
                //DeviceListActivity.super.onBackPressed();//send back to home screen
            }
        });
    }

/*
    @Override
    protected void onResume() {
        super.onResume();

        //tvSocketConnection = new TextView(this);
        getDeviceInfo();
        setContentView(R.layout.slider_activity);
        //setContentView(mainViewTable());
        //System.out.println("Bottom Button Height"+deviceListButtonTableView.getHeight());

        String[] deviceInfo = cPanel.getStoredString(deviceMacAddressSlider).split(";");

        //System.out.println(" ***"+cPanel.getStoredString(deviceMacAddress));

        if(deviceInfo[2].equalsIgnoreCase("Bluetooth")){
            bluetooth = true;
        }else{
            bluetooth = false;
        }

        if(cPanel.connected == false){
            if(bluetooth){
                if(cPanel.connect(deviceMacAddressSlider) == false){
                    System.out.println("Could not connect");
                }
            }else{
                if(cPanel.connect(defaultIP, port) == false){
                    System.out.println("Could not connect");
                }
            }

//			fusion = cPanel.checkFusion();
        }else{
//			fusion = cPanel.checkFusion();
        }
    }


    public void getDeviceInfo(){
        //Get device settings for its name and number of relays.
        System.out.println(deviceMacAddressSlider.toString());
        String[] deviceSettings = cPanel.getStoredString(deviceMacAddressSlider).split(";");

        System.out.println(" ***"+cPanel.getStoredString(deviceMacAddressSlider));


        for (int i = 0; i<deviceSettings.length; i++)
        {
            System.out.println(deviceSettings[i]);
        }



        if(deviceSettings[2].equalsIgnoreCase("Bluetooth")){
            bluetooth = true;
            btDeviceAddress = deviceMacAddressSlider;
        }else{
            bluetooth = false;
        }
        numberOfRelays = Integer.parseInt(deviceSettings[3]);
        deviceName = deviceSettings[4];
        if(!bluetooth){
            defaultIP = deviceSettings[1];
            port = Integer.parseInt(deviceSettings[2]);
        }


        //Get Relay Names
        relayNames = cPanel.getStoredString(deviceMacAddressSlider+"Names").split(";");

        //Get button momentary or not.
        String[] momentaryString = cPanel.getStoredString(deviceMacAddressSlider+"Momentary").split(";");
        momentaryIntArray = new int[numberOfRelays];
        for(int i = 0; i<momentaryString.length; i++){
            if (momentaryString[i].equals("1")){
//				System.out.println("momentaryString[i] = 1|0");
                momentaryIntArray[i] = 1;
            }else
            {
                momentaryIntArray[i] = 0;
            }
        }

        if(deviceSettings[6].equalsIgnoreCase("true")){
            displayInputs = true;
        }else{
            displayInputs = false;
        }
        if(deviceSettings.length > 8){
            if(deviceSettings[8].equalsIgnoreCase("true")){
                winet = true;
                cPanel.winet = true;
            }else{
                cPanel.winet = false;
            }
        }else{
            cPanel.winet = false;
        }
        if(deviceSettings[11].equalsIgnoreCase("true")){
            displayMacros = true;
        }


    }

 */





}
