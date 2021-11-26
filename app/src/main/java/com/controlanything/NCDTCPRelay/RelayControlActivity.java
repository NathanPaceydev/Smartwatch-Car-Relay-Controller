package com.controlanything.NCDTCPRelay;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;

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
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

public class RelayControlActivity extends Activity{
	
	ControlPanel cPanel;
	Class<? extends Intent> callingClass;
	
	Intent i;
	String deviceMacAddress;
	String[] relayNames;
	int[] momentaryIntArray; 
	int numberOfRelays;
	String deviceName;
	String defaultIP;
	int port;
	
	Typeface font;
	AnimationDrawable saveButtonAnimation;
	
	//Info on Device
	int currentapiVersion = android.os.Build.VERSION.SDK_INT;
	
	ImageButton[] relayButtons;
	TextView[] relayLabels;
	RelativeLayout titleTable;
	ImageView bottomButton;
	ImageView speechButton;
	public int[] relayStatusArray;
	public TextView tvSocketConnection;
	TextView bText;
	int textColor = Color.WHITE;
	int subTextSize = 20;
	
	AlertDialog lostConnectionDialog;
	ProgressDialog progressDialog;
	
	Intent findDeviceIntent;
	Messenger findDeviceMessenger;
	
	ScrollView sView;
	
	GestureDetector gDetector;
	boolean displayInputs;
	boolean displayMacros;
	boolean fusion = false;
	int[] currentBankStatus = {0,0,0,0,0,0,0,0};
	
	//Haptic feedback
	private Vibrator myVib;
	
	//Bluetooth connection objects
	BluetoothAdapter btAdapter;
	BluetoothDevice btDevice;
	android.bluetooth.BluetoothSocket btSocket;
	String btDeviceAddress;
	boolean bluetooth;
	boolean winet;
	boolean switchToADActivity = false;
	boolean switchToMacrosActivity = false;

	//private ActivityMainBinding binding;
	private static final int SPEECH_RECOGNIZER_REQUEST_CODE = 0;
	private float tipPercent = .15f;

	// method to show Toast messages on the main UI thread instead of locally
	public void showToast(final String message) {
		final Handler mHandler = new Handler(); // set Handler

		new Thread(new Runnable() { // add new thread
			@Override
			public void run () {
				// Perform long-running task here
				// (like audio buffering).
				// You may want to update a progress
				// bar every second, so use a handler:

				mHandler.post(new Runnable() {
					@Override
					public void run () {
						// make the Toast
						Toast newToast =Toast.makeText(RelayControlActivity.this, message.toString(),Toast.LENGTH_LONG);
						newToast.show();
					}
				});
			}
		}).start();
	}


	private void startSpeech() {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		startActivityForResult(intent, SPEECH_RECOGNIZER_REQUEST_CODE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode,resultCode, data);


		if (requestCode == SPEECH_RECOGNIZER_REQUEST_CODE){
			if(resultCode == RESULT_OK){
				List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
				String recognizeTxt = results.get(0);

				System.out.println("You Said: "+recognizeTxt);
				relaySpeechControl(recognizeTxt);


			}
		}
	}

	// method to activate the relay from the speech text
	public void relaySpeechControl(String speechText){

		//System.out.println(speechText); // test print
		speechText = speechText.toLowerCase();
		//System.out.println(relayNames[0].toString());
		int relayNumberActivated = -1;

		// loop through the relay names and se if the speech text contains the element
		for(int i =0; i<relayNames.length; i++){
			//System.out.println(relayNames[i]);

			// convert to lowercase for compare
			if(speechText.contains(relayNames[i].toString().toLowerCase())){
				System.out.println("Speech Matches: "+relayNames[i]);
				String relayActivated = relayNames[i]; // activated features
				relayNumberActivated = i;

				// print the activated relay
				showToast(relayActivated+" Activated");
				clickRelay(relayNumberActivated, 1);

				//return; // break the loop

			}
		}
		if (relayNumberActivated == -1) {
			showToast("Relays Not Activated\nUse Relay Names to Activate");
		}

	}




