package com.controlanything.NCDTCPRelay;

import java.util.Arrays;

import com.controlanything.NCDTCPRelay.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.text.InputType;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

// Network Discovery activity
public class DiscoveryActivity extends Activity{
	
	ControlPanel cPanel;
	
	Intent deviceDiscoveryIntent;
	Messenger deviceDiscoveryMessenger;
	Boolean continueScanningUDP = true;
	
	Intent webIDiscoveryIntent;
	Messenger webIdiscoveryMessenger;
	
	public ArrayAdapter<String> discoveredDevicesArrayAdapter;
	String discoveredDevicesString;
	
	AnimationDrawable saveButtonAnimation;
	Typeface font;
	
	int displayWidth;
	int displayHeight;
	int currentapiVersion = android.os.Build.VERSION.SDK_INT;
	
	String api;
	int port = 2101;
	String dBm;
	String displayDBM;
	String ssid;
	
	//Global Views
	ImageView bottomButton;
	RelativeLayout rLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		cPanel = ((ControlPanel)getApplicationContext());

		System.out.println("Network Discovery Activity");
		
		//Get display size minus top bar
		Display display = getWindowManager().getDefaultDisplay(); 
		displayWidth = display.getWidth();  // deprecated
		displayHeight = display.getHeight();
		displayHeight = displayHeight - getStatusBarHeight();
		
		font = Typeface.createFromAsset(getAssets(), "fonts/neuropolxfree.ttf"); //set font
		
		//Get connected Network Name:
		WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		ssid = wifiInfo.getSSID();
		
		setContentView(mainTable()); // call the main table layout
		if(currentapiVersion>=11){
			saveButtonAnimation.start();
		}
		
		
		deviceDiscoveryIntent = new Intent(this, DiscoveryUtility.class);
		deviceDiscoveryIntent.setAction("Start");

		deviceDiscoveryMessenger = new Messenger(discoveryHandler());
    	deviceDiscoveryIntent.putExtra("BOOLEAN", continueScanningUDP);
    	deviceDiscoveryIntent.putExtra("MESSENGER", deviceDiscoveryMessenger);
    	
    	webIDiscoveryIntent = new Intent(this, WebiDiscoveryService.class);
    	webIDiscoveryIntent.setAction("Start");

    	webIdiscoveryMessenger = new Messenger(discoveryHandler());
    	webIDiscoveryIntent.putExtra("BOOLEAN", continueScanningUDP);
    	webIDiscoveryIntent.putExtra("MESSENGER", webIdiscoveryMessenger);
    	
