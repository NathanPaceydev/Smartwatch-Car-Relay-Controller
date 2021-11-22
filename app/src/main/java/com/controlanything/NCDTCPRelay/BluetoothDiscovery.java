package com.controlanything.NCDTCPRelay;

import java.util.Set;

import com.controlanything.NCDTCPRelay.R;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
// bluetooth devices screen activity
public class BluetoothDiscovery extends Activity{
	
	//Global Variables
	Typeface font;
	int currentapiVersion = android.os.Build.VERSION.SDK_INT;
	AnimationDrawable saveButtonAnimation;
	boolean pwm;
	
	//Global Views
	RelativeLayout titleTable;
	ImageView bottomButton;
	
	//Global Classes
	ControlPanel cPanel;
	BluetoothAdapter mBtAdapter;
	ArrayAdapter<String> pairedDevicesListAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		font = Typeface.createFromAsset(getAssets(), "fonts/neuropolxfree.ttf");
		cPanel = ((ControlPanel)getApplicationContext());
		
		setContentView(mainView());
		if(currentapiVersion>=11){
    		saveButtonAnimation.start();
    	}
	}
	
	public RelativeLayout mainView(){
		RelativeLayout rLayout = new RelativeLayout(this);
		rLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		rLayout.setBackgroundResource(R.drawable.background);
		
		rLayout.addView(title());

		// set the layout of the Exit button
		RelativeLayout.LayoutParams bottomButtonParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 100);
		bottomButtonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		rLayout.addView(exitButton(), bottomButtonParams);
		
		RelativeLayout.LayoutParams listViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
		listViewParams.addRule(RelativeLayout.BELOW, titleTable.getId());
		listViewParams.addRule(RelativeLayout.ABOVE, bottomButton.getId());
		rLayout.addView(listView(), listViewParams);
		
		return rLayout;
		
		
	}
	
	public RelativeLayout title(){
		titleTable = new RelativeLayout(this);
		titleTable.setBackgroundResource(R.drawable.top_bar);
		titleTable.setId(1);
		
//		table.setLayoutParams(new LayoutParams(displayWidth,(int) convertPixelsToDp(248, this)));
		

		RelativeLayout.LayoutParams titleLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		
		titleLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
		
		//titleTable.addView(titleLayoutParams);
		
		return titleTable;
	}
	
	public ImageView exitButton(){
		bottomButton = new ImageView(this);
		bottomButton.setId(2);
		if(currentapiVersion >= 11){
			
			bottomButton.setImageResource(R.drawable.animationxmlexit);
			//bottomButton.setBackgroundResource(R.drawable.bottom_bar);
			
			saveButtonAnimation = (AnimationDrawable)bottomButton.getDrawable();
			saveButtonAnimation.setEnterFadeDuration(1000);
			saveButtonAnimation.setExitFadeDuration(2000);
			
			bottomButton.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					Intent deviceList = new Intent(getApplicationContext(), DeviceListActivity.class);
					startActivity(deviceList);
					finish();
					
				}
				
			});
		}else{
			bottomButton.setImageResource(R.drawable.bottom_bar_exit);
			bottomButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			//bottomButton.setBackgroundResource(R.drawable.bottom_bar);
			
			bottomButton.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					Intent deviceList = new Intent(getApplicationContext(), DeviceListActivity.class);
					startActivity(deviceList);
					finish();
					
				}
				
			});
			
		}
		
		
		return bottomButton;
	}

	public ListView listView(){
		ListView pairedDevicesList = new ListView(this);
		pairedDevicesListAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
		
		//Get Paired Devices
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
		if (pairedDevices.size() > 0) {

			for (BluetoothDevice device : pairedDevices) {
				pairedDevicesListAdapter.add(device.getName() + "\n" + device.getAddress());
			}
		} else {
			pairedDevicesListAdapter.add("No Paired Devices");
		}
		
		pairedDevicesList.setAdapter(pairedDevicesListAdapter);
		pairedDevicesList.setOnItemClickListener(listItemSelectedListener);
		return pairedDevicesList;
	}
	
	public OnItemClickListener listItemSelectedListener = new OnItemClickListener(){

		public void onItemClick(AdapterView<?> arg0, View v, int arg2,
				long arg3) {
			// Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            String deviceName = info.substring(0, info.length() - 17);
            System.out.println("Selected Device Bluetooth Address: "+address);
            System.out.println("Selected Device Name: "+deviceName);
            
            String deviceInfo = ((TextView) v).getText().toString();
			final String[] deviceInfoArray = deviceInfo.split("\n");
            
            if (cPanel.getStoredString("savedDevices") != "n/a")
			{
				cPanel.saveString("savedDevices", cPanel.getStoredString("savedDevices") +  deviceInfoArray[1] + ";");
//				cPanel.saveString(deviceInfoArray[0], deviceInfoArray[0] + ";" + deviceInfoArray[1]);
				//					createSaveSettingsPage(deviceInfoArray[0]);

			}

			else
			{
				cPanel.saveString("savedDevices", deviceInfoArray[1] + ";");
				//					createSaveSettingsPage(deviceInfoArray[0]);

			}
            
            if(cPanel.connect(address)){
            	pwm = cPanel.checkPWM();
            }
            
            
            Intent settingsPage = new Intent(getBaseContext(), SettingsPageActivity.class);
            settingsPage.putExtra("BLUETOOTHADDRESS", address);
            settingsPage.putExtra("DEVICENAME", deviceName);
            if(pwm){
            	settingsPage.putExtra("PWM", "true");
            }
            startActivity(settingsPage);
            finish();
			
		}
		
	};

	
	@Override
	protected void onResume() {
		mBtAdapter.startDiscovery();
		BroadcastReceiver broadcastReciever = new BroadcastReceiver(){

			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if(action.equalsIgnoreCase(BluetoothDevice.ACTION_FOUND)){
					// Get the BluetoothDevice object from the Intent
			        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			        pairedDevicesListAdapter.add(device.getName() + "\n" + device.getAddress());
				}
				
			}
			
		};
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND); 
		registerReceiver(broadcastReciever, filter);
		super.onResume();
	}

	@Override
	protected void onPause() {
		mBtAdapter.cancelDiscovery();
		super.onPause();
	}
	

	
	
}
