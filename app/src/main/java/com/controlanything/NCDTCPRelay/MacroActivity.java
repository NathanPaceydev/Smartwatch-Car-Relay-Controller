package com.controlanything.NCDTCPRelay;

import java.util.ArrayList;
import java.util.HashMap;

import com.controlanything.NCDTCPRelay.RelayControlActivity.MyGestureDetector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.Vibrator;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MacroActivity extends Activity{
	
	//Global Objects
	HashMap<String,ArrayList<byte[]>> macroDownCommands;
	HashMap<String,ArrayList<Integer>>macroDownDelays;
	HashMap<String,ArrayList<byte[]>> macroUpCommands;
	HashMap<String,ArrayList<Integer>>macroUpDelays;
	HashMap<String,String>macroNames;
	ControlPanel cPanel;
	private Vibrator myVib;
	BroadcastReceiver mReceiver;
	
	//Global Variables
	String deviceMacAddress;
	String[] macroIDs;
	String deviceName;
	int currentapiVersion = android.os.Build.VERSION.SDK_INT;
	boolean switchToADActivity = false;
	boolean switchToRelayActivity = false;
	boolean displayInputs = false;
	boolean displayRelays = false;
	boolean fusion = false;
	
	
	//Global Views
	RelativeLayout titleTable;
	ScrollView sView;
	ImageView bottomButton;
	Button[] macroButtons;
	AlertDialog lostConnectionDialog;
	ProgressDialog progressDialog;
	TextView bText;
	
	//Global Variables for views
	Typeface font;
	AnimationDrawable saveButtonAnimation;
	GestureDetector gDetector;
	int textColor = Color.WHITE;
	int subTextSize = 20;
	
	
	//Global Variables for Network TCP connection
	String defaultIP;
	int port;
	boolean winet;
	
	//BlueTooth connection objects
	BluetoothAdapter btAdapter;
	BluetoothDevice btDevice;
	android.bluetooth.BluetoothSocket btSocket;
	String btDeviceAddress;
	boolean bluetooth;	
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Get info from calling Intent
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		deviceMacAddress = extras.getString("MAC");
		
		//Instantiate global objects.
		macroDownCommands = new HashMap<String,ArrayList<byte[]>>();
		macroDownDelays = new HashMap<String,ArrayList<Integer>>();
		macroUpCommands = new HashMap<String,ArrayList<byte[]>>();
		macroUpDelays = new HashMap<String,ArrayList<Integer>>();
		macroNames = new HashMap<String,String>();
		cPanel = ((ControlPanel)getApplicationContext());
		myVib = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
		
		font = Typeface.createFromAsset(getAssets(), "fonts/neuropolxfree.ttf");
		gDetector = new GestureDetector(getApplicationContext(), new MyGestureDetector());	
		mReceiver = new communicationHandlerReceiver();
		this.registerReceiver(mReceiver, new IntentFilter(cPanel.cPanelIntentFilter));
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(cPanel.connected == true){
			if(!switchToADActivity || switchToADActivity){
				cPanel.disconnect();
			}
			
		}else{
			System.out.println("cPanel.connected == false");
		}
		this.unregisterReceiver(mReceiver);
		cPanel.connected = false;
		finish();
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		String deviceSettings = cPanel.getStoredString(deviceMacAddress);
		String[] deviceSettingsSplit = deviceSettings.split(";");
		
		getStoredSettings();
		setContentView(mainViewTable());
		if(currentapiVersion>=11){
			saveButtonAnimation.start();
		}
		if(!cPanel.connected){
			if(!bluetooth){
				cPanel.connect(cPanel.sAddress.getAddress().getHostAddress(), cPanel.port);
			}else{
				if(!cPanel.connect(btDeviceAddress)){
					//TODO throw alert
					System.out.println("Could not connect on resume via Bluetooth from MacroActivity");
				}
			}
		}
		
	}
	
	public RelativeLayout mainViewTable(){
		RelativeLayout mTable = new RelativeLayout(this);
		mTable.setBackgroundResource(R.drawable.background);
		mTable.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		mTable.addView(title());
		
		RelativeLayout.LayoutParams bottomButtonParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 171);
		bottomButtonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
				
		mTable.addView(deviceListButton(), bottomButtonParams);	
		
		if(displayInputs || displayRelays){
			RelativeLayout.LayoutParams bottomTextParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
			bottomTextParams.addRule(RelativeLayout.ABOVE, bottomButton.getId());
			if(displayInputs){
				mTable.addView(bottomText("Swipe right to display Inputs Page"), bottomTextParams);
			}else{
				mTable.addView(bottomText("Swipe right to display Relays Page"), bottomTextParams);
			}
		}
		
		
		
		RelativeLayout.LayoutParams scrollViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
		scrollViewParams.addRule(RelativeLayout.BELOW, titleTable.getId());
		if(displayInputs || displayRelays){
			scrollViewParams.addRule(RelativeLayout.ABOVE, bText.getId());
		}else{
			scrollViewParams.addRule(RelativeLayout.ABOVE, bottomButton.getId());
		}
		
		
		mTable.addView(scrollView(), scrollViewParams);
		
		
		

		
		return mTable;
	}
	
	public RelativeLayout title(){
		titleTable = new RelativeLayout(this);
		//titleTable.setBackgroundResource(R.drawable.top_bar);
		titleTable.setId(1);
				
		final TextView tView = new TextView(this);
		tView.setText(deviceName);
		tView.setTypeface(font);
		tView.setTextSize(30);
		tView.setTextColor(Color.BLACK);

		RelativeLayout.LayoutParams titleLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		
		titleLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
		
		titleTable.addView(tView,titleLayoutParams);
		
		return titleTable;
	}
	
	public TextView bottomText(String message){
		bText = new TextView(this);
		bText.setId(3);
		bText.setText(message);
		bText.setTextColor(textColor);
		bText.setTextSize(this.subTextSize);
		return bText;
	}
	
	public ScrollView scrollView(){
		sView = new ScrollView(this);
		//Allows for height of bottom button
		sView.setPadding(0, 0, 0, 171);

		sView.addView(controlsTable());
		
		if(displayInputs || displayRelays){
			sView.setOnTouchListener(new View.OnTouchListener() {
				
				public boolean onTouch(View v, MotionEvent event) {
					if(gDetector.onTouchEvent(event)){
						
						if(displayInputs){
							System.out.println("Switching to ADInput Activity");
							switchToADActivity = true;
							Intent adIntent = new Intent(getApplicationContext(), ADInputActivity.class);
							adIntent.putExtra("MAC", deviceMacAddress);
							if(!bluetooth){
								adIntent.putExtra("IP", cPanel.sAddress.getAddress().getHostAddress());
								adIntent.putExtra("PORT", cPanel.sAddress.getPort());
							}else{
								adIntent.putExtra("BLUETOOTHADDRESS", deviceMacAddress);
							}
							
							startActivity(adIntent);
							return true;
						}
						
						if(displayRelays){
							System.out.println("Switching to Relays Activity");
							switchToRelayActivity = true;
							Intent relayControlIntent = new Intent(getApplicationContext(), RelayControlActivity.class);
							relayControlIntent.setAction("Start");
							relayControlIntent.putExtra("MAC", deviceMacAddress);
							startActivity(relayControlIntent);
							return true;
							
						}
					}
					return false;
				}
			});
		}
		
		
		
		return sView;
	}
	
	public LinearLayout controlsTable(){
		LinearLayout cTable = new LinearLayout(this);
		cTable.setOrientation(LinearLayout.VERTICAL);
		
		if(cPanel.connected == false){
			if(bluetooth){
				cPanel.connect(deviceMacAddress);
			}else{
				cPanel.connect(cPanel.sAddress.getAddress().getHostAddress(), cPanel.port);
			}
			
			fusion = cPanel.checkFusion();
		}else{
			fusion = cPanel.checkFusion();
		}
		
		macroButtons = new Button[macroIDs.length];
		for(int i = 0; i < macroIDs.length; i++){
			
			String macroID = macroIDs[i];	
			
			macroButtons[i] = new Button(this);
			macroButtons[i] = macroButton(macroID);
			
			cTable.addView(macroButtons[i]);
		}
		
		return cTable;
	}
	
	private Button macroButton(String macroID){
		final Button button = new Button(this);
		button.setBackgroundResource(R.drawable.textbox);
		button.setText(macroNames.get(macroID));
		button.setTextColor(textColor);
		button.setTypeface(font);
		button.setTextSize(subTextSize);
		button.setPadding(10, 10, 10, 10);
		button.setTag(macroID);
		button.setOnTouchListener(new View.OnTouchListener() {
			
			public boolean onTouch(View v, MotionEvent event) {
				if(cPanel.connected){
					if (event.getAction() == MotionEvent.ACTION_DOWN){
						if(!(macroDownCommands.get(button.getTag()) == null)){
							cPanel.sendCommand(macroDownCommands.get(button.getTag()), macroDownDelays.get(button.getTag()));
						}					
					}
					else if (event.getAction() == MotionEvent.ACTION_UP){
						if(!(macroUpCommands.get(button.getTag()) == null)){
							cPanel.sendCommand(macroUpCommands.get(button.getTag()), macroUpDelays.get(button.getTag()));
						}					
					}
					return false;
				}else{
					if(!bluetooth){
						if(cPanel.connect(cPanel.sAddress.getAddress().getHostAddress(), cPanel.port)){
							if (event.getAction() == MotionEvent.ACTION_DOWN){
								if(!(macroDownCommands.get(button.getTag()) == null)){
									cPanel.sendCommand(macroDownCommands.get(button.getTag()), macroDownDelays.get(button.getTag()));
								}					
							}
							else if (event.getAction() == MotionEvent.ACTION_UP){
								if(!(macroUpCommands.get(button.getTag()) == null)){
									cPanel.sendCommand(macroUpCommands.get(button.getTag()), macroUpDelays.get(button.getTag()));
								}					
							}
							return false;
						}else{
							//TODO trigger connection lost handler
							System.out.println("Could not connect via TCP from MacroActivity");
							return false;
						}
					}else{
						if(cPanel.connect(btDeviceAddress)){
							if (event.getAction() == MotionEvent.ACTION_DOWN){
								if(!(macroDownCommands.get(button.getTag()) == null)){
									cPanel.sendCommand(macroDownCommands.get(button.getTag()), macroDownDelays.get(button.getTag()));
								}					
							}
							else if (event.getAction() == MotionEvent.ACTION_UP){
								if(!(macroUpCommands.get(button.getTag()) == null)){
									cPanel.sendCommand(macroUpCommands.get(button.getTag()), macroUpDelays.get(button.getTag()));
								}					
							}
							return false;
						}else{
							//TODO trigger connection lost handler
							System.out.println("Could not connect via Bluetooth from MacroActivity");
							return false;
						}
					}
				}
				
			}
		});
		
		
		
		return button;
	}
	
	public ImageView deviceListButton(){
		bottomButton = new ImageView(this);
		bottomButton.setId(2);
		
		if(currentapiVersion>=11){
			bottomButton.setImageResource(R.drawable.animationxmldevicelist);
			bottomButton.setBackgroundResource(0);
			bottomButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 100));
			//bottomButton.setBackgroundResource(R.drawable.bottom_bar);
			
			saveButtonAnimation = (AnimationDrawable)bottomButton.getDrawable();
			saveButtonAnimation.setEnterFadeDuration(1500);
			saveButtonAnimation.setExitFadeDuration(1500);
			
			bottomButton.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					Intent listView = new Intent(getApplicationContext(), DeviceListActivity.class);
					startActivity(listView);
					if(cPanel.connected == true){
						cPanel.disconnect();
					}
					finish();
					
				}
				
			});
			
		}else{
			bottomButton.setImageResource(R.drawable.bottom_bar_list);
			bottomButton.setBackgroundResource(0);
			//bottomButton.setBackgroundResource(R.drawable.bottom_bar);
			
			bottomButton.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					Intent listView = new Intent(getApplicationContext(), DeviceListActivity.class);
					startActivity(listView);
					if(cPanel.connected == true){
						cPanel.disconnect();
					}else{
						System.out.println("cPanel.connected == false");
					}
					finish();
					
				}
				
			});
		}
		
		
		
		return bottomButton;
	}
	
	public void getStoredSettings(){
		//Get device settings for its name and number of relays.
		String[] deviceSettings = cPanel.getStoredString(deviceMacAddress).split(";");
		if(deviceSettings[2].equalsIgnoreCase("Bluetooth")){
			bluetooth = true;
		}else{
			bluetooth = false;
		}
		deviceName = deviceSettings[4];
		if(!bluetooth){
			defaultIP = deviceSettings[1];
			port = Integer.parseInt(deviceSettings[2]);
		}
		
		//Get info for switching control Activities
		if(deviceSettings[6].equalsIgnoreCase("true")){
			displayInputs = true;
		}else{
			displayInputs = false;
		}
		if(deviceSettings[7].equalsIgnoreCase("true")){
			displayRelays = true;
		}else{
			displayRelays = false;
		}
		
		//Get info for WiNet 
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
		
		//Call Method to read all Macro button settings
		setupMacros();
		
		
		
	}
	
	private void setupMacros(){
		//Get Stored info for Macros
		//Get Macro IDs
		macroIDs = cPanel.getStoredString(deviceMacAddress+"Macros").split(";");
		if(macroIDs.length != 0){

			for(String macroID : macroIDs){
				String[] macroInfo = cPanel.getStoredString(deviceMacAddress+macroID).split("~");
				if(macroInfo.length > 1){
					macroNames.put(macroID, macroInfo[0]);
					if(!macroInfo[1].startsWith(";")){
						String[] upDown = macroInfo[1].split(";");
						String[] downActions = upDown[0].split("-");
						ArrayList<byte[]>commands = new ArrayList<byte[]>();
						ArrayList<Integer>delays = new ArrayList<Integer>();
						for(String action : downActions){

							String[] commandDelaySplit = action.split("#");
							String[] commandSplit = commandDelaySplit[0].split(",");
							delays.add(Integer.parseInt(commandDelaySplit[1]));
							byte[] commandBytes = new byte[commandSplit.length];
							for(int i = 0; i < commandSplit.length; i++){
								int c = Integer.parseInt(commandSplit[i]);
								if(c < 0){
									c = c+256;
								}
								commandBytes[i] = (byte)c;
							}
							commands.add(commandBytes);
						}
						macroDownCommands.put(macroID, commands);
						macroDownDelays.put(macroID, delays);

						if(upDown.length > 1){
							String[] upActions = upDown[1].split("-");
							ArrayList<byte[]>commands1 = new ArrayList<byte[]>();
							ArrayList<Integer>delays1 = new ArrayList<Integer>();
							for(String action : upActions){
								String[] commandDelaySplit = action.split("#");
								String[] commandSplit = commandDelaySplit[0].split(",");
								delays1.add(Integer.parseInt(commandDelaySplit[1]));
								byte[] commandBytes = new byte[commandSplit.length];
								for(int i = 0; i < commandSplit.length; i++){
									int c = Integer.parseInt(commandSplit[i]);
									if(c < 0){
										c = c+256;
									}
									commandBytes[i] = (byte)c;
								}
								commands1.add(commandBytes);
							}
							macroUpCommands.put(macroID, commands1);
							macroUpDelays.put(macroID, delays1);
						}
						else{
							macroUpCommands.put(macroID, null);
							macroUpDelays.put(macroID, null);
						}


					}else{
						macroDownCommands.put(macroID, null);
						macroDownDelays.put(macroID, null);
					}
				}else{
					macroNames.put(macroID, macroInfo[0]);
					macroDownCommands.put(macroID, null);
					macroDownDelays.put(macroID, null);
				}
			}

		}else{
			System.out.println("No Stored Macros, switching to DeviceList Activity");
			makeToast("No Stored Macros");

		}
	}
	
	public void switchToDeviceList(){
		Intent listView = new Intent(getApplicationContext(), DeviceListActivity.class);
		startActivity(listView);
		if(cPanel.connected == true){
			cPanel.disconnect();
		}
		finish();
	}
	
	private void makeToast(String message){
		Toast toast = Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG);
		toast.show();
	}
	
	private class MyGestureDetector extends SimpleOnGestureListener
    {
    	@Override
    	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY){
    		if(e1 != null && e2 != null){
    			if(e2.getX()-e1.getX() > 200){
        			System.out.println("onFling returning true");
        			return true;
        		}
    		}
    		
    		System.out.println("onFling returning false");
			return false;
    		
    	}
    }

	private class communicationHandlerReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			System.out.println("communicationHandlerReceiver fired");
			Bundle extras = intent.getExtras();
			if(extras != null){
				if(extras.getString("ERROR")!= null){
					showAlertDialog("Connection Lost");
				} 
			}
			
		}
		
	}
	
	public void showAlertDialog(String title){
		System.out.println("showAlertDialog called");
		
		AlertDialog lostConnectionDialog; 
		
		final AlertDialog.Builder removeDeviceAlert = new AlertDialog.Builder(this);
		
		if(bluetooth){
			
			removeDeviceAlert.setTitle(title);
	    	removeDeviceAlert.setMessage("Retry Connection");
	    	removeDeviceAlert.setCancelable(false);
	    	removeDeviceAlert.setPositiveButton("Retry", new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {
					showProgressDialog("Connecting");
					if(cPanel.connect(btDeviceAddress)){
						dismissProgressDialog();
						Toast toast = Toast.makeText(getBaseContext(), "Connected", Toast.LENGTH_LONG);
						toast.show();
					}else{
						dismissProgressDialog();
						showAlertDialog("Could Not Connect");
					}
					
				}
	    		
	    	});
	    	removeDeviceAlert.setNeutralButton("Exit", new DialogInterface.OnClickListener() {
				
				public void onClick(DialogInterface dialog, int which) {
					if(cPanel.connected){
						cPanel.disconnect();
					}
					Intent deviceListActivity = new Intent(getApplicationContext(), DeviceListActivity.class);
					startActivity(deviceListActivity);
					finish();
					
				}
			});
			
		}else{

			if(title.equals("Connection Lost")){
				if(isWiFiConnected(getBaseContext()) == true){
					removeDeviceAlert.setMessage("Retry Connection");
				}else{
					removeDeviceAlert.setMessage("Retry Connection \nWiFi Not Available");
				}


				removeDeviceAlert.setTitle(title);
				//	    	removeDeviceAlert.setMessage("Retry Connection");
				removeDeviceAlert.setCancelable(false);
				removeDeviceAlert.setPositiveButton("Local", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which){
						showProgressDialog("Searching Lan for device");
						cPanel.disconnect();

						if(cPanel.connect(defaultIP, port)==true){
							System.out.println("cPanel.connect = true");
							dismissProgressDialog();
							dialog.dismiss();
							sView.removeAllViews();
							sView.addView(controlsTable());
							Toast toast = Toast.makeText(getBaseContext(), "Connected", Toast.LENGTH_LONG);
							toast.show();

						}else{
							startFindDeviceService(deviceMacAddress, "local");
						}
					}
				});
				removeDeviceAlert.setNeutralButton("Remote", new DialogInterface.OnClickListener(){

					public void onClick(DialogInterface dialog, int which) {
						showProgressDialog("Getting Connection through Signal Switch");
						cPanel.disconnect();

						String wiNetMac = cPanel.getStoredString(deviceMacAddress+"-"+"wiNet-wiNetMac");
						if(!wiNetMac.equalsIgnoreCase("n/a")){
							startFindDeviceService(wiNetMac, "remote");

						}else{
							startFindDeviceService(deviceMacAddress, "remote");

						}
					}

				});
				removeDeviceAlert.setNegativeButton("Exit", new DialogInterface.OnClickListener(){

					public void onClick(DialogInterface dialog, int which) {
						Intent deviceListActivity = new Intent(getApplicationContext(), DeviceListActivity.class);
						startActivity(deviceListActivity);
						finish();
					}

				});
			}
			if(title.equals("Could Not Connect Local")){
				removeDeviceAlert.setTitle(title);
				if(isWiFiConnected(getBaseContext()) == true){
					removeDeviceAlert.setMessage("Retry Connection");
				}else{
					removeDeviceAlert.setMessage("Retry Connection \nWiFi Not Available");
				}
				removeDeviceAlert.setCancelable(false);
				removeDeviceAlert.setPositiveButton("Local", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which){
						cPanel.disconnect();

						if(cPanel.connect(defaultIP, port)==true){
							dismissProgressDialog();
							dialog.dismiss();
							sView.removeAllViews();
							sView.addView(controlsTable());
							Toast toast = Toast.makeText(getBaseContext(), "Connected", Toast.LENGTH_LONG);
							toast.show();
						}else{
							startFindDeviceService(deviceMacAddress, "local");
						}
					}
				});
				removeDeviceAlert.setNeutralButton("Remote", new DialogInterface.OnClickListener(){

					public void onClick(DialogInterface dialog, int which) {
						showProgressDialog("Getting Connection through Signal Switch");
						cPanel.disconnect();

						String wiNetMac = cPanel.getStoredString(deviceMacAddress+"-"+"wiNet-wiNetMac");
						if(!wiNetMac.equalsIgnoreCase("n/a")){
							startFindDeviceService(wiNetMac, "remote");

						}else{
							startFindDeviceService(deviceMacAddress, "remote");
						}

					}

				});
				removeDeviceAlert.setNegativeButton("Exit", new DialogInterface.OnClickListener(){

					public void onClick(DialogInterface dialog, int which) {
						Intent deviceListActivity = new Intent(getApplicationContext(), DeviceListActivity.class);
						startActivity(deviceListActivity);
						finish();
					}

				});
			}
			if(title.equals("Could Not Connect")){
				removeDeviceAlert.setTitle(title);
				if(isWiFiConnected(getBaseContext()) == true){
					removeDeviceAlert.setMessage("Retry Connection");
				}else{
					removeDeviceAlert.setMessage("Retry Connection \nWiFi Not Available");
				}
				removeDeviceAlert.setCancelable(false);
				removeDeviceAlert.setPositiveButton("Local", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which){
						cPanel.disconnect();

						if(cPanel.connect(defaultIP, port)==true){
							dismissProgressDialog();
							dialog.dismiss();
							sView.removeAllViews();
							sView.addView(controlsTable());
							Toast toast = Toast.makeText(getBaseContext(), "Connected", Toast.LENGTH_LONG);
							toast.show();
						}else{
							startFindDeviceService(deviceMacAddress, "local");
						}
					}
				});
				removeDeviceAlert.setNeutralButton("Remote", new DialogInterface.OnClickListener(){

					public void onClick(DialogInterface dialog, int which) {
						showProgressDialog("Getting Connection through Signal Switch");
						cPanel.disconnect();

						String wiNetMac = cPanel.getStoredString(deviceMacAddress+"-"+"wiNet-wiNetMac");
						if(!wiNetMac.equalsIgnoreCase("n/a")){
							startFindDeviceService(wiNetMac, "remote");

						}else{
							startFindDeviceService(deviceMacAddress, "remote");
						}

					}

				});
				removeDeviceAlert.setNegativeButton("Exit", new DialogInterface.OnClickListener(){

					public void onClick(DialogInterface dialog, int which) {
						Intent deviceListActivity = new Intent(getApplicationContext(), DeviceListActivity.class);
						startActivity(deviceListActivity);
						finish();
					}

				});

			}
			if(title.equals("Could Not Connect Remote")){
				removeDeviceAlert.setTitle(title);
				if(isWiFiConnected(getBaseContext()) == true){
					removeDeviceAlert.setMessage("Retry Connection");
				}else{
					removeDeviceAlert.setMessage("Retry Connection \nWiFi Not Available");
				}
				removeDeviceAlert.setCancelable(false);
				removeDeviceAlert.setPositiveButton("Local", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which){
						cPanel.disconnect();

						if(cPanel.connect(defaultIP, port)==true){
							dismissProgressDialog();
							dialog.dismiss();
							sView.removeAllViews();
							sView.addView(controlsTable());
							Toast toast = Toast.makeText(getBaseContext(), "Connected", Toast.LENGTH_LONG);
							toast.show();
						}else{
							startFindDeviceService(deviceMacAddress, "local");
						}
					}
				});
				removeDeviceAlert.setNeutralButton("Remote", new DialogInterface.OnClickListener(){

					public void onClick(DialogInterface dialog, int which) {
						showProgressDialog("Getting Connection through Signal Switch");
						cPanel.disconnect();

						String wiNetMac = cPanel.getStoredString(deviceMacAddress+"-"+"wiNet-wiNetMac");
						if(!wiNetMac.equalsIgnoreCase("n/a")){
							startFindDeviceService(wiNetMac, "remote");

						}else{
							startFindDeviceService(deviceMacAddress, "remote");
						}

					}

				});
				removeDeviceAlert.setNegativeButton("Exit", new DialogInterface.OnClickListener(){

					public void onClick(DialogInterface dialog, int which) {
						Intent deviceListActivity = new Intent(getApplicationContext(), DeviceListActivity.class);
						startActivity(deviceListActivity);
						finish();
					}

				});

			}
		}
		lostConnectionDialog = removeDeviceAlert.create();
    	lostConnectionDialog.show();
	}
	
	private void showProgressDialog(String message){
    	progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(message);
        progressDialog.show();
    }
	
	private void dismissProgressDialog() {
        if(progressDialog != null)
            progressDialog.dismiss();
    }
	
	public void startFindDeviceService(String deviceMac, String location ){
		Intent findDeviceIntent = new Intent(getApplicationContext(), FindDevice.class);
		Messenger findDeviceMessenger = new Messenger(findDeviceHandler());
		findDeviceIntent.setAction("Start");
		findDeviceIntent.putExtra("LOCATION", location);
		findDeviceIntent.putExtra("MAC", deviceMac);
		findDeviceIntent.putExtra("MESSENGER", findDeviceMessenger);
		startService(findDeviceIntent);
	}
	
	private Handler findDeviceHandler(){
		Handler fdHandler = new Handler(){
			public void handleMessage(Message message){
				dismissProgressDialog();
				System.out.println(message.obj.toString());
				System.out.println("MacroActivity finddevice handler called");
				if (message.obj.toString() == "device not found local")
				{
					showAlertDialog("Could Not Connect Local");
					return;
				}
				
				if(message.obj.toString() == "device not available"){
					showAlertDialog("Could Not Connect Remote");
					return;
				}
				
				String recievedIP = message.obj.toString();
				Toast toast = Toast.makeText(getBaseContext(), "Got This IP back: "+recievedIP, Toast.LENGTH_LONG);
				toast.show();
				if(cPanel.connect(recievedIP, port)){
					lostConnectionDialog.dismiss();
					dismissProgressDialog();
					sView.removeAllViews();
    				sView.addView(controlsTable());
				}else{
					System.out.println("Could not Connect to "+recievedIP);
					dismissProgressDialog();
					Toast toast1 = Toast.makeText(getBaseContext(), "Could not find device", Toast.LENGTH_LONG);
					toast1.show();
					
					lostConnectionDialog.dismiss();
					showAlertDialog("Could Not Connect");
					
				}
			}
		};
		
		return fdHandler;
	}
	
	public static boolean isWiFiConnected(Context context){
    	ConnectivityManager connectivityManager = (ConnectivityManager)
    	        context.getSystemService(Context.CONNECTIVITY_SERVICE);
    	    NetworkInfo networkInfo = null;
    	    if (connectivityManager != null) {
    	        networkInfo =
    	            connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    	    }
    	    return networkInfo == null ? false : networkInfo.isConnected();
    }
	
}