	// method called for speech recogintion -> assumes click relay
	public boolean clickRelay(final int relayNumber, final int bankNumber)
	{

		if (momentaryIntArray[relayNumber] == 0) {
			if (relayStatusArray[relayNumber] == 0) {
				int[] returnedStatus = (cPanel.TurnOnRelayFusion((relayNumber - ((bankNumber-1)*8)), bankNumber));
				if(returnedStatus[0] != 260){
					myVib.vibrate(50);

					updateButtonTextFusion(bankNumber, returnedStatus);
				}else{
					changeTitleToRed();
				}
			}
			else {
				int[] returnedStatus = (cPanel.TurnOnRelayFusion((relayNumber - ((bankNumber-1)*8)), bankNumber));

				if(returnedStatus[0] != 260){
					myVib.vibrate(50);
					updateButtonTextFusion(bankNumber, returnedStatus);
				}
				else{
					changeTitleToRed();
				}
			}

		}
		else{
				int[] returnedStatus = (cPanel.TurnOnRelayFusion((relayNumber - ((bankNumber-1)*8)), bankNumber));
				if(returnedStatus[0] != 260){
					myVib.vibrate(50);
					updateButtonTextFusion(bankNumber, returnedStatus);
				}
				else{
					changeTitleToRed();
				}

		}
				return false;

	}




	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// might not work LOL
		setContentView(R.layout.relay_control);

		cPanel = ((ControlPanel)getApplicationContext());
		gDetector = new GestureDetector(getApplicationContext(), new MyGestureDetector());
		myVib = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
		font = Typeface.createFromAsset(getAssets(), "fonts/neuropolxfree.ttf");
		
		System.out.println("RelayControlActivity ID = "+this.toString());
		
		i = getIntent();
		callingClass = i.getClass();
		System.out.println("Calling Class = "+callingClass.toString());
		deviceMacAddress = i.getStringExtra("MAC");

