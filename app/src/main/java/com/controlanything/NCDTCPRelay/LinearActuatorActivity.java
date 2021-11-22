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
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class LinearActuatorActivity extends Activity{

	//Global Objects
	ControlPanel cPanel;
	Typeface font;
	Intent i;
	Intent findDeviceIntent;
	Messenger findDeviceMessenger;
	GestureDetector gDetector;


	//Global Variables
	String deviceMacAddress;
	String deviceName;
	String defaultIP;
	int port;
	int numberOfChannels;
	String[] channelNames;
	int currentapiVersion = android.os.Build.VERSION.SDK_INT;
	public int[] channelStatusArray;
	boolean right = false;
	int currentChannel;
	//Staticly setting speed for now
	int speed = 255;
	boolean communicationError = false;
	int counter = 0;
	int failureCount = 0;

	//Global Views
	AnimationDrawable saveButtonAnimation;
	RelativeLayout titleTable;
	ImageView bottomButton;
	TextView[] channelLabels;
	AlertDialog lostConnectionDialog;
	ProgressDialog progressDialog;
	ScrollView sView;
	RelativeLayout cView;
	Button inButton;
	Button outButton;
	SeekBar[] channelSliderBars;
	TextView channelTitle;

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
		gDetector = new GestureDetector(getApplicationContext(), new MyGestureDetector());
		font = Typeface.createFromAsset(getAssets(), "fonts/neuropolxfree.ttf");
		i = getIntent();

		deviceMacAddress = i.getStringExtra("MAC");
		
		Messenger cPanelMessenger = new Messenger(inputStatusHandler());
		cPanel.setMessenger(cPanelMessenger);
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
				}else{
					cPanel.getActuatorPosition();
				}
			}else{
				if(cPanel.connect(defaultIP, port) == false){
					System.out.println("Could not connect");
				}else{
					cPanel.getActuatorPosition();
				}
			}
		}else{
			
			cPanel.getActuatorPosition();
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
		channelSliderBars = new SeekBar[numberOfChannels];
		
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
		titleTable.setBackgroundResource(R.drawable.top_bar);
		titleTable.setId(1);

		//			table.setLayoutParams(new LayoutParams(displayWidth,(int) convertPixelsToDp(248, this)));

		final TextView tView = new TextView(this);
		//			tView.setPadding(15, 70, 0, 0);
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
			//				bottomButton.setMinimumHeight(120);
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
			//				bottomButton.setMinimumHeight(120);
			//				bottomButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
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
		TableLayout table = new TableLayout(this);
		
		for(int i = 0; i < numberOfChannels; i++){
			table.addView(controlView(i));
		}
		return table;
	}
	
	public RelativeLayout controlView(int channel){
		channel = channel +1;
		cView = new RelativeLayout(this);
		cView.setId(channel);
		
		channelTitle = new TextView(this);
		channelTitle.setId(channel+10);
		channelTitle.setText(channelNames[channel - 1]);
		channelTitle.setTextSize(20);
		channelTitle.setTextColor(Color.WHITE);
		channelTitle.setTypeface(font);
		channelTitle.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
		
		cView.addView(channelTitle);
		
		RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
		buttonParams.addRule(RelativeLayout.BELOW, channelTitle.getId());
		
		cView.addView(buttons(channel), buttonParams);
		
		RelativeLayout.LayoutParams sliderParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
		sliderParams.addRule(RelativeLayout.BELOW, 60+channel);
		
		cView.addView(channelSlider(channel), sliderParams);
		
		return cView;
	}
	
	public LinearLayout buttons(int channel){
		LinearLayout lLayout = new LinearLayout(this);
		lLayout.setOrientation(LinearLayout.HORIZONTAL);
		lLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		lLayout.setId(60 + channel);
		LinearLayout.LayoutParams ButtonParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
		lLayout.addView(inButton(channel), ButtonParams);
		lLayout.addView(outButton(channel), ButtonParams);
		lLayout.addView(stopButton(channel), ButtonParams);
		return lLayout;
	}
	
	public Button outButton(int channel){
		final int controlChannel = channel;
		System.out.println("building out button for channel "+controlChannel);
		outButton = new Button(this);
		outButton.setText("Out");
		outButton.setId(channel + 20);
//		outButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		outButton.setOnTouchListener(new View.OnTouchListener() {
			
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction() == MotionEvent.ACTION_DOWN){
					if(cPanel.moveActuatorOut(controlChannel, speed)){
						
					}else{
						showAlertDialog("Connection Lost");
					}
				}else{
					if(event.getAction() == MotionEvent.ACTION_UP){
						if(cPanel.stopActuator(controlChannel)){
							cPanel.getActuatorPosition();
						}else{
							showAlertDialog("Connection Lost");
						}
					}
				}
				return false;
			}
		});
		
		return outButton;
	}
	
	public Button inButton(int channel){
		final int controlChannel = channel;
		System.out.println("building in button for channel "+controlChannel);
		inButton = new Button(this);
		inButton.setText("In");
		inButton.setId(channel + 30);
//		inButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		inButton.setOnTouchListener(new View.OnTouchListener() {
			
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction() == MotionEvent.ACTION_DOWN){
					if(cPanel.moveActuatorIn(controlChannel, speed)){
						
					}else{
						showAlertDialog("Connection Lost");
					}
				}else{
					if(event.getAction() == MotionEvent.ACTION_UP){
						if(cPanel.stopActuator(controlChannel)){
							cPanel.getActuatorPosition();
						}else{
							showAlertDialog("Connection Lost");
						}
					}
				}
				return false;
			}
		});
		
		return inButton;
	}
	
	public Button stopButton(int channel){
		final int controlChannel = channel;
		Button stopButton = new Button(this);
		stopButton.setText("Stop");
		stopButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				if(cPanel.stopActuator(controlChannel)){
					cPanel.getActuatorPosition();
				}else{
					showAlertDialog("Connection Lost");
				}
				
				
			}
		});
		return stopButton;
	}
	
	public SeekBar channelSlider(int channel){
		
		int channelNumber = channel - 1;
		
		channelSliderBars[channelNumber] = new SeekBar(this);
		channelSliderBars[channelNumber].setId(channel + 40);
		channelSliderBars[channelNumber].setMax(1023);
		channelSliderBars[channelNumber].setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		channelSliderBars[channelNumber].setPadding(10, 0, 10, 10);
		channelSliderBars[channelNumber].setOnSeekBarChangeListener(new seekBarListener());
		
		return channelSliderBars[channelNumber];
	}
	
	private class seekBarListener implements OnSeekBarChangeListener{

		public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
			
		}

		public void onStartTrackingTouch(SeekBar bar) {
			
		}

		public void onStopTrackingTouch(SeekBar bar) {
			int channel = bar.getId()-40;
			int barStatus = bar.getProgress();
			
			int[] mlSB = msbLSB(barStatus);
			
			if(cPanel.setActuatorPosition(channel, mlSB[0], mlSB[1], speed)){
				//Command Executed
			}else{
				showAlertDialog("Connection Lost");
			}
			
		}
		
	}
	
	public void updateAllInuts(int[] values){
		
		if(values == null){
			System.out.println("values == null");
			return;
		}
		
		System.out.println("values length = "+values.length);
		if(values.length == 8){

			for(int i = 0; i < numberOfChannels; i++){
				int msbPos = 0;
				int lsbPos = 0;
				switch(i){
				case 0: msbPos = 0;
				lsbPos = 1;
				break;
				case 1: msbPos = 2;
				lsbPos = 3;
				break;
				case 2: msbPos = 4;
				lsbPos = 5;
				break;
				case 3: msbPos = 6;
				lsbPos = 7;
				break;
				case 4: msbPos = 8;
				lsbPos = 9;
				break;
				case 5: msbPos = 10;
				lsbPos = 11;
				break;
				case 6: msbPos = 12;
				lsbPos = 13;
				break;
				case 7: msbPos = 14;
				lsbPos = 15;
				break;
				}

				//					channelText[i].setText(iNames[i]+" : " + tenBit(values[msbPos], values[lsbPos]));
				channelSliderBars[i].setProgress(tenBit(values[msbPos], values[lsbPos]));


			}
		}else{
			System.out.println("somehting went wrong getting channel status");
		}

	}
	
	public int tenBit(int MSB, int LSB){
		int value = (MSB*256)+LSB;
		return value;
	}
	
	public int[] msbLSB(int status){
		int[] returnInts = new int[2];
		int MSB = status/256;
		int LSB = status - (MSB * 256);
		
		returnInts[0] = MSB;
		returnInts[1] = LSB;
		
		return returnInts;
	}
	
	public class MyGestureDetector extends SimpleOnGestureListener
    {
    	@Override
    	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY){
    		if(e1 != null && e2 != null){
    			if(e1.getX() > e2.getX()){
    				right = true;
    			}else{
    				right = false;
    			}
    			if(e1.getX()-e2.getX() > 200 || e2.getX()-e1.getX() > 200){
        			System.out.println("onFling returning true");
        			return true;
        		}
    		}
    		
    		System.out.println("onFling returning false");
			return false;
    		
    	}
    }
	
	public Handler inputStatusHandler()
	{
		Handler dHandler = new Handler(){
	            public void handleMessage(Message message) 
	            {
	            	System.out.println("inputStatusHander received a message");
	            	
	            	if(message.arg1 == Activity.RESULT_CANCELED){
	            		System.out.println("RESULT_CANCELED");
            			communicationError = true;
            			updateAllInuts(null);
            			return;
            		}
	            	
	            	if(winet){
	            		int[] data = (int[])message.obj;
	            		if(data.length == 9){
	            			communicationError = false;
	            			updateAllInuts((int[])message.obj);
	            			failureCount = 0;
	            		}else{
	            			communicationError = true;
	            			updateAllInuts(null);
	            		}
	            		return;
	            	}else{
	            		int[] data = (int[])message.obj;
	            		for(int i = 0; i < data.length; i++){
	            			if(data[i]<0){
	            				data[i] = data[i]+256;
	            			}
	            		}
	            		if(data[0] == 170 || winet){
	            			int[] dataToSend = new int[8];
	            			for(int i = 0; i < dataToSend.length; i++){
	            				dataToSend[i] = data[i+2];
	            			}
	            			communicationError = false;
	            			updateAllInuts(dataToSend);
	            			failureCount = 0;
	            		}
	            	}
	            	
	            		
	            	
	            }
		};
		return dHandler;
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
							sView.addView(controlView(currentChannel));
							Toast toast = Toast.makeText(getBaseContext(), "Connected", Toast.LENGTH_LONG);
							toast.show();

						}else{
							System.out.println("Could not connect.  Calling finddevice, sending: "+deviceMacAddress+" for mac");
							findDeviceIntent = new Intent(getApplicationContext(), FindDevice.class);
							findDeviceMessenger = new Messenger(findDeviceHandler());
							findDeviceIntent.setAction("Start");
							findDeviceIntent.putExtra("LOCATION", "local");
							findDeviceIntent.putExtra("MAC", deviceMacAddress);
							findDeviceIntent.putExtra("MESSENGER", findDeviceMessenger);
							startService(findDeviceIntent);
						}
					}
				});
				removeDeviceAlert.setNeutralButton("Remote", new DialogInterface.OnClickListener(){

					public void onClick(DialogInterface dialog, int which) {
						showProgressDialog("Getting Connection through Signal Switch");
						cPanel.disconnect();

						String wiNetMac = cPanel.getStoredString(deviceMacAddress+"-"+"wiNet-wiNetMac");
						if(!wiNetMac.equalsIgnoreCase("n/a")){

							findDeviceIntent = new Intent(getApplicationContext(), FindDevice.class);
							findDeviceMessenger = new Messenger(findDeviceHandler());
							findDeviceIntent.setAction("Start");
							findDeviceIntent.putExtra("LOCATION", "remote");
							findDeviceIntent.putExtra("MAC", wiNetMac);
							findDeviceIntent.putExtra("MESSENGER", findDeviceMessenger);
							startService(findDeviceIntent);

						}else{
							findDeviceIntent = new Intent(getApplicationContext(), FindDevice.class);
							findDeviceMessenger = new Messenger(findDeviceHandler());
							findDeviceIntent.setAction("Start");
							findDeviceIntent.putExtra("LOCATION", "remote");
							findDeviceIntent.putExtra("MAC", deviceMacAddress);
							findDeviceIntent.putExtra("MESSENGER", findDeviceMessenger);
							startService(findDeviceIntent);
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
							sView.addView(controlView(currentChannel));
							Toast toast = Toast.makeText(getBaseContext(), "Connected", Toast.LENGTH_LONG);
							toast.show();
						}else{
							findDeviceIntent = new Intent(getApplicationContext(), FindDevice.class);
							findDeviceMessenger = new Messenger(findDeviceHandler());
							findDeviceIntent.setAction("Start");
							findDeviceIntent.putExtra("LOCATION", "local");
							findDeviceIntent.putExtra("MAC", deviceMacAddress);
							findDeviceIntent.putExtra("MESSENGER", findDeviceMessenger);
							startService(findDeviceIntent);
						}
					}
				});
				removeDeviceAlert.setNeutralButton("Remote", new DialogInterface.OnClickListener(){

					public void onClick(DialogInterface dialog, int which) {
						showProgressDialog("Getting Connection through Signal Switch");
						cPanel.disconnect();

						String wiNetMac = cPanel.getStoredString(deviceMacAddress+"-"+"wiNet-wiNetMac");
						if(!wiNetMac.equalsIgnoreCase("n/a")){

							findDeviceIntent = new Intent(getApplicationContext(), FindDevice.class);
							findDeviceMessenger = new Messenger(findDeviceHandler());
							findDeviceIntent.setAction("Start");
							findDeviceIntent.putExtra("LOCATION", "remote");
							findDeviceIntent.putExtra("MAC", wiNetMac);
							findDeviceIntent.putExtra("MESSENGER", findDeviceMessenger);
							startService(findDeviceIntent);

						}else{
							findDeviceIntent = new Intent(getApplicationContext(), FindDevice.class);
							findDeviceMessenger = new Messenger(findDeviceHandler());
							findDeviceIntent.setAction("Start");
							findDeviceIntent.putExtra("LOCATION", "remote");
							findDeviceIntent.putExtra("MAC", deviceMacAddress);
							findDeviceIntent.putExtra("MESSENGER", findDeviceMessenger);
							startService(findDeviceIntent);
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
							sView.addView(controlView(currentChannel));
							Toast toast = Toast.makeText(getBaseContext(), "Connected", Toast.LENGTH_LONG);
							toast.show();
						}else{
							findDeviceIntent = new Intent(getApplicationContext(), FindDevice.class);
							findDeviceMessenger = new Messenger(findDeviceHandler());
							findDeviceIntent.setAction("Start");
							findDeviceIntent.putExtra("LOCATION", "local");
							findDeviceIntent.putExtra("MAC", deviceMacAddress);
							findDeviceIntent.putExtra("MESSENGER", findDeviceMessenger);
							startService(findDeviceIntent);
						}
					}
				});
				removeDeviceAlert.setNeutralButton("Remote", new DialogInterface.OnClickListener(){

					public void onClick(DialogInterface dialog, int which) {
						showProgressDialog("Getting Connection through Signal Switch");
						cPanel.disconnect();

						String wiNetMac = cPanel.getStoredString(deviceMacAddress+"-"+"wiNet-wiNetMac");
						if(!wiNetMac.equalsIgnoreCase("n/a")){

							findDeviceIntent = new Intent(getApplicationContext(), FindDevice.class);
							findDeviceMessenger = new Messenger(findDeviceHandler());
							findDeviceIntent.setAction("Start");
							findDeviceIntent.putExtra("LOCATION", "remote");
							findDeviceIntent.putExtra("MAC", wiNetMac);
							findDeviceIntent.putExtra("MESSENGER", findDeviceMessenger);
							startService(findDeviceIntent);

						}else{
							findDeviceIntent = new Intent(getApplicationContext(), FindDevice.class);
							findDeviceMessenger = new Messenger(findDeviceHandler());
							findDeviceIntent.setAction("Start");
							findDeviceIntent.putExtra("LOCATION", "remote");
							findDeviceIntent.putExtra("MAC", deviceMacAddress);
							findDeviceIntent.putExtra("MESSENGER", findDeviceMessenger);
							startService(findDeviceIntent);
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
							sView.addView(controlView(currentChannel));
							Toast toast = Toast.makeText(getBaseContext(), "Connected", Toast.LENGTH_LONG);
							toast.show();
						}else{
							findDeviceIntent = new Intent(getApplicationContext(), FindDevice.class);
							findDeviceMessenger = new Messenger(findDeviceHandler());
							findDeviceIntent.setAction("Start");
							findDeviceIntent.putExtra("LOCATION", "local");
							findDeviceIntent.putExtra("MAC", deviceMacAddress);
							findDeviceIntent.putExtra("MESSENGER", findDeviceMessenger);
							startService(findDeviceIntent);
						}
					}
				});
				removeDeviceAlert.setNeutralButton("Remote", new DialogInterface.OnClickListener(){

					public void onClick(DialogInterface dialog, int which) {
						showProgressDialog("Getting Connection through Signal Switch");
						cPanel.disconnect();

						String wiNetMac = cPanel.getStoredString(deviceMacAddress+"-"+"wiNet-wiNetMac");
						if(!wiNetMac.equalsIgnoreCase("n/a")){

							findDeviceIntent = new Intent(getApplicationContext(), FindDevice.class);
							findDeviceMessenger = new Messenger(findDeviceHandler());
							findDeviceIntent.setAction("Start");
							findDeviceIntent.putExtra("LOCATION", "remote");
							findDeviceIntent.putExtra("MAC", wiNetMac);
							findDeviceIntent.putExtra("MESSENGER", findDeviceMessenger);
							startService(findDeviceIntent);

						}else{
							findDeviceIntent = new Intent(getApplicationContext(), FindDevice.class);
							findDeviceMessenger = new Messenger(findDeviceHandler());
							findDeviceIntent.setAction("Start");
							findDeviceIntent.putExtra("LOCATION", "remote");
							findDeviceIntent.putExtra("MAC", deviceMacAddress);
							findDeviceIntent.putExtra("MESSENGER", findDeviceMessenger);
							startService(findDeviceIntent);
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
    				sView.addView(controlView(currentChannel));
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
