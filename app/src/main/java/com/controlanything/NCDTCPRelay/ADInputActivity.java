package com.controlanything.NCDTCPRelay;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.AnimationDrawable;
import android.text.format.Time;
import android.view.Display;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;


// ** activity when Network Device Selected ***
public class ADInputActivity extends Activity {
	
	TextView readTimeTextView;
	
	RelativeLayout masterTable;
	ImageView bottomButton;
	TextView bText;
	TextView[] channelText;
	ProgressBar[] pBarArray;
	TableLayout[] inputTables;
	RelativeLayout titleTable;
	TableLayout sTable;
	Intent readInput;
	Messenger readInputMessenger;
	int textColor = Color.WHITE;
	int subTextSize = 20;
	
	Intent readADInput;
	Messenger readADInputMessenger;
	
	Intent callingIntent;
	
	//Static Set for testing purposes
	String ip = "192.168.2.9";
	String defaultIP;
	int port = 2101;
	String mac;
	String deviceName;
	int channel = 0;
	int displayWidth;
	int displayHeight;
	Typeface font;
	AnimationDrawable saveButtonAnimation;
	int currentapiVersion = android.os.Build.VERSION.SDK_INT;
	boolean communicationError = false;
	int failureCount = 0;
	Intent findDeviceIntent;
	ProgressDialog progressDialog;
	AlertDialog lostConnectionDialog;
	Messenger findDeviceMessenger;
	
	//variables for battery charger meter
	int readCounts = 0;
	int readSums = 0;
	
	String[] iNames;
	
	Socket s;
	InetSocketAddress sAddress;
	OutputStream oStream;
	InputStream iStream;
	ControlPanel cPanel;
	Class callingClass;
	
	boolean continueReading;
	int adValue;
	boolean displayRelays;
	boolean displayMacros;
	
	int counter = 0;
	long previousTime;
	long duration;
	long currentTime;
	long averageTime;
	long totalTime;
	
	double vRef = 5.000000;
	double voltsPerStep = 4.926/1024.000000;
	
	int contactCount = 0;
	boolean closurePresent = false;
	
	ArrayList<byte[]> commandBuffer;
	int[] inputBuffer;
	
	int[] inputValueType;
	
	static int noDisplay = 0;
	static int raw10Bit = 1;
	static int voltage = 2;
	static int resistance = 3;
	static int closureInput = 4;
	static int temperature495F = 5;
	static int temperature495C = 6;
	static int temperature317F = 7;
	static int temperature317C = 8;
	static int temperature4952172F = 9;
	static int temperature4952172C = 10;
	static int pdvP8001 = 11;
	static int currentH722 = 12;
	static int currentH822 = 13;
	
	GestureDetector gDetector;
	
	//Bluetooth connection objects
	BluetoothAdapter btAdapter;
	BluetoothDevice btDevice;
	android.bluetooth.BluetoothSocket btSocket;
	String btDeviceAddress;
	boolean bluetooth;
	
	//WiNet stuff
	boolean winet;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		gDetector = new GestureDetector(getApplicationContext(), new MyGestureDetector());
		
		Messenger cPanelMessenger = new Messenger(inputStatusHandler());
		
		cPanel = ((ControlPanel)getApplicationContext());
		
		cPanel.setMessenger(cPanelMessenger);
		
		callingIntent = getIntent();
		
		callingClass = callingIntent.getClass();
		
		if(callingIntent.getAction() == Intent.ACTION_MAIN){
			
		}else{
			Bundle extras = callingIntent.getExtras();
			if(extras.getString("BLUETOOTHADDRESS")!= null){ 
				bluetooth = true;
				btDeviceAddress = extras.getString("BLUETOOTHADDRESS");
				mac = btDeviceAddress;
				cPanel.bluetooth = true;
			}else{
				ip = extras.getString("IP");
				port = extras.getInt("PORT");
				mac = extras.getString("MAC");
				cPanel.bluetooth = false;
			}
			
		}

		// variable main activity generated based on the number of assets
		commandBuffer = new ArrayList<byte[]>();
		inputBuffer = new int[19];
		channelText = new TextView[8];
		pBarArray = new ProgressBar[8];
		inputTables = new TableLayout[8];
		font = Typeface.createFromAsset(getAssets(), "fonts/neuropolxfree.ttf");

		for(int i = 0; i < channelText.length; i++){
			channelText[i] = new TextView(this);
			channelText[i].setText("...");
//			channelText[i].setTypeface(font);
			channelText[i].setTextColor(Color.WHITE);
			channelText[i].setTextSize(20);
			channelText[i].setPadding(10, 0, 0, 10);
			pBarArray[i] = new ProgressBar(getApplicationContext(), null, android.R.attr.progressBarStyleHorizontal);
			pBarArray[i].setPadding(10, 0, 10, 0);
			pBarArray[i].getProgressDrawable().setColorFilter(0xffAEEEff, Mode.MULTIPLY);
			inputTables[i] = new TableLayout(this);
			inputTables[i].setBackgroundResource(R.drawable.textbox);
			inputTables[i].setPadding(10, 40, 10, 40);
			if(currentapiVersion>=11){
				inputTables[i].setAlpha(50);
			}
			
			
		}


		iNames = new String[8];
		inputValueType = new int[8];
		
		Display display = getWindowManager().getDefaultDisplay(); 
		displayWidth = display.getWidth();  // deprecated
		displayHeight = display.getHeight();
		displayHeight = displayHeight - getStatusBarHeight();
		
		
		
		getStoredInfo();
		
		
		
		setContentView(mainTable());
		
