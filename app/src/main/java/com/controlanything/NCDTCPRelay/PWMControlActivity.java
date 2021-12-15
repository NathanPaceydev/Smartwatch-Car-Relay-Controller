package com.controlanything.NCDTCPRelay;

import com.controlanything.NCDTCPRelay.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class PWMControlActivity extends Activity{
	
	//Global Objects
	ControlPanel cPanel;
	Typeface font;
	Intent i;
	Intent findDeviceIntent;
	Messenger findDeviceMessenger;
	
	//Global Variables
	String deviceMacAddress;
	String deviceName;
	String defaultIP;
	int port;
	int numberOfChannels;
	String[] channelNames;
	int currentapiVersion = android.os.Build.VERSION.SDK_INT;
	public int[] channelStatusArray;
	
	//Global Views
	AnimationDrawable saveButtonAnimation;
	RelativeLayout titleTable;
	ImageView bottomButton;
	TextView[] channelLabels;
	AlertDialog lostConnectionDialog;
	ProgressDialog progressDialog;
	ScrollView sView;
	SeekBar[] channelBar;
	
	//Bluetooth connection objects
	BluetoothAdapter btAdapter;
	BluetoothDevice btDevice;
	android.bluetooth.BluetoothSocket btSocket;
	String btDeviceAddress;
	boolean bluetooth;
	boolean winet;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		cPanel = ((ControlPanel)getApplicationContext());
		font = Typeface.createFromAsset(getAssets(), "fonts/neuropolxfree.ttf");
		i = getIntent();
		
		deviceMacAddress = i.getStringExtra("MAC");
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(cPanel.connected == true){
			cPanel.disconnect();
			
		}else{
			System.out.println("cPanel.connected == false");
		}
		cPanel.connected = false;
		finish();
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		getDeviceInfo();
		setContentView(mainViewTable());
		if(currentapiVersion>=11){
			saveButtonAnimation.start();
		}
		
		if(cPanel.connected == false){
			if(bluetooth){
				if(cPanel.connect(deviceMacAddress) == false){
					System.out.println("Could not connect");
				}
			}else{
				if(cPanel.connect(defaultIP, port) == false){
					System.out.println("Could not connect");
				}
			}
			
//			fusion = cPanel.checkFusion();
		}
		
	}
	
	public void getDeviceInfo(){
		
		String[] deviceSettings = cPanel.getStoredString(deviceMacAddress).split(";");
		if(deviceSettings[2].equalsIgnoreCase("Bluetooth")){
			bluetooth = true;
		}else{
			bluetooth = false;
		}
		numberOfChannels = Integer.parseInt(deviceSettings[3]);
		deviceName = deviceSettings[4];
		if(!bluetooth){
			defaultIP = deviceSettings[1];
			port = Integer.parseInt(deviceSettings[2]);
		}
		//Initialize view objects
		channelLabels = new TextView[numberOfChannels];
		channelStatusArray = new int[numberOfChannels];
		channelBar = new SeekBar[numberOfChannels];
		
		//Get Channel Names
		channelNames = cPanel.getStoredString(deviceMacAddress+"Names").split(";");
		
		//Check WiNet
		if(deviceSettings[8].equalsIgnoreCase("true")){
			cPanel.winet = true;
		}
		
		//This is definitely not a Fusion controller so make sure cPanel knows that
		cPanel.fusion = false;
	}
	
	//This is the main view that holds everything
	public RelativeLayout mainViewTable(){
		RelativeLayout mTable = new RelativeLayout(this);
		mTable.setBackgroundResource(R.drawable.background);
		mTable.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		mTable.addView(title());
		
		RelativeLayout.LayoutParams scrollViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
		scrollViewParams.addRule(RelativeLayout.BELOW, titleTable.getId());
		
		mTable.addView(scrollView(), scrollViewParams);
		
		RelativeLayout.LayoutParams bottomButtonParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 171);
		bottomButtonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		
		mTable.addView(deviceListButton(), bottomButtonParams);	
		

		
		return mTable;
	}
	
	public RelativeLayout title(){
		titleTable = new RelativeLayout(this);
		//titleTable.setBackgroundResource(R.drawable.top_bar);
		titleTable.setId(1);
		
//		table.setLayoutParams(new LayoutParams(displayWidth,(int) convertPixelsToDp(248, this)));
		
		final TextView tView = new TextView(this);
//		tView.setPadding(15, 70, 0, 0);
		tView.setText(deviceName);
		tView.setTypeface(font);
		tView.setTextSize(30);
		tView.setTextColor(Color.BLACK);

		RelativeLayout.LayoutParams titleLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		
		titleLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
		
		titleTable.addView(tView,titleLayoutParams);
		
		return titleTable;
	}

	public ScrollView scrollView(){
		sView = new ScrollView(this);
		//Allows for height of bottom button
		sView.setPadding(0, 0, 0, 171);

		sView.addView(controlsTable());		
		
		return sView;
	}
	
	public ImageView deviceListButton(){
		bottomButton = new ImageView(this);
		bottomButton.setId(2);
		
		if(currentapiVersion>=11){
			bottomButton.setImageResource(R.drawable.animationxmldevicelist);
			bottomButton.setBackgroundResource(0);
//			bottomButton.setMinimumHeight(120);
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
//			bottomButton.setMinimumHeight(120);
//			bottomButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
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

	public TableLayout controlsTable(){
		TableLayout cTable = new TableLayout(this);
		if(cPanel.connected == false){
			if(bluetooth){
				cPanel.connect(deviceMacAddress);
			}else{
				if(cPanel.remote == true){
					cPanel.connect(cPanel.sAddress.getAddress().getHostAddress(), cPanel.port);
				}else{
					cPanel.connect(defaultIP, port);
				}
			}
		}
		
		for(int i = 0; i<numberOfChannels; i++){
			
			//Initialize Relative Layout view to hold channel view objects
			RelativeLayout channelControlRow = new RelativeLayout(this);
			channelControlRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
			
			//initialize individual view objects for channel
			channelBar[i] = new SeekBar(this);
			channelLabels[i] = new TextView(this);
			
			//set up the channel label
			channelLabels[i].setText(channelNames[i]);
			channelLabels[i].setTextSize(20);
			channelLabels[i].setTextColor(Color.WHITE);
			channelLabels[i].setGravity(Gravity.CENTER_VERTICAL);
			channelLabels[i].setPadding(10, 35, 0, 0);
			channelLabels[i].setTypeface(font);
			channelLabels[i].setMaxLines(2);
			channelLabels[i].setLineSpacing(50, 1);
			channelLabels[i].setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
			channelLabels[i].setId(i+20);
			
			channelBar[i].setId(i);
			channelBar[i].setMax(255);
			channelBar[i].setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			channelBar[i].setPadding(40, 0, 40, 0);
			channelBar[i].setOnSeekBarChangeListener(new seekBarListener());
			RelativeLayout.LayoutParams channelBarParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
			channelBarParams.addRule(RelativeLayout.BELOW, channelLabels[i].getId());

			channelControlRow.addView(channelLabels[i]);
			channelControlRow.addView(channelBar[i], channelBarParams);
			
			cTable.addView(channelControlRow);
			
			//Set the status of seekBars
			
		}
		updateChannelStatus();
		return cTable;
		
	}
	
	private class seekBarListener implements OnSeekBarChangeListener{

		public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
			// TODO Auto-generated method stub
			
		}

		public void onStartTrackingTouch(SeekBar bar) {
			// TODO Auto-generated method stub
			
		}

		public void onStopTrackingTouch(SeekBar bar) {
			int channel = bar.getId()+1;
			if(cPanel.setChannelBrightness(channel, bar.getProgress())){
				//Command Executed
			}else{
				showAlertDialog("Connection Lost");
			}
			
		}
		
	}
	
	public void updateChannelStatus(){
		int[] currentStatus = cPanel.getChannelBrightness(numberOfChannels);
		if(currentStatus != null){
			for(int i = 0; i<channelBar.length;i++){
				System.out.println("Channel "+(i+1)+" status = "+currentStatus[i]);
				channelBar[i].setProgress(currentStatus[i]);
			}
		}
	}
	
	public void showAlertDialog(String title){
		System.out.println("showAlertDialog called");
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
						showProgressDialog("Searching Lan for device");
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
	
	public void startFindDeviceService(String deviceMac, String location ){
		findDeviceIntent = new Intent(getApplicationContext(), FindDevice.class);
		findDeviceMessenger = new Messenger(findDeviceHandler());
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
				System.out.println("RelayControlActivity finddevice handler called");
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
				System.out.println("Got this IP back");
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