    	startService(deviceDiscoveryIntent);
    	startService(webIDiscoveryIntent);
	}

	
	public RelativeLayout mainTable(){

		RelativeLayout mTable = new RelativeLayout(this);
		mTable.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		mTable.setBackgroundResource(R.drawable.background);
		mTable.setGravity(RelativeLayout.CENTER_HORIZONTAL);
		
		mTable.addView(titleTable());
		
		//Set layout rules for bottom button
		RelativeLayout.LayoutParams bottomButtonParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, 100);
		bottomButtonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		//Add button to master view
		mTable.addView(exitButton(), bottomButtonParams);
		
		//Set layout rules for ScrollView
		RelativeLayout.LayoutParams scrollViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		scrollViewParams.addRule(RelativeLayout.BELOW, rLayout.getId());
		scrollViewParams.addRule(RelativeLayout.ABOVE, bottomButton.getId());
		//Add ScrollView to master view.
		mTable.addView(listView(), scrollViewParams);		
		
		return mTable;
	}
	
	public RelativeLayout titleTable(){
		rLayout = new RelativeLayout(this);
		//rLayout.setBackgroundResource(R.drawable.top_bar); // got rid of title background
        int id = rLayout.generateViewId();
		rLayout.setId(id);
		
		TableLayout tTable = new TableLayout(this);
//		tTable.setLayoutParams(new LayoutParams(displayWidth,248));
		TableRow tRow = new TableRow(this);
		
		//Create TextView
		TextView title = new TextView(this);
		title.setText("Scanning");
		title.setTextColor(Color.BLACK);
		title.setTextSize(30);
		title.setTypeface(font);
//		title.setWidth(310);
        int id1 = title.generateViewId();
		title.setId(id1);
		tRow.addView(title);
		
		//Create manual add button
		Button mButton = new Button(this);
		mButton.setText("Manual");
		mButton.setTextColor(Color.WHITE);
		mButton.setBackgroundResource(R.drawable.textbox);
		mButton.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				manualAdd();
				
			}
			
		});
		tRow.addView(mButton);
		tTable.addView(tRow);
		
		//Create text view for SSID
		TextView networkName = new TextView(this);
		networkName.setText(ssid);
		networkName.setTextColor(Color.BLACK);
		networkName.setTextSize(12);
		networkName.setTypeface(font);
		networkName.setWidth(310);
		tTable.addView(networkName);
		
		RelativeLayout.LayoutParams titleLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		
		titleLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
		
		rLayout.addView(tTable, titleLayoutParams);
		
		return rLayout;

	}


	
	public RelativeLayout title(){
		RelativeLayout rLayout = new RelativeLayout(this);
		rLayout.setBackgroundResource(R.drawable.top_bar);
		rLayout.setLayoutParams(new LayoutParams(displayWidth,248));
		
		//Create TextView
		TextView title = new TextView(this);
		title.setText("Scanning");
		title.setTextColor(Color.BLACK);
		title.setTextSize(30);
		title.setTypeface(font);
		title.setWidth(310);
		int id = title.generateViewId();
        title.setId(id);
		
		//Create layout params for title textView
		RelativeLayout.LayoutParams titleParams = new RelativeLayout.LayoutParams(100,55);
		titleParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		titleParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		titleParams.leftMargin = 15;
		titleParams.topMargin = 8;
		titleParams.rightMargin = 0;
		
		//add title to view
		rLayout.addView(title, titleParams);
		
		
		Button mButton = new Button(this);
		mButton.setText("Manual");
		mButton.setTextColor(Color.WHITE);
		mButton.setBackgroundResource(R.drawable.textbox);
		mButton.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				manualAdd();
				
			}
			
		});
		
		RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(150, 100);
		buttonParams.addRule(RelativeLayout.RIGHT_OF, title.getId());
		buttonParams.leftMargin = 50;
		buttonParams.topMargin = 20;
		
		rLayout.addView(mButton, buttonParams);
		
		TextView networkName = new TextView(this);
		networkName.setText(ssid);
		networkName.setTextColor(Color.BLACK);
		networkName.setTextSize(12);
		networkName.setTypeface(font);
		networkName.setWidth(310);
		
		RelativeLayout.LayoutParams networkNameParams = new RelativeLayout.LayoutParams(300, 30);
		networkNameParams.addRule(RelativeLayout.BELOW, title.getId());
		networkNameParams.leftMargin = 30;
		
		rLayout.addView(networkName, networkNameParams);

		return rLayout;
	}
	
	public ListView listView(){
		discoveredDevicesArrayAdapter =  new ArrayAdapter<String>(this, R.layout.device_name);
		
		//Read Stored devices so we dont show them to user
    	discoveredDevicesString = null;
    	
    	String storedDevices = cPanel.getStoredString("savedDevices");
    	if (storedDevices != "n/a")
    	{
    		discoveredDevicesString = storedDevices;
    	}
    	if (discoveredDevicesString != null){
    	}
    	
    	ListView discoveredDevices = new ListView(this);
    	discoveredDevices.setAdapter(discoveredDevicesArrayAdapter);
    	discoveredDevices.setOnItemClickListener(addDeviceClickListener);
    	discoveredDevices.setLongClickable(true);
//    	discoveredDevices.setLayoutParams(new LayoutParams(displayWidth, displayHeight - 420));
    	
    	
    	return discoveredDevices;
	}
	
	public ImageView exitButton(){
		bottomButton = new ImageView(this);
        int id = bottomButton.generateViewId();
		bottomButton.setId(id);
		
		if(currentapiVersion>=11){
			bottomButton.setImageResource(R.drawable.animationxmlexit);
			bottomButton.setBackgroundResource(0);
			bottomButton.setPadding(0, 10, 0, 10);
			bottomButton.setMinimumHeight(120);
			bottomButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			bottomButton.setPadding(0, 10, 0, 10);
			//bottomButton.setBackgroundResource(R.drawable.bottom_bar);

            saveButtonAnimation = (AnimationDrawable)bottomButton.getDrawable();
            saveButtonAnimation.setEnterFadeDuration(1000);
            saveButtonAnimation.setExitFadeDuration(1000);
			
			
			bottomButton.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					Intent listViewIntent = new Intent(getApplicationContext(), DeviceListActivity.class);
					startActivity(listViewIntent);
					listViewIntent.setAction("Stop");
					continueScanningUDP = false;
					finish();
					
				}
				
			});
		}else{
			bottomButton.setImageResource(R.drawable.bottom_bar_exit);
			bottomButton.setBackgroundResource(0);
			bottomButton.setPadding(0, 10, 0, 10);
			bottomButton.setMinimumHeight(120);
			bottomButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			bottomButton.setPadding(0, 10, 0, 10);
			//bottomButton.setBackgroundResource(R.drawable.bottom_bar);
			
			bottomButton.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					Intent listViewIntent = new Intent(getApplicationContext(), DeviceListActivity.class);
					startActivity(listViewIntent);
					listViewIntent.setAction("Stop");
					continueScanningUDP = false;
					finish();
					
				}
				
			});
		}
		
		
		
		return bottomButton;
	}
	
	public void manualAdd(){
		
		final AlertDialog.Builder manualAddAlertBuilder = new AlertDialog.Builder(this);
		manualAddAlertBuilder.setTitle("Manualy Add Device");
		manualAddAlertBuilder.setMessage("Enter Mac Address of Device to add");
		final EditText macEdit = new EditText(this);
		macEdit.setHint("00:06:66:71:46:85");
		manualAddAlertBuilder.setView(macEdit);
		manualAddAlertBuilder.setCancelable(true);
		manualAddAlertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){

			public void onClick(DialogInterface dialog, int which) {
				
				
			}
			
		});
		manualAddAlertBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener(){

			public void onClick(DialogInterface dialog, int which) {
				String deviceInfo = macEdit.getText().toString();

				deviceDiscoveryIntent.setAction("Stop");
				webIDiscoveryIntent.setAction("Stop");
				if (cPanel.getStoredString("savedDevices") != "n/a")
				{
					cPanel.saveString("savedDevices", cPanel.getStoredString("savedDevices") +  deviceInfo + ";");
					cPanel.saveString(deviceInfo, deviceInfo + ";");
//					createSaveSettingsPage(deviceInfoArray[0]);

				}
				
				else
				{
					cPanel.saveString("savedDevices", deviceInfo + ";");
//					createSaveSettingsPage(deviceInfoArray[0]);

				}
				
				cPanel.saveString(deviceInfo, (deviceInfo + ";" + "" + ";" + "2101" + ";" + "1" + ";" + "Enter Name" +";"+"Enter Network"));
				Intent settingsPageIntent = new Intent(getApplicationContext(), SettingsPageActivity.class);
				settingsPageIntent.putExtra("MAC", deviceInfo);
				settingsPageIntent.putExtra("IP", "");
				startActivity(settingsPageIntent);
				finish();
				
			}
			
		});
		
		AlertDialog manualAddDialog = manualAddAlertBuilder.create();
		manualAddDialog.show();
		
	}
	
	public String wifiSignalStrength(int value, String valueString){
		
		String DBM;
		
		if(value > 74){
			dBm = "Weak";  
			DBM = ("Signal Strength -("+valueString+") dBm"+" ("+dBm+")");
			return DBM;
		}else{
			if(value<=74 && value>=64){
				dBm = "Fair";
				DBM = ("Signal Strength -("+valueString+") dBm"+" ("+dBm+")");
				return DBM;
			}else{
				if(value<64 && value>=53){
					dBm = "Good";
					DBM = ("Signal Strength -("+valueString+") dBm"+" ("+dBm+")");
					return DBM;
				}else{
					if(value<53 && value>=42){
						dBm = "Very Good";
						DBM = ("Signal Strength -("+valueString+") dBm"+" ("+dBm+")");
						return DBM;
					}else{
						//less than 42
							dBm = "Excelent";
							DBM = ("Signal Strength -("+valueString+") dBm"+" ("+dBm+")");
							return DBM;
					}
				}
			}
		}
		
	}
	
	public Handler discoveryHandler()
	{
		Handler dHandler = new Handler(){
	            public void handleMessage(Message message) 
	            {
	            	Object path = message.obj;
	            	Boolean saveTest = true;
	            	if (message.arg1 == RESULT_OK && path != null)
	            	{
	            		String mac = message.obj.toString();
	        			String[] splitMac = mac.split("~");
	        			System.out.println(splitMac[0]+" device found");
	        			
	            		if (discoveredDevicesString == null)
	            		{
	            			discoveredDevicesString = (splitMac[0] + ";");
	            			
	            			//Add Found Device to List View
	            			String[] listItem = mac.split("~");
	            			
	            			//Device is Ethernet
	            			if(listItem[3].equals("Ethernet")){
	            				displayDBM = "Ethernet Device";
	            			}else{
	            				//Device is WiNet
	            				if(listItem[3].equalsIgnoreCase("WiNet")){
	            					displayDBM = "WiNet";
	            				}
	            				else{
	            					if(listItem[3].equalsIgnoreCase("WEB-i")){
	            						System.out.println("Device is WEB-i");
	            						displayDBM = "WEB-i";
	            					}else{
	            						if(listItem[3].equalsIgnoreCase("webib")){
		            						displayDBM = "WEB-i";
		            						if (continueScanningUDP == true)
		                					{
		                            			startService(deviceDiscoveryIntent);
		                            			startService(webIDiscoveryIntent);
		                					}
			            					return;
			            				}else{
			            					//Device is WiFi so display signal strength
			            					int value = Integer.parseInt(listItem[3]);
			            					displayDBM = wifiSignalStrength(value, listItem[3]);
			            				}
	            					}
	            				}

	            			}
	            			//Add device to Array Adapter
	            			discoveredDevicesArrayAdapter.add(listItem[0] + "\n" + listItem[1]+":" +listItem[2] + "\n"+displayDBM);
	            			
	            			//If still active start the WiFi and Ethernet UDP listeners back up
	            			if (continueScanningUDP == true)
	            			{
	            				System.out.println("Activity starting discovery Services");
	                			startService(deviceDiscoveryIntent);
	                			startService(webIDiscoveryIntent);
	                		}
	            		}
	            		else
	            		{
	            			String[] splitString = discoveredDevicesString.split(";");
	            
	            			//Check all discovered devices to see if this device is already displayed
	            			for (int i = 0; i < splitString.length; i++)
	            			{

	            				if (splitString[i].toString().equals(splitMac[0].toString()))
	            				{
	            					if (continueScanningUDP == true)
	            					{
	            						System.out.println("Activity starting discovery Services");
	                        			startService(deviceDiscoveryIntent);
	                        			startService(webIDiscoveryIntent);
	            					}
	            					saveTest = false;
	            				}
	            			}
	            			if (saveTest)
	            			{
	            				discoveredDevicesString = (discoveredDevicesString + splitMac[0] + ";");
	            				//Add Found Device to List View
		            			String[] listItem = mac.split("~");
		            			//Device is Ethernet
		            			if(listItem[3].equals("Ethernet")){
		            				displayDBM = "Ethernet Device";
		            			}else{
		            				//Device is WiNet
		            				if(listItem[3].equalsIgnoreCase("WiNet")){
		            					displayDBM = "WiNet";
		            				}
		            				else
		            				{
		            					if(listItem[3].equalsIgnoreCase("web-i")){
		            						displayDBM = "WEB-i";
		            					}else{
		            						if(listItem[3].equalsIgnoreCase("webib")){
			            						if (continueScanningUDP == true)
			                					{
			                            			startService(deviceDiscoveryIntent);
			                            			startService(webIDiscoveryIntent);
			                					}
			            						return;
			            					}else{
			            						//Device is WiFi so display signal strength
			            						int value = Integer.parseInt(listItem[3]);
			            						displayDBM = wifiSignalStrength(value, listItem[3]);
			            					}
		            					}
		            				}
		            			}
		            			discoveredDevicesArrayAdapter.add(listItem[0] + "\n" + listItem[1]+":" +listItem[2] + "\n"+displayDBM);
	            				if (continueScanningUDP == true)
	        					{
	            					System.out.println("Activity starting discovery Services");
	                    			startService(deviceDiscoveryIntent);
	                    			startService(webIDiscoveryIntent);
	        					}
	            			}
	            		}
	            		
	            	}else{
	            		Toast toast = Toast.makeText(getBaseContext(), "Device found but could not connect, check device is listed on network(possible dhcp issue).", Toast.LENGTH_LONG);
						toast.show();
	            		if (continueScanningUDP == true)
    					{
                			startService(deviceDiscoveryIntent);
                			startService(webIDiscoveryIntent);
    					}
	            	}
	              

	            };
		};
		return dHandler;
	}
	
	private OnItemClickListener addDeviceClickListener = new OnItemClickListener(){

		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) 
		{
			boolean webi = false;
			
			deviceDiscoveryIntent.setAction("Stop");
			webIDiscoveryIntent.setAction("Stop");

			String deviceInfo = ((TextView) arg1).getText().toString();
			final String[] deviceInfoArray = deviceInfo.split("\n");

			final String[] deviceIPPort = deviceInfoArray[1].split(":");

			//WiNet Device
			if(deviceInfoArray[2].contains("WiNet")){
				
				System.out.println("WiNet device Selected");
				Intent wiNetIntent = new Intent(getApplicationContext(), WiNetDeviceList.class);
				if(pwm(true, deviceIPPort[0], 2101)){
					wiNetIntent.putExtra("PWM", "true");
				}
				if(linearActuator(true, deviceIPPort[0], 2101)){
					wiNetIntent.putExtra("ACTUATOR", "true");
				}
				
				cPanel.disconnect();
				wiNetIntent.putExtra("IP", deviceIPPort[0]);
				wiNetIntent.putExtra("MAC", deviceInfoArray[0]);
				startActivity(wiNetIntent);
				return;
			}else{
				if(deviceInfoArray[2].contains("WEB-i")){
					webi = true;
				}
				System.out.println(deviceInfoArray[2]);
			}

			if (cPanel.getStoredString("savedDevices") != "n/a")
			{
				cPanel.saveString("savedDevices", cPanel.getStoredString("savedDevices") +  deviceInfoArray[0] + ";");
				cPanel.saveString(deviceInfoArray[0], deviceInfoArray[0] + ";" + deviceInfoArray[1]);
				//					createSaveSettingsPage(deviceInfoArray[0]);

			}

			else
			{
				cPanel.saveString("savedDevices", deviceInfoArray[0] + ";");
				//					createSaveSettingsPage(deviceInfoArray[0]);

			}
			WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();

			cPanel.saveString(deviceInfoArray[0], (deviceInfoArray[0] + ";" + deviceIPPort[0] + ";" + deviceIPPort[1] + ";" + "1" + ";" + "Enter Name" + ";" +wifiInfo.getSSID()));
			Intent settingsPageIntent = new Intent(getApplicationContext(), SettingsPageActivity.class);
			if(pwm(false, deviceIPPort[0], Integer.parseInt(deviceIPPort[1]))){
				settingsPageIntent.putExtra("PWM", "true");
			}
			if(linearActuator(false, deviceIPPort[0], Integer.parseInt(deviceIPPort[1]))){
				settingsPageIntent.putExtra("ACTUATOR", "true");
			}
			cPanel.disconnect();
			settingsPageIntent.putExtra("MAC", deviceInfoArray[0]);
			settingsPageIntent.putExtra("IP", deviceIPPort[0]);
			settingsPageIntent.putExtra("PORT", deviceIPPort[1]);
			System.out.println("Device is WEBi? "+webi);
			if(webi){
				System.out.println("telling settings that device is webi");
				settingsPageIntent.putExtra("WEBI", "true");
			}
			startActivity(settingsPageIntent);

		}

	};
	
	public boolean pwm(boolean winet, String IP, int port){
		System.out.println("Checking PWM from discoveryActivity");
		cPanel.winet = winet;
		cPanel.bluetooth = false;
		if(cPanel.connect(IP, port)){
			return cPanel.checkPWM();
		}else{
			return false;
		}
		
		
		
	}
	
	public boolean linearActuator(boolean winet, String IP, int port){
		System.out.println("Checking Actuator");
		cPanel.winet = winet;
		cPanel.bluetooth = false;
		if(cPanel.connect(IP, port)){
			boolean pwm;
			pwm = cPanel.checkActuator();
			System.out.println("PWM " + pwm);
			return pwm;
		}else{
			System.out.println("Could not connect to check actuator.");
			return false;
		}
	}
		

	public int getStatusBarHeight() {
			int result = 0;
			int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
			if (resourceId > 0) {
				result = getResources().getDimensionPixelSize(resourceId);
			}
			return result;
		}
		
	@Override
	protected void onPause() {
		super.onPause();
		if(deviceDiscoveryIntent != null){
			continueScanningUDP = false;
			stopService(deviceDiscoveryIntent);
			stopService(webIDiscoveryIntent);
			finish();
		}
		
	}

	@Override
	protected void onResume() {
		super.onResume();
	}
}