		if(currentapiVersion>=11){
			saveButtonAnimation.start();
		}
	}
	
	public void getStoredInfo(){
		
		String[] deviceSettings = cPanel.getStoredString(mac).split(";");
		if(!bluetooth){
			defaultIP = deviceSettings[1];
		}
		deviceName = deviceSettings[4];
		
		
		
		if(deviceSettings[7].equalsIgnoreCase("true")){
			displayRelays = true;
			
		}else{
			displayRelays = false;
		}
		System.out.println("Display Relays = "+displayRelays);
		String storedString = cPanel.getStoredString(mac+"Inputs");
		String[] storedStringSplit = storedString.split(";");
		if(storedString!="n/a"){
			for(int i = 0; i < 8; i++){
				String[] tempSplit = storedStringSplit[i].split("~");
				iNames[i] = tempSplit[0];
				inputValueType[i] = Integer.parseInt(tempSplit[1]);
				vRef = Double.parseDouble(storedStringSplit[8]);
			}
		}else{
			for(int i = 0; i<8;i++){
				iNames[i] = "Input"+(i+1);
				inputValueType[i] = 1;
				vRef = 5;
			}
		}
		//Check if device is winet
		if(deviceSettings.length > 8){
			if(deviceSettings[8].equalsIgnoreCase("true")){
				winet = true;
				cPanel.winet = true;
			}else{
				winet = false;
				cPanel.winet = false;
			}
		}else{
			winet = false;
			cPanel.winet = false;
		}
		displayMacros = deviceSettings[11].equalsIgnoreCase("true");
		
		voltsPerStep = vRef/1024.000000;
	}
	
	public RelativeLayout mainTable(){
		masterTable = new RelativeLayout(this);
		masterTable.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		masterTable.setBackgroundResource(R.drawable.background);
		
		masterTable.addView(title());
		
		//Set layout rules for bottom button
		RelativeLayout.LayoutParams bottomButtonParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 171);
		bottomButtonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		//Add button to master view
		masterTable.addView(deviceListButton(), bottomButtonParams);
		
		if(displayMacros && displayRelays){
			RelativeLayout.LayoutParams bottomTextParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
			bottomTextParams.addRule(RelativeLayout.ABOVE, bottomButton.getId());
				masterTable.addView(bottomText("Swipe right to display Relays Page Left to display Macros"), bottomTextParams);
		}else if(displayMacros || displayRelays){
			RelativeLayout.LayoutParams bottomTextParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
			bottomTextParams.addRule(RelativeLayout.ABOVE, bottomButton.getId());
			if(displayMacros){
				masterTable.addView(bottomText("Swipe Left to display Macros Page"), bottomTextParams);
			}else{
				masterTable.addView(bottomText("Swipe Right to display Relays Page"), bottomTextParams);
			}
		}
		
		//Set layout rules for ScrollView
		RelativeLayout.LayoutParams scrollViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		scrollViewParams.addRule(RelativeLayout.BELOW, titleTable.getId());
		if(displayRelays || displayMacros){
			scrollViewParams.addRule(RelativeLayout.ABOVE, bText.getId());
		}else{
			scrollViewParams.addRule(RelativeLayout.ABOVE, bottomButton.getId());
		}
		
		
		masterTable.addView(scrollView(), scrollViewParams);
		
		return masterTable;
	}
	
	public TextView bottomText(String message){
		bText = new TextView(this);
		bText.setId(3);
		bText.setText(message);
		bText.setTextColor(textColor);
		bText.setTextSize(this.subTextSize);
		return bText;
	}
	
	public void redrawView(){
		masterTable.removeView(sTable);
		masterTable.removeView(bottomButton);
		masterTable.addView(scrollView());
		masterTable.addView(deviceListButton());
	}
	
	public ScrollView scrollView(){
		ScrollView sView = new ScrollView(this);
		
		sView.addView(scrollTable());
		if(displayRelays || displayMacros){
			sView.setOnTouchListener(new View.OnTouchListener() {
				
				public boolean onTouch(View v, MotionEvent event) {
					gDetector.onTouchEvent(event);
					return true;

				}
			});
		}
		
		
		
		return sView;
	}


	// ****** Bottom LIST ********
	public ImageView deviceListButton(){
		bottomButton = new ImageView(this);
		bottomButton.setId(2);
		
		if(currentapiVersion>=11){
			bottomButton.setImageResource(R.drawable.animationxmldevicelist);
			
			bottomButton.setBackgroundResource(0);
			bottomButton.setMinimumHeight(120);
			bottomButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
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
			bottomButton.setBackgroundResource(0);
			bottomButton.setMinimumHeight(120);
			bottomButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 171));
			//bottomButton.setBackgroundResource(R.drawable.bottom_bar);
			
			bottomButton.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					Intent listView = new Intent(getApplicationContext(), DeviceListActivity.class);
					startActivity(listView);
					if(cPanel.connected == true){
						cPanel.disconnect();
					}else{

					}
					finish();
					
				}
				
			});
		}
		
		
		
		return bottomButton;
	}
	
	public TableLayout scrollTable(){
		sTable = new TableLayout(this);
		sTable.setPadding(20, 10, 20, 10);
		sTable.setBackgroundResource(R.drawable.background);
		
		
		
		readTimeTextView = new TextView(this);
		readTimeTextView.setText("...");
		
		sTable.addView(readTimeTextView);
		
		for(int i = 0; i < channelText.length; i++){
//			TableRow tRow = new TableRow(this);
			if(inputTables[i] != null){
				inputTables[i].removeAllViews();
			}
			
			if(inputValueType[i] != 0){
				
				inputTables[i].addView(channelText[i]);
				switch(inputValueType[i]){
				case 1: 
					//Raw 10 Bit
//					pBarArray[i].getProgressDrawable().setColorFilter(Color.DKGRAY, Mode.OVERLAY);
					pBarArray[i].setMax(1024);
					pBarArray[i].setProgress(0);
					inputTables[i].addView(pBarArray[i]);
					break;
				case 2:
					//Voltage
//					pBarArray[i].getProgressDrawable().setColorFilter(Color.DKGRAY, Mode.OVERLAY);
					pBarArray[i].setMax(1024);
					pBarArray[i].setProgress(0);
					inputTables[i].addView(pBarArray[i]);
					break;
				case 3:
					//Resistance
//					pBarArray[i].getProgressDrawable().setColorFilter(Color.DKGRAY, Mode.OVERLAY);
					pBarArray[i].setMax(1024);
					pBarArray[i].setProgress(0);
					inputTables[i].addView(pBarArray[i]);
					break;
				case 5:
					//Temperature 495 Degrees F
					//No progress Bar

					break;
				case 6:
					//Temperature 495 Degrees C
					//No progress Bar
					break;
				case 7:
					//Temperature 495 Degrees F
					//No progress Bar
					break;
				case 8:
					//Temperature 495 Degrees C
					//No progress Bar
					break;
				case 9:
					//Temperature 495-2172 Degrees F
					//No progress Bar
					break;
				case 10:
					//Temperature 495-2172 Degrees C
					//No progress Bar
					break;
				case 11:
					//Lux PDV-8001
					pBarArray[i].setMax(1024);
					pBarArray[i].setProgress(0);
					inputTables[i].addView(pBarArray[i]);
					break;
				case 12:
					//Amps H722
					pBarArray[i].setMax(1024);
					pBarArray[i].setProgress(0);
					inputTables[i].addView(pBarArray[i]);
					break;
				case 13:
					//Amps H822
					pBarArray[i].setMax(1024);
					pBarArray[i].setProgress(0);
					inputTables[i].addView(pBarArray[i]);
					break;
				case 14:
					//Solar Battary Status
					pBarArray[i].setMax(440);
					pBarArray[i].setProgress(0);
					inputTables[i].addView(pBarArray[i]);
					break;
				case 15:
					pBarArray[i].setMax(1024);
					pBarArray[i].setProgress(0);
					inputTables[i].addView(pBarArray[i]);
					break;
				case 16:
					pBarArray[i].setMax(1024);
					pBarArray[i].setProgress(0);
					inputTables[i].addView(pBarArray[i]);
					break;
					
				}
				
				sTable.addView(inputTables[i]);
//				table.addView(tRow);
			}
		}
		
		return sTable;
	}


	// *** TOP title Bar ***
	public RelativeLayout title(){
		titleTable = new RelativeLayout(this);
		//titleTable.setBackgroundResource(R.drawable.top_bar);



		// testing
		/*titleTable.setScaleY(0.25F);
		titleTable.setScaleX(0.1F);
		titleTable.setPivotY(0F);
		titleTable.setPivotX(0F);

		titleTable.setY(0F);
		titleTable.setX(0F);


		titleTable.setId(1);*/



//		table.setLayoutParams(new LayoutParams(displayWidth,(int) convertPixelsToDp(248, this)));
		
		final TextView tView = new TextView(this);
//		tView.setPadding(15, 70, 0, 0);
		tView.setText(" ");
		tView.setTypeface(font);
		tView.setTextSize(18);
		tView.setHeight(40);
		tView.setTextColor(Color.WHITE);
		RelativeLayout.LayoutParams titleLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		
		titleLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);

		titleTable.addView(tView,titleLayoutParams);
		
		return titleTable;
	}
	
	public void updateAllInuts(int[] values){
		if(!communicationError){
			//update ui
			if(values != null){
				System.out.println("values length = "+values.length);
				if(values.length == 16){
				
				if(counter == 0){
					previousTime = System.currentTimeMillis();
					counter = 1;
				}else{
					currentTime = System.currentTimeMillis();
					duration = currentTime - previousTime;
					totalTime = totalTime+duration;
					averageTime = totalTime/counter;
					previousTime = currentTime;
					counter = counter +1;
					readTimeTextView.setText("Average Reading Time: "+averageTime + " ms");
				}
				
				for(int i = 0; i < 8; i++){
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
					
					switch(inputValueType[i]){
					case 0:  //Do Nothing
						break;
						
					case 1: //System.out.println("Raw 10 Bit(input "+ (i+1) + ") = " +tenBit(values[msbPos], values[lsbPos])); 
					channelText[i].setText(iNames[i]+" : " + tenBit(values[msbPos], values[lsbPos]));
					pBarArray[i].setProgress(tenBit(values[msbPos], values[lsbPos]));
						break;
						
					case 2: //System.out.println("Voltge(input "+ (i+1) + ") = " + voltage(values[msbPos], values[lsbPos]));
					channelText[i].setText(iNames[i]+" : " + voltage(values[msbPos], values[lsbPos])+" vdc");
					pBarArray[i].setProgress(tenBit(values[msbPos], values[lsbPos]));
						break;
						
					case 3://System.out.println("Resistance(input "+ (i+1) +") = " + resistance(values[msbPos], values[lsbPos]));
					channelText[i].setText(iNames[i]+" : " + resistance(values[msbPos], values[lsbPos])+" ohms");
					pBarArray[i].setProgress(tenBit(values[msbPos], values[lsbPos]));
						break;
						
					case 4://System.out.println("Closure Present on(input "+ (i+1) +") = " + closurePresent(values[msbPos], values[lsbPos]));
					channelText[i].setText(iNames[i]+" : " + closurePresent(values[msbPos], values[lsbPos]));
					pBarArray[i].setProgress(tenBit(values[msbPos], values[lsbPos]));
						break;
					case 5:
						channelText[i].setText(iNames[i]+" : "+temperature495F(values[msbPos], values[lsbPos])+" Degrees F");
						break;
					case 6:
						channelText[i].setText(iNames[i]+" : "+temperature495C(values[msbPos], values[lsbPos])+" Degrees C");
						break;
					case 7:
						channelText[i].setText(iNames[i]+" : "+temperature317F(values[msbPos], values[lsbPos])+" Degrees F");
						break;
					case 8:
						channelText[i].setText(iNames[i]+" : "+temperature317C(values[msbPos], values[lsbPos])+" Degrees C");
						break;
					case 9:
						channelText[i].setText(iNames[i]+" : "+temperature495F(values[msbPos], values[lsbPos])+" Degrees F");
						break;
					case 10:
						channelText[i].setText(iNames[i]+" : "+temperature495C(values[msbPos], values[lsbPos])+" Degrees C");
						break;
					case 11:
						channelText[i].setText(iNames[i]+" : "+pdvp8001(values[msbPos], values[lsbPos])+" Lux");
						pBarArray[i].setProgress(tenBit(values[msbPos], values[lsbPos]));
						break;
					case 12:
						channelText[i].setText(iNames[i]+" : "+currentH722(values[msbPos], values[lsbPos])+" Amps");
						pBarArray[i].setProgress(tenBit(values[msbPos], values[lsbPos]));
						break;
					case 13:
						channelText[i].setText(iNames[i]+" : "+currentH822(values[msbPos], values[lsbPos])+" Amps");
						pBarArray[i].setProgress(tenBit(values[msbPos], values[lsbPos]));
						break;
					case 14:
						
						int status = tenBit(values[msbPos], values[lsbPos]);
						double currentVoltage = voltage(values[msbPos], values[lsbPos]);

						if(currentVoltage>=2.27){
							//Overcharge state
							channelText[i].setTextColor(Color.RED);
							channelText[i].setText(iNames[i]+" : "+"Over Charge! " + round(voltage(values[msbPos], values[lsbPos]), 2, BigDecimal.ROUND_HALF_UP)+"VDC");
							pBarArray[i].setVisibility(View.VISIBLE);
							pBarArray[i].setMax(1);
							pBarArray[i].getProgressDrawable().setColorFilter(Color.RED, Mode.DARKEN);
							pBarArray[i].setProgress(1);
						}else{
							if(currentVoltage>=2.25){
								//Fully Charged
								channelText[i].setTextColor(Color.GREEN);
								channelText[i].setText(iNames[i]+" : "+"Fully Charged "+round(voltage(values[msbPos], values[lsbPos]), 2, BigDecimal.ROUND_HALF_UP)+"VDC");
								pBarArray[i].setVisibility(View.VISIBLE);
								pBarArray[i].setMax(1);
								pBarArray[i].getProgressDrawable().setColorFilter(Color.GREEN, Mode.DARKEN);
								pBarArray[i].setProgress(1);
								
							}else{
								if(currentVoltage>2.12){
									//Charging
									channelText[i].setTextColor(Color.WHITE);
									double chargePercentage = ((((2.25-currentVoltage)/(2.25-2.13))-1)*-100);
									channelText[i].setText(iNames[i]+" : "+"Charging " +round(voltage(values[msbPos], values[lsbPos]), 2, BigDecimal.ROUND_HALF_UP)+"VDC");
									pBarArray[i].setMax(100);
									double roundedVoltage = round(voltage(values[msbPos], values[lsbPos]), 2, BigDecimal.ROUND_HALF_UP);
									int barStatus = (int)(((roundedVoltage-2.12)/.12)*100);
									pBarArray[i].getProgressDrawable().setColorFilter(Color.GREEN, Mode.DARKEN);
									int chargeStatus = (int)(((2.24-currentVoltage)-1)*-100);
									pBarArray[i].setProgress(barStatus);
									pBarArray[i].setVisibility(View.VISIBLE);
									
								}else{
									if(currentVoltage>=1.696){
										//DOD ok range
										double chargePercentage = (((2.12-currentVoltage)/(2.12-1.696))-1)*-100;
										channelText[i].setTextColor(Color.WHITE);
										channelText[i].setText(iNames[i]+" : "+"Discharging "+ round(chargePercentage,2,BigDecimal.ROUND_DOWN )+"% DOD  "+round(voltage(values[msbPos], values[lsbPos]), 2, BigDecimal.ROUND_HALF_UP)+"VDC");
										pBarArray[i].getProgressDrawable().setColorFilter(Color.YELLOW, Mode.DARKEN);
										pBarArray[i].setMax(100);
										pBarArray[i].setVisibility(View.VISIBLE);
										pBarArray[i].setProgress((int)chargePercentage);
										
									}else{
										//Below recommended DOD
										double chargePercentage = (status/435.00)*100;
										channelText[i].setTextColor(Color.WHITE);
										channelText[i].setText(iNames[i]+" : "+"Charge Percentage: "+round(chargePercentage, 2, BigDecimal.ROUND_DOWN)+"% Charge");
										pBarArray[i].getProgressDrawable().setColorFilter(Color.RED, Mode.DARKEN);
										
										pBarArray[i].setVisibility(View.VISIBLE);
										pBarArray[i].setMax(435);
										pBarArray[i].setProgress(status);
									}
									
								}
							}
						}
						break;
					//0-100 PSI Sensor
					case 15:
						channelText[i].setText(iNames[i]+" : "+oneHundredPSIPressure(values[msbPos], values[lsbPos])+" PSI");
						pBarArray[i].setProgress(tenBit(values[msbPos], values[lsbPos]));
						break;
					//0-300 PSI Sensor
					case 16:
						channelText[i].setText(iNames[i]+" : "+threeHundredPSIPressure(values[msbPos], values[lsbPos])+" PSI");
						pBarArray[i].setProgress(tenBit(values[msbPos], values[lsbPos]));
						break;
					case 17:
						channelText[i].setText(iNames[i]+" : "+PX3224(values[msbPos], values[lsbPos])+" PSI");
						pBarArray[i].setProgress(tenBit(values[msbPos], values[lsbPos])-204);
						break;
					case 18:
						
						
					}
					
					
				}
			}else{
				comError();
			}
			}
		}else{
			comError();
		}
		
		
		if(continueReading){
			cPanel.readAllInputs10Bit();
		}
	}
	
	public Handler inputStatusHandler()
	{
		Handler dHandler = new Handler(){
	            public void handleMessage(Message message) 
	            {
	            	if(message.arg1 == Activity.RESULT_CANCELED){
	            		System.out.println("RESULT_CANCELED");
            			communicationError = true;
            			updateAllInuts(null);
            			return;
            		}
	            	
	            	if(winet){
	            		int[] data = (int[])message.obj;
	            		if(data.length == 16){
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
	            			int[] dataToSend = new int[16];
	            			for(int i = 0; i < dataToSend.length; i++){
	            				dataToSend[i] = data[i+2];
	            			}
	            			communicationError = false;
	            			updateAllInuts(dataToSend);
	            			failureCount = 0;
	            		}else{
	            			//Standard ProXR
	            			System.out.println("data length = "+data.length);
	            			if(data.length == 16){
	            				int[] dataToSend = new int[16];
	            				for(int i = 0; i < dataToSend.length; i++){
		            				dataToSend[i] = data[i];
		            			}
	            				updateAllInuts(dataToSend);
	            			}else{
	            				System.out.println("data[0] = "+data[0]);
//		            			System.out.println("data[0] == 170 || winet is false");
		            			communicationError = true;
		            			updateAllInuts(null);
	            			}
	            			
	            			
	            			
	            		}
	            	}
	            	
	            		
	            	
	            }
		};
		return dHandler;
	}
	
	public int tenBit(int MSB, int LSB){
		int value = (MSB*256)+LSB;
		return value;
	}
	
	public double voltage(int MSB, int LSB){
		int tenBit = (MSB*256)+LSB;
		
		double voltsPerStep = vRef/1024.000000;
		
		double voltage = voltsPerStep*tenBit;
		
		return voltage;
	}
	
	public double resistance(int MSB, int LSB){
		double voltage = voltage(MSB, LSB);
		
		double value = (10000*(voltage/5))/(1 - (voltage/5));
		return value;
	}
	
	public String closurePresent(int MSB, int LSB){
		int value = (MSB*256)+LSB;
		if(value < 512){
			return "Closed";
		}
		return "Open";
	}
	
	public double temperature495F(int MSB, int LSB){
		double voltage = voltage(MSB,LSB);
		double resistance = (10000*(voltage/5))/(1 - (voltage/5));
		
		double lookupValue = resistance/10000;
		
		double[] lookupTable={96.3, 67.01, 47.17, 33.65, 24.26, 17.7, 13.04, 9.707, 7.293, 5.533, 4.232, 3.265, 2.539, 1.99, 1.571,
			1.249, 1.00, 0.8057, 0.6531, 0.5327, 0.4369, 0.3603, 0.2986, 0.2488, 0.2083, 0.1752, 0.1481, 0.1258, 0.1072, 0.09177,
			0.07885, 0.068, 0.05886, 0.05112, 0.04454, 0.03893, 0.03417, 0.03009, 0.02654, 0.02348, 0.02083, 0.01853, 0.01653};
		
		int[] temps={-55, -50, -45, -40, -35, -30, -25, -20, -15, -10, -5, 0, 5, 10, 15,
			20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100, 110, 
			115, 120, 125, 130, 135, 140, 145, 150, 155};
		
		int tablePossition = 0;
		
		for(int i = 0; i < lookupTable.length; i++){
			if(lookupValue>lookupTable[i]){
				tablePossition = i;
				break;
			}
		}
		int lowtemp = 0;
		if(tablePossition != 0){
			lowtemp = temps[tablePossition-1];
		}
		
//		return lowtemp;
		if(tablePossition !=0){
			int hightemp = temps[tablePossition];
		}
		
		double tableValLow = lookupTable[tablePossition];
		double tableValHigh = 0;
		if(tablePossition !=0){
			tableValHigh = lookupTable[tablePossition-1];
		}
		
		
		double difference = tableValHigh-tableValLow;


		double stepVal = difference/5;


		double remainder = lookupValue - tableValLow;


		double tempDifference = (difference - remainder) / stepVal;


		int temperature = (int) (lowtemp + tempDifference);

		int farenheit = (temperature*9/5) +32;


		return farenheit;
		
		
		
	}
	
	public double temperature495C(int MSB, int LSB){
		//Convert reading to voltage
		double voltage = voltage(MSB,LSB);
		//Use voltage on input to calculate resistance
		double resistance = (10000*(voltage/5))/(1 - (voltage/5));
		
		//Convert resistance reading to usable value for 495 lookup table
		double lookupValue = resistance/10000;
		
		double[] lookupTable={96.3, 67.01, 47.17, 33.65, 24.26, 17.7, 13.04, 9.707, 7.293, 5.533, 4.232, 3.265, 2.539, 1.99, 1.571,
			1.249, 1.00, 0.8057, 0.6531, 0.5327, 0.4369, 0.3603, 0.2986, 0.2488, 0.2083, 0.1752, 0.1481, 0.1258, 0.1072, 0.09177,
			0.07885, 0.068, 0.05886, 0.05112, 0.04454, 0.03893, 0.03417, 0.03009, 0.02654, 0.02348, 0.02083, 0.01853, 0.01653};
		
		int[] temps={-55, -50, -45, -40, -35, -30, -25, -20, -15, -10, -5, 0, 5, 10, 15,
			20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100, 110, 
			115, 120, 125, 130, 135, 140, 145, 150, 155};
		
		int tablePossition = 0;
		
		for(int i = 0; i < lookupTable.length; i++){
			if(lookupValue>lookupTable[i]){
				tablePossition = i;
				break;
			}
		}
		int lowtemp = 0;
		if(tablePossition != 0){
			lowtemp = temps[tablePossition-1];
		}
		
		double tableValLow = lookupTable[tablePossition];
		double tableValHigh = 0;
		if(tablePossition !=0){
			tableValHigh = lookupTable[tablePossition-1];
		}
		
		
		double difference = tableValHigh-tableValLow;

		double stepVal = difference/5;

		double remainder = lookupValue - tableValLow;

		double tempDifference = (difference - remainder) / stepVal;

		int temperature = (int) (lowtemp + tempDifference);

		return temperature;
	}
	
	public double temperature317C(int MSB, int LSB){
		
		//Convert reading to voltage
		double voltage = voltage(MSB,LSB);
		//Use voltage on input to calculate resistance
		double resistance = (10000*(voltage/5))/(1 - (voltage/5));
		
		double lookupValue = resistance;
		
		double[] lookupTable={111000.3, 86000.39, 67000.74, 53000.39, 42000.45, 33000.89, 27000.28, 22000.05, 17000.96, 14000.68, 12000.09, 10000.00, 8000.313, 6000.941, 5000.828, 4000.912, 4000.161, 3000.537, 3000.021,
				2000.589, 2000.229, 1000.924, 1000.669, 1000.451, 1000.266, 1000.108, 973.5, 857.4, 757.9};
		int[] temps={-30, -25, -20, -15, -10, -5, 0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100, 105, 110};
		
		int tablePossition = 0;
		
		for(int i = 0; i < lookupTable.length; i++){
			if(lookupValue>lookupTable[i]){
				tablePossition = i;
				break;
			}
		}
		int lowtemp = 0;
		if(tablePossition != 0){
			lowtemp = temps[tablePossition-1];
		}
		
		if(tablePossition !=0){
			int hightemp = temps[tablePossition];
		}
		
		double tableValLow = lookupTable[tablePossition];
		double tableValHigh = 0;
		if(tablePossition !=0){
			tableValHigh = lookupTable[tablePossition-1];
		}
		
		
		double difference = tableValHigh-tableValLow;

		double stepVal = difference/5;

		double remainder = lookupValue - tableValLow;

		double tempDifference = (difference - remainder) / stepVal;

		int temperature = (int) (lowtemp + tempDifference);

		return temperature;
	}
	
	public double temperature317F(int MSB, int LSB){
		
		//Convert reading to voltage
		
				double voltage = voltage(MSB,LSB);
				//Use voltage on input to calculate resistance
				double resistance = (10000*(voltage/5))/(1 - (voltage/5));
				
				double lookupValue = resistance;
				
				double[] lookupTable={111000.3, 86000.39, 67000.74, 53000.39, 42000.45, 33000.89, 27000.28, 22000.05, 17000.96, 14000.68, 12000.09, 10000.00, 8000.313, 6000.941, 5000.828, 4000.912, 4000.161, 3000.537, 3000.021,
						2000.589, 2000.229, 1000.924, 1000.669, 1000.451, 1000.266, 1000.108, 973.5, 857.4, 757.9};
				int[] temps={-30, -25, -20, -15, -10, -5, 0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100, 105, 110};
				
				int tablePossition = 0;
				
				for(int i = 0; i < lookupTable.length; i++){
					if(lookupValue>lookupTable[i]){
						tablePossition = i;
						break;
					}
				}
				int lowtemp = 0;
				if(tablePossition != 0){
					lowtemp = temps[tablePossition-1];
				}
				
				if(tablePossition !=0){
					int hightemp = temps[tablePossition];
				}
				
				double tableValLow = lookupTable[tablePossition];
				double tableValHigh = 0;
				if(tablePossition !=0){
					tableValHigh = lookupTable[tablePossition-1];
				}
				
				
				double difference = tableValHigh-tableValLow;

				double stepVal = difference/5;

				double remainder = lookupValue - tableValLow;

				double tempDifference = (difference - remainder) / stepVal;

				int temperature = (int) (lowtemp + tempDifference);
				
				int tempF = (temperature*9/5) +32;
				
				return tempF;
	}
	
	public int pdvp8001(int MSB, int LSB){
		double voltage = voltage(MSB,LSB);
		double resistance = (10000*(voltage/5))/(1 - (voltage/5));
		int lux = (int) (3777479.31*Math.pow(resistance, -1.3));
		
		return lux;
		
	}
	
	public double currentH722(int MSB, int LSB){
		int reading = tenBit(MSB, LSB);
		double currentPerStep = (60.000000/1024.000000);
		double current = reading*currentPerStep;
		return current;
	}
	
	public double currentH822(int MSB, int LSB){

		double inputVoltage = voltage(MSB,LSB);

		int steps = (int)(inputVoltage/voltsPerStep);
		
		double currentPerStep = (10.000000/1024.000000);
		double current = steps*currentPerStep;
		return current;
	}
	
	public int oneHundredPSIPressure(int MSB, int LSB){
		
		double inputVoltage = voltage(MSB,LSB);
		
		int steps = (int)(inputVoltage/voltsPerStep);

		double psiPerStep = (100.000000/1024.000000);
		
		int psi = (int) (steps * psiPerStep);
		
		return psi;
	}
	
	public int threeHundredPSIPressure(int MSB, int LSB){
		
		double inputVoltage = voltage(MSB,LSB);
		
		int steps = (int)(inputVoltage/voltsPerStep);
		
		double psiPerStep = (300.000000/1024);
		
		int psi = (int) (steps*psiPerStep);
		
		return psi;
	}
	
	public double PX3224(int MSB, int LSB){
		double inputVoltage = voltage(MSB,LSB);
		
		//voltsPerStep = 0.0048105
		int steps = (int)((inputVoltage/voltsPerStep)-207);		//2.878598		

		double psiPerStep = (100.000000/819.000000);		//0.1221001
		
		int psi = (int) (steps * psiPerStep);
		
		return psi;
	}
	
	public double PA6229(int MSB, int LSB){
		int reading = tenBit(MSB, LSB)-204;
		double barsPerStep = 1/819.2;			//0.001221001221
		return(reading*barsPerStep);
		
	}
	
	public byte[] read8Channels10Bit(){

		byte[] sendBytes = new byte[5];
		
		sendBytes[0] = (byte)170;
		sendBytes[1] = (byte)2;
		sendBytes[2] = (byte)254;
		sendBytes[3] = (byte)167;
		//Calculate checksum for api packet
		sendBytes[4] = (byte)((sendBytes[0]+sendBytes[1]+sendBytes[2]+sendBytes[3])&255);
		
		return sendBytes;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		callingIntent = getIntent();
		
		callingClass = callingIntent.getClass();
		
		if(callingIntent.getAction() == Intent.ACTION_MAIN){
			
		}else{
			Bundle extras = callingIntent.getExtras();
			if(extras.getString("BLUETOOTHADDRESS")!= null){
				btDeviceAddress = extras.getString("BLUETOOTHADDRESS");
				bluetooth = true;
				continueReading = true;
				cPanel.readAllInputs10Bit();
			}else{
				ip = extras.getString("IP");
				port = extras.getInt("PORT");
				mac = extras.getString("MAC");
				sAddress = new InetSocketAddress(ip,port);
//				new readInput().execute(readChannel10Bit(1));
				continueReading = true;
				cPanel.readAllInputs10Bit();
			}
		}
		
	}

	@Override
	protected void onPause() {
		super.onPause();
		System.out.println("On Pause Called");
		continueReading = false;
		contactCount = 0;
		counter = 0;
		averageTime = 0;
		previousTime = 0;
		if(!displayRelays || displayMacros){
			cPanel.disconnect();
			cPanel.connected = false;
		}
		
		finish();
	}

	private void comError(){
		//Error on connection, read, or write
		long averageTime = 0;
		System.out.println("Com Error");
		if(failureCount < 2){
			failureCount = failureCount+1;
		}else{
			//
			if(lostConnectionDialog == null){
				continueReading = false;
				if(!bluetooth){
					showAlertDialog("Connection Lost");
				}else{

					showAlertDialog("Bluetooth Connection Lost");

				}
			}

		}
	}
	
	private void restartComs(){
		int[] blankStatus = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
		
		continueReading = true;
		cPanel.readAllInputs10Bit();
		
		lostConnectionDialog = null;
		failureCount = 0;
		
	}
	
	private void switchToMacros(){
		Intent macroActivityIntent = new Intent(getApplicationContext(), MacroActivity.class);
		macroActivityIntent.setAction("Start");
		macroActivityIntent.putExtra("MAC", mac);
		startActivity(macroActivityIntent);
		finish();
	}
	
	private void switchToRelays(){
		Intent relayControlIntent = new Intent(getApplicationContext(), RelayControlActivity.class);
		relayControlIntent.setAction("Start");
		relayControlIntent.putExtra("MAC", mac);
		startActivity(relayControlIntent);
		finish();
	}
	
	public class MyGestureDetector extends SimpleOnGestureListener
    {
		
		
    	@Override
    	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY){
    		if(e1 != null && e2 != null){
    			if(e2.getX()-e1.getX() > 200 && displayRelays){
    				switchToRelays();
    				return true;
    			}
    			if(e1.getX()-e2.getX() > 200 && displayMacros){
    				switchToMacros();
    				return true;
    			}
    			
    		}
    		
			return false;
    		
    	}
    }

	private int getStatusBarHeight() {
	  	  int result = 0;
	  	  int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
	  	  if (resourceId > 0) {
	  	      result = getResources().getDimensionPixelSize(resourceId);
	  	  }
	  	  return result;
	  	}
	
	private void showAlertDialog(String title){
		continueReading = false;
		final AlertDialog.Builder removeDeviceAlert = new AlertDialog.Builder(this);
		
		if(bluetooth){
			
			removeDeviceAlert.setTitle(title);
	    	removeDeviceAlert.setMessage("Retry Connection");
	    	removeDeviceAlert.setCancelable(false);
	    	removeDeviceAlert.setPositiveButton("Retry", new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {
					showProgressDialog("Connecting");
					if(cPanel.connect(btDeviceAddress)){
						cPanel.disconnect();
						restartComs();
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
					continueReading = false;

					Intent deviceListActivity = new Intent(getApplicationContext(), DeviceListActivity.class);
					startActivity(deviceListActivity);
					finish();
					
				}
			});
			
		}else{
		
		if(title.equals("Connection Lost")){
			
			
			removeDeviceAlert.setTitle(title);
	    	removeDeviceAlert.setMessage("Retry Connection");
	    	removeDeviceAlert.setCancelable(false);
	    	removeDeviceAlert.setPositiveButton("Local", new DialogInterface.OnClickListener() {
	    		public void onClick(DialogInterface dialog, int which){
	    			showProgressDialog("Searching Lan for device");
	    			
	    			if(cPanel.connect(defaultIP, port)==true){
	    				cPanel.disconnect();
	    				restartComs();
	    				dismissProgressDialog();
	    				dialog.dismiss();
	    				Toast toast = Toast.makeText(getBaseContext(), "Connected", Toast.LENGTH_LONG);
						toast.show();
	    				
	    			}else{
	    				findDeviceIntent = new Intent(getApplicationContext(), FindDevice.class);
	    				findDeviceMessenger = new Messenger(findDeviceHandler());
	    				findDeviceIntent.setAction("Start");
						findDeviceIntent.putExtra("LOCATION", "local");
						findDeviceIntent.putExtra("MAC", mac);
						findDeviceIntent.putExtra("MESSENGER", findDeviceMessenger);
						startService(findDeviceIntent);
	    			}
	    		}
	    	});
	    	removeDeviceAlert.setNeutralButton("Remote", new DialogInterface.OnClickListener(){

				public void onClick(DialogInterface dialog, int which) {
					showProgressDialog("Getting Connection through Signal Switch");
					
					String wiNetMac = cPanel.getStoredString(mac+"-"+"wiNet-wiNetMac");
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
						findDeviceIntent.putExtra("MAC", mac);
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
	    	removeDeviceAlert.setMessage("Retry Connection");
	    	removeDeviceAlert.setCancelable(false);
	    	removeDeviceAlert.setPositiveButton("Local", new DialogInterface.OnClickListener() {
	    		public void onClick(DialogInterface dialog, int which){
	    			
	    			if(cPanel.connect(defaultIP, port)==true){
	    				cPanel.disconnect();
	    				restartComs();
	    				dismissProgressDialog();
	    				dialog.dismiss();
	    				Toast toast = Toast.makeText(getBaseContext(), "Connected", Toast.LENGTH_LONG);
						toast.show();
	    			}else{
	    				findDeviceIntent = new Intent(getApplicationContext(), FindDevice.class);
	    				findDeviceMessenger = new Messenger(findDeviceHandler());
	    				findDeviceIntent.setAction("Start");
						findDeviceIntent.putExtra("LOCATION", "local");
						findDeviceIntent.putExtra("MAC", mac);
						findDeviceIntent.putExtra("MESSENGER", findDeviceMessenger);
						startService(findDeviceIntent);
	    			}
	    		}
	    	});
	    	removeDeviceAlert.setNeutralButton("Remote", new DialogInterface.OnClickListener(){

				public void onClick(DialogInterface dialog, int which) {
					showProgressDialog("Getting Connection through Signal Switch");
					
					String wiNetMac = cPanel.getStoredString(mac+"-"+"wiNet-wiNetMac");
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
						findDeviceIntent.putExtra("MAC", mac);
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
	    	removeDeviceAlert.setMessage("Retry Connection");
	    	removeDeviceAlert.setCancelable(false);
	    	removeDeviceAlert.setPositiveButton("Local", new DialogInterface.OnClickListener() {
	    		public void onClick(DialogInterface dialog, int which){
	    			
	    			if(cPanel.connect(defaultIP, port)==true){
	    				cPanel.disconnect();
	    				restartComs();
	    				dismissProgressDialog();
	    				dialog.dismiss();
	    				Toast toast = Toast.makeText(getBaseContext(), "Connected", Toast.LENGTH_LONG);
						toast.show();
	    			}else{
	    				findDeviceIntent = new Intent(getApplicationContext(), FindDevice.class);
	    				findDeviceMessenger = new Messenger(findDeviceHandler());
	    				findDeviceIntent.setAction("Start");
						findDeviceIntent.putExtra("LOCATION", "local");
						findDeviceIntent.putExtra("MAC", mac);
						findDeviceIntent.putExtra("MESSENGER", findDeviceMessenger);
						startService(findDeviceIntent);
	    			}
	    		}
	    	});
	    	removeDeviceAlert.setNeutralButton("Remote", new DialogInterface.OnClickListener(){

				public void onClick(DialogInterface dialog, int which) {
					showProgressDialog("Getting Connection through Signal Switch");
					
					String wiNetMac = cPanel.getStoredString(mac+"-"+"wiNet-wiNetMac");
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
						findDeviceIntent.putExtra("MAC", mac);
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
	    	removeDeviceAlert.setMessage("Retry Connection");
	    	removeDeviceAlert.setCancelable(false);
	    	removeDeviceAlert.setPositiveButton("Local", new DialogInterface.OnClickListener() {
	    		public void onClick(DialogInterface dialog, int which){
	    			
	    			if(cPanel.connect(defaultIP, port)==true){
	    				cPanel.disconnect();
	    				restartComs();
	    				dismissProgressDialog();
	    				dialog.dismiss();
	    				Toast toast = Toast.makeText(getBaseContext(), "Connected", Toast.LENGTH_LONG);
						toast.show();
	    			}else{
	    				findDeviceIntent = new Intent(getApplicationContext(), FindDevice.class);
	    				findDeviceMessenger = new Messenger(findDeviceHandler());
	    				findDeviceIntent.setAction("Start");
						findDeviceIntent.putExtra("LOCATION", "local");
						findDeviceIntent.putExtra("MAC", mac);
						findDeviceIntent.putExtra("MESSENGER", findDeviceMessenger);
						startService(findDeviceIntent);
	    			}
	    		}
	    	});
	    	removeDeviceAlert.setNeutralButton("Remote", new DialogInterface.OnClickListener(){

				public void onClick(DialogInterface dialog, int which) {
					showProgressDialog("Getting Connection through Signal Switch");
					
					String wiNetMac = cPanel.getStoredString(mac+"-"+"wiNet-wiNetMac");
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
						findDeviceIntent.putExtra("MAC", mac);
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
				if(cPanel.connect(recievedIP, port)){
					lostConnectionDialog.dismiss();
					sTable.removeAllViews();
    				scrollTable();
					Toast toast = Toast.makeText(getBaseContext(), "Connected", Toast.LENGTH_LONG);
					toast.show();
				}else{
					System.out.println("Could Not Connect");
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
	
	private static double round(double unrounded, int precision, int roundingMode)
	{
	    BigDecimal bd = new BigDecimal(unrounded);
	    BigDecimal rounded = bd.setScale(precision, roundingMode);
	    return rounded.doubleValue();
	}

}