		/*
		binding = ActivityMainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		System.out.println(binding.text);
		 */
	}
	
	public void getDeviceInfo(){
		//Get device settings for its name and number of relays.
		String[] deviceSettings = cPanel.getStoredString(deviceMacAddress).split(";");
		if(deviceSettings[2].equalsIgnoreCase("Bluetooth")){
			bluetooth = true;
			btDeviceAddress = deviceMacAddress;
		}else{
			bluetooth = false;
		}
		numberOfRelays = Integer.parseInt(deviceSettings[3]);
		deviceName = deviceSettings[4];
		if(!bluetooth){
			defaultIP = deviceSettings[1];
			port = Integer.parseInt(deviceSettings[2]);
		}
		
		
		relayButtons = new ImageButton[numberOfRelays];
		relayLabels = new TextView[numberOfRelays];
		relayStatusArray = new int [numberOfRelays];
		
		//Get Relay Names
		relayNames = cPanel.getStoredString(deviceMacAddress+"Names").split(";");
		
		//Get button momentary or not.
		String[] momentaryString = cPanel.getStoredString(deviceMacAddress+"Momentary").split(";");
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
	
	public RelativeLayout mainViewTable(){

		// call the empty relative layout
		setContentView(R.layout.relay_control);
		RelativeLayout mTable = (RelativeLayout) findViewById(R.id.control_layout); // assign to mTable
//		mTable.setBackgroundResource(R.drawable.background);
		mTable.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		mTable.addView(title());

				
		RelativeLayout.LayoutParams bottomButtonParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, 100);
		bottomButtonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

		ImageView listButton = deviceListButton();
		mTable.addView(listButton, bottomButtonParams);


		
		if(displayInputs || displayMacros){
			RelativeLayout.LayoutParams bottomTextParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
			bottomTextParams.addRule(RelativeLayout.ABOVE, bottomButton.getId());
			if(displayInputs){
				mTable.addView(bottomText("Swipe left to display Inputs Page"), bottomTextParams);
			}else{
				mTable.addView(bottomText("Swipe left to display Macros Page"), bottomTextParams);
			}
		}
	
		RelativeLayout.LayoutParams scrollViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
		scrollViewParams.addRule(RelativeLayout.ABOVE, titleTable.getId());
		if(displayInputs || displayMacros){
			scrollViewParams.addRule(RelativeLayout.ABOVE, bText.getId());

		}else{
			scrollViewParams.addRule(RelativeLayout.ABOVE, bottomButton.getId());
		}
		mTable.addView(scrollView(), scrollViewParams);

		//		** new speech button layout **
		RelativeLayout.LayoutParams speechButtonParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, 70);
		speechButtonParams.addRule(RelativeLayout.ABOVE, scrollView().getId());
		//speechButtonParams.addRule(RelativeLayout.ABOVE,listButton.getId() );
		mTable.addView(speechControlButton(), speechButtonParams);


		return mTable;
	}


	public RelativeLayout title(){
		titleTable = new RelativeLayout(this);
		//titleTable.setBackgroundResource(R.drawable.top_bar);
		titleTable.setId(1);

//		table.setLayoutParams(new LayoutParams(displayWidth,(int) convertPixelsToDp(248, this)));

		RelativeLayout.LayoutParams titleLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		
		titleLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);

		//titleTable.addView(speechButton,titleLayoutParams);
		
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
		sView.setPadding(32, 70, 16, 0);

		sView.addView(controlsTable());
		
		if(displayInputs || displayMacros){
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
						showProgressDialog("Loading");
						startActivity(adIntent);
						dismissProgressDialog();
						return true;
						}
						if(displayMacros){
							switchToMacrosActivity = true;
							System.out.println("Switching to Macros Activity");

							Intent macroActivityIntent = new Intent(getApplicationContext(), MacroActivity.class);
							macroActivityIntent.setAction("Start");
							macroActivityIntent.putExtra("MAC", deviceMacAddress);
							showProgressDialog("Loading");
							startActivity(macroActivityIntent);
							dismissProgressDialog();
							return true;

						}
					}
					return false;
				}
			});
		}

		return sView;
	}

	
	public ImageView deviceListButton(){
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);

		bottomButton = new ImageView(this);
		bottomButton.setId(2);
		int buttonWidth = (int) (metrics.widthPixels)/4;
		int buttonHeight = (int) (metrics.widthPixels)/8;
		
		if(currentapiVersion>=11){
			bottomButton.setImageResource(R.drawable.animationxmldevicelist);
			//bottomButton.setBackgroundResource(0);
//			bottomButton.setMinimumHeight(120);
			bottomButton.setLayoutParams(new LayoutParams(buttonWidth, buttonHeight));
			//bottomButton.setBackgroundResource(R.drawable.bottom_bar);

            saveButtonAnimation = (AnimationDrawable)bottomButton.getDrawable();
            saveButtonAnimation.setEnterFadeDuration(1000);
            saveButtonAnimation.setExitFadeDuration(1000);
			
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
			//bottomButton.setBackgroundResource(0);
//			bottomButton.setMinimumHeight(120);
			bottomButton.setLayoutParams(new LayoutParams(buttonHeight, buttonHeight));
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


	// ** new speech control Method **
	public ImageView speechControlButton(){
		speechButton = new ImageView(this);

		speechButton.setId(4);
		//speechButton.setLayoutParams(new LayoutParams(60, 60));

		speechButton.setClickable(true);
		speechButton.setPadding(0,8,0,8);


		if(currentapiVersion >=11) {
			speechButton.setImageResource(R.drawable.google_mic);

			speechButton.setOnClickListener(new OnClickListener(){
				public void onClick(View v) {
					// set the class / activity to be called
					startSpeech();
				}

			});


		}

		return speechButton;
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
			
			fusion = cPanel.checkFusion();
		}else{
			fusion = cPanel.checkFusion();
		}
		
		for(int i = 0; i<numberOfRelays; i++){
			LinearLayout relayControlRow = new LinearLayout(this);
			relayControlRow.setOrientation(LinearLayout.HORIZONTAL);
			
			relayButtons[i] = new ImageButton(this);

			relayLabels[i] = new TextView(this);

			relayControlRow.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.MATCH_PARENT));
			
			// setting the relay labels
			relayLabels[i].setText(relayNames[i]);
			// setting the text size
			relayLabels[i].setTextSize(16);
			relayLabels[i].setTextColor(Color.WHITE);
			relayLabels[i].setGravity(Gravity.CENTER);

			// set the padding for watchface
			relayLabels[i].setPadding(0, -100, 0, -100);

			relayLabels[i].setTypeface(font);
			relayLabels[i].setMaxLines(1);
			relayLabels[i].setLineSpacing(-1000, -100); // reduce the line spacing
			relayLabels[i].setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.MATCH_PARENT));
			
			
			
			if (i <= 7)
    		{
				relayButtons[i] = newButton(i, 1);
    		}
    		else
    		{
    			if (i <= 15)
    			{
    				relayButtons[i] = newButton(i, 2);
    			}
    			else
    			{
    				if (i <= 23)
    				{
    					relayButtons[i] = newButton(i, 3);
    				}
    				else
    				{
    					relayButtons[i] = newButton(i, 4);
    				}
    			}
    		}
			relayButtons[i].setId(i);

    		// ** scale the relay buttons **
    		relayButtons[i].setLayoutParams(new LayoutParams(120,80));
    		relayButtons[i].setPadding(0,0,0,0);

			relayControlRow.addView(relayButtons[i]);
			relayControlRow.addView(relayLabels[i]);


			cTable.addView(relayControlRow);
			
		}
		
		if (numberOfRelays <= 8)
		{
			updateButtonText(1);
		}
		else
		{
			if (numberOfRelays == 16)
			{
				updateButtonText(1);
				updateButtonText(2);
			}
			else
			{
				if (numberOfRelays == 24)
				{
					updateButtonText(1);
					updateButtonText(2);
					updateButtonText(3);
				}
				else
				{
					updateButtonText(1);
					updateButtonText(2);
					updateButtonText(3);
					updateButtonText(4);
				}
			}
		}
		
		return cTable;
		
	}
	
	public ImageButton newButton(final int relayNumber, final int bankNumber){
		final ImageButton relayButton = new ImageButton(this);
  		relayButton.setAdjustViewBounds(true);
  		setContentView(R.layout.relay_button);
  		relayButton.setImageResource(R.drawable.button_dead);
  		relayButton.setBackgroundResource(0);

  		//Fusion
  		if(fusion){
  			System.out.println("Fusion Button");
  			
  			if (momentaryIntArray[relayNumber] == 0)
			{
				relayButton.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {

						if (relayStatusArray[relayNumber] == 0)
						{
							int[] returnedStatus = (cPanel.TurnOnRelayFusion((relayNumber - ((bankNumber-1)*8)), bankNumber));
							if(returnedStatus[0] != 260){
								myVib.vibrate(50);
								
								updateButtonTextFusion(bankNumber, returnedStatus);
							}else{
								changeTitleToRed();
							}
						}
						else {
							int[] returnedStatus = (cPanel.TurnOffRelayFusion((relayNumber - ((bankNumber-1)*8)), bankNumber));
							if(returnedStatus[0] != 260){
								myVib.vibrate(50);
								updateButtonTextFusion(bankNumber, returnedStatus);
							}else{
								changeTitleToRed();
							}
						}
						
					}
				});


				
			}
			else
			{
				relayButton.setOnTouchListener(new View.OnTouchListener()
				{
					public boolean onTouch(View v, MotionEvent event) {
												
						if (event.getAction() == MotionEvent.ACTION_DOWN){
							int[] returnedStatus = (cPanel.TurnOnRelayFusion((relayNumber + ((bankNumber-1)*8)), bankNumber));
							if(returnedStatus[0] != 260){
								myVib.vibrate(50);
								updateButtonTextFusion(bankNumber, returnedStatus);
							}else{
								changeTitleToRed();
							}
						}
						else if (event.getAction() == MotionEvent.ACTION_UP)
						{
							int[] returnedStatus = (cPanel.TurnOffRelayFusion((relayNumber - ((bankNumber-1)*8)), bankNumber));
							if(returnedStatus[0] != 260){
								myVib.vibrate(50);
								updateButtonTextFusion(bankNumber, returnedStatus);
							}else{
								changeTitleToRed();
							}
	  					
	  					}
						return false;
					}
	  			});
			}
  		}
  		//Not Fusion
  		else{
  			if (momentaryIntArray[relayNumber] == 0)
			{
				relayButton.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {

						if (relayStatusArray[relayNumber] == 0) 
						{
							if (cPanel.TurnOnRelay((relayNumber - ((bankNumber-1)*8)), bankNumber) == false)
							{
								
								relayButton.setEnabled(false);
								changeTitleToRed();								
								
							}else{
								myVib.vibrate(50);
								changeTitleToGreen();
								updateButtonText(bankNumber);
							}
						}
						else {
							if (cPanel.TurnOffRelay((relayNumber - ((bankNumber-1)*8)), bankNumber) == true){
								myVib.vibrate(50);
								changeTitleToGreen();
								updateButtonText(bankNumber);
								
							}else{
								relayButton.setEnabled(false);
								changeTitleToRed();								
							}
						}
						
					}
				});


				
			}
			else
			{
				relayButton.setOnTouchListener(new View.OnTouchListener()
				{
					public boolean onTouch(View v, MotionEvent event) {
												
						if (event.getAction() == MotionEvent.ACTION_DOWN){
							if (cPanel.TurnOnRelay((relayNumber - ((bankNumber-1)*8)), bankNumber) == true)
							{
								myVib.vibrate(50);
								changeTitleToGreen();
								updateButtonText(bankNumber);
							}
							else{
								relayButton.setEnabled(false);
								changeTitleToRed();								
							}
						}
						else if (event.getAction() == MotionEvent.ACTION_UP)
						{
							if (cPanel.TurnOffRelay((relayNumber - ((bankNumber-1)*8)), bankNumber) == true)
	  						{
								myVib.vibrate(50);
								changeTitleToGreen();
								updateButtonText(bankNumber);
	  						}else{
	  							relayButton.setEnabled(false);
	  							changeTitleToRed();
	  						}
	  					
	  					}
						return false;
					}
	  			});
			}
  		}

  		
  		return relayButton;
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(cPanel.connected == true){
			if(!switchToADActivity || switchToMacrosActivity){
				cPanel.disconnect();
			}
			
		}else{
			System.out.println("cPanel.connected == false");
		}
		cPanel.connected = false;
		//finish();
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		tvSocketConnection = new TextView(this);
		getDeviceInfo();
		setContentView(mainViewTable());
		System.out.println("Bottom Button Height"+bottomButton.getHeight());
		
		if(currentapiVersion>=11){
			saveButtonAnimation.start();
		}
		
		String[] deviceInfo = cPanel.getStoredString(deviceMacAddress).split(";");
		if(deviceInfo[2].equalsIgnoreCase("Bluetooth")){
			bluetooth = true;
		}else{
			bluetooth = false;
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
		}else{
//			fusion = cPanel.checkFusion();
		}
		System.out.println("Bottom Button Height"+bottomButton.getHeight());
	}
	
	private void updateButtonText(int bankNumber) {
		int[] returnStatus = cPanel.getBankStatus(bankNumber);
		System.out.println("Return status = "+Arrays.toString(returnStatus));
		if(returnStatus != null){
			
			if (numberOfRelays < 8){
				
				for(int i = 0; i < numberOfRelays; i++){
					relayStatusArray[i+((bankNumber-1)*8)] = returnStatus[i];
				}
				
				for (int i = 0; i < numberOfRelays; i++) {

					if (relayStatusArray[i+((bankNumber-1)*8)] != 0){
						relayButtons[i + ((bankNumber-1)*8)].setImageResource(R.drawable.blue_button_no_glow);

					}
					else {
						relayButtons[i + ((bankNumber-1)*8)].setImageResource(R.drawable.button_dead);
					}
				}
			}
			else {
				for(int i = 0; i < 8; i++){
					relayStatusArray[i+((bankNumber-1)*8)] = returnStatus[i];
				}
				for (int i = 0; i < 8; i++) {

					if (relayStatusArray[i+((bankNumber-1)*8)] != 0){
						relayButtons[i + ((bankNumber-1)*8)].setImageResource(R.drawable.blue_button_no_glow);
					}
					else {
						relayButtons[i + ((bankNumber-1)*8)].setImageResource(R.drawable.button_dead);
					}
				}
			}
		}else{
			changeTitleToRed();
		}
	}
	
	private void updateButtonTextFusion(int bankNumber, int[] status) {
		if(status != null){
			
			if(numberOfRelays < 8){
				
				for(int i = 0; i < numberOfRelays; i++){
					relayStatusArray[i+((bankNumber-1)*8)] = status[i];
				}
				
				for (int i = 0; i < numberOfRelays; i++) {

					if (relayStatusArray[i+((bankNumber-1)*8)] != 0){
						relayButtons[i + ((bankNumber-1)*8)].setImageResource(R.drawable.blue_button_no_glow);

					}
					else {
						relayButtons[i + ((bankNumber-1)*8)].setImageResource(R.drawable.button_dead);
					}
				}
				
			}else{
				
				for(int i = 0; i < 8; i++){
					relayStatusArray[i+((bankNumber-1)*8)] = status[i];
				}
				
				for (int i = 0; i < 8; i++) {

					if (relayStatusArray[i+((bankNumber-1)*8)] != 0){
						relayButtons[i + ((bankNumber-1)*8)].setImageResource(R.drawable.blue_button_no_glow);

					}
					else {
						relayButtons[i + ((bankNumber-1)*8)].setImageResource(R.drawable.button_dead);
					}
				}
				
			}

				System.out.println("Current relay's status "+Arrays.toString(relayStatusArray));
		}else{
			changeTitleToRed();
		}
	}
	
	public void changeTitleToRed(){
    	showAlertDialog("Connection Lost");
    	
    }
    
    public void changeTitleToGreen(){
    	tvSocketConnection.setBackgroundColor(Color.GREEN);
    	tvSocketConnection.setText("NCD TCP Relay: Connected");
    	
    }
    
    public void changeTitleToYellow(){
    	tvSocketConnection.setBackgroundColor(Color.YELLOW);
    	tvSocketConnection.setText("NCD TCP Relay: Connecting....");
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
					System.out.println("Passing "+btDeviceAddress+" as bt address");
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
    
    public class MyGestureDetector extends SimpleOnGestureListener
    {
    	@Override
    	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY){
    		if(e1 != null && e2 != null){
    			if(e1.getX()-e2.getX() > 200){
        			System.out.println("onFling returning true");
        			return true;
        		}
    		}
    		
    		System.out.println("onFling returning false");
			return false;
    		
    	}
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
