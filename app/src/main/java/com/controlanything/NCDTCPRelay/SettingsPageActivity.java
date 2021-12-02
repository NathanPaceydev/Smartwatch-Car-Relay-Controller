package com.controlanything.NCDTCPRelay;

import java.util.Arrays;

import com.controlanything.NCDTCPRelay.R;
import com.controlanything.NCDTCPRelay.RelayControlActivity.MyGestureDetector;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.opengl.Visibility;
import android.os.Bundle;
import android.text.InputType;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsPageActivity extends Activity{
	
	Intent i;
	//These variables will be read when intent is called.  When AddDeviceActivity calls this activity it will pass an IP address.
	String deviceMacAddress;
	String sentIPAddress;
	
	//Usable Screen Size
	int displayHeight;
	int displayWidth;
	RelativeLayout rLayout;
	ControlPanel cPanel;
	
	//Global Variables for Views
	int textColor = Color.WHITE;
	int subTextSize = 12;
	int buttonWidth = 100;
	int textBoxWidth = 220;
	
	//Global Variables for device settings
	String deviceName;
	String ipAddress;
	String ssid;
	int portNumber;
	int numberOfRelays;
	String[] relayNames;
	int[]momentaryIntArray;
	boolean displayInputs;
	boolean displayRelays;
	boolean displayMacros;
	boolean lockConfig;
	String lockConfigPin;
	boolean lockAccess;
	String lockAccessPin;
	//Variables for Bluetooth Device Settings
	boolean bluetooth;
	boolean winet = false;
	boolean webi = false;
	boolean pwm = false;
	boolean linearActuator=false;
	String bluetoothAddress;
	String bluetoothName;
	
	//Global Views(these have to be global so we can access information from them when saving settings)
	EditText nameEditText;
	EditText ipEditText;
	EditText portEditText;
	EditText ssidEditText;
	Spinner numberOfRelaysSpinner;
	Spinner numberOfPWMChannelsSpinner;
	Spinner numberOfActuatorsSpinner;
	EditText[] relayNamesEditTextArray;
	CheckBox[] momentaryCheckBoxArray;
	TableLayout editRelayNamesTable;
	AnimationDrawable saveButtonAnimation;
	Typeface font;
	CheckBox lockConfigCheckBox;
	EditText configLockPinEditText;
	CheckBox lockAccessCheckBox;
	EditText lockAccessPinEditText;
	CheckBox inputsCheckBox;
	CheckBox relaysCheckBox;
	CheckBox macrosCheckBox;
	Button inputsSetupButton;
	Button macroSetupButton;
	TableRow lockConfigPinRow;
	TableRow lockAccessPinRow;
	TableRow inputSetupRow;
	TableRow macroSetupRow;
	TableRow numRelaysRow;
	TableRow numPWMChannelsRow;
	TableRow numActuatorsRow;
	//RelativeLayout titleTable;
	ImageView sButton;
	
	String api;
	
	GestureDetector gDetector;
	
	int currentapiVersion = android.os.Build.VERSION.SDK_INT;
	
	TableLayout subTable;
	
	String channelType;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		cPanel = ((ControlPanel)getApplicationContext());
		
		//Get the Mac address of the device we are editing.  This is passed from the calling activity.
		i = getIntent();
		
		if(i.getStringExtra("PWM")!=null){
			if(i.getStringExtra("PWM").equalsIgnoreCase("true")){
				pwm = true;
				channelType = "Channel ";
				Toast toast = Toast.makeText(getBaseContext(), "Device is PWM", Toast.LENGTH_LONG);
				toast.show();
			}else{
				pwm = false;
				channelType = "Relay ";
			}
		}else{
			pwm = false;
			channelType = "Relay ";
		}
		if(i.getStringExtra("ACTUATOR")!=null){
			if(i.getStringExtra("ACTUATOR").equalsIgnoreCase("true")){
				linearActuator = true;
				channelType = "Actuator ";
				Toast toast = Toast.makeText(getBaseContext(), "Device is Actuator", Toast.LENGTH_LONG);
				toast.show();
			}else{
				linearActuator = false;
			}
		}else{
			linearActuator = false;
		}
		if(i.getStringExtra("WEBI") != null){
			if(i.getStringExtra("WEBI").equalsIgnoreCase("true")){
				webi = true;
				System.out.println("Settings Page, device is webi? "+webi);
			}else{
				System.out.println("WEBI extra not true");
				webi = false;
			}
		}else{
			System.out.println("No Extra for WEBI");
			webi = false;
		}
		
		if(i.getStringExtra("BLUETOOTHADDRESS") != null){
			bluetooth = true;
			bluetoothAddress = i.getStringExtra("BLUETOOTHADDRESS");
			bluetoothName = i.getStringExtra("DEVICENAME");
			//Bluetooth address will be used as identifier for saved strings.
			deviceMacAddress = bluetoothAddress;
			
		}else{
			bluetooth = false;
			deviceMacAddress = i.getStringExtra("MAC");
			if(i.getStringExtra("IP") != null){
				sentIPAddress = i.getStringExtra("IP");
			}
			if(i.getStringExtra("PORT") != null){
				portNumber = Integer.parseInt(i.getStringExtra("PORT"));
			}
			if(i.getStringExtra("WINET") != null){
				if(i.getStringExtra("WINET").equalsIgnoreCase("true")){
					winet = true;
				}
			}
			System.out.println(deviceMacAddress);
			if(!cPanel.getStoredString(deviceMacAddress).equalsIgnoreCase("n/a")){
				String[] savedSettings = cPanel.getStoredString(deviceMacAddress).split(";");
				if(savedSettings.length > 8){
					if(savedSettings[8].equalsIgnoreCase("true")){
						winet = true;
					}
				}
			}
		}
		
		//Initiate object of ControlPanel class.  We will use this for reading and writing saved strings only in this Activity.
		
		displayHeight = this.getWindow().getWindowManager().getDefaultDisplay().getHeight();
		displayHeight = (displayHeight - getStatusBarHeight());
		displayWidth = this.getWindow().getWindowManager().getDefaultDisplay().getWidth();
		
		//Call local method to read settings of device we are editing and put then in global variables for use at a later time.
		
		getStoredInfo();
		
		font = Typeface.createFromAsset(getAssets(), "fonts/neuropolxfree.ttf");
		
		gDetector = new GestureDetector(getApplicationContext(), new MyGestureDetector());

		setContentView(settingsTable());
		if(currentapiVersion>=11){
			saveButtonAnimation.start();
		}

	}

	private void getStoredInfo(){
		
		//Get stored information on the device we are editing in order to populate view
				String storedSettings = cPanel.getStoredString(deviceMacAddress);
				System.out.println("Stored Settings: "+storedSettings);
				String[] storedSettingsSplit = storedSettings.split(";");
				
				if (storedSettings != "n/a" && storedSettingsSplit.length > 1){
					
					if(storedSettingsSplit[2].equalsIgnoreCase("Bluetooth")){
						bluetooth = true;
					}
					if(bluetooth){
						bluetoothAddress = storedSettingsSplit[0];
						bluetoothName = storedSettingsSplit[1];
						numberOfRelays = Integer.parseInt(storedSettingsSplit[3]);
					}else{
						ipAddress = storedSettingsSplit[1];
						portNumber = Integer.parseInt(storedSettingsSplit[2]);
						numberOfRelays = Integer.parseInt(storedSettingsSplit[3]);
						ssid = storedSettingsSplit[5];
					}
					if(numberOfRelays == 0){
						numberOfRelays = 1;
					}
					deviceName = storedSettingsSplit[4];
					
					if(storedSettingsSplit.length > 6){
						if(storedSettingsSplit[6].equalsIgnoreCase("true")){
							displayInputs = true;
						}else{
							displayInputs = false;
						}
					}else{
						displayInputs = false;
					}
					if(storedSettingsSplit.length > 7){
						if(storedSettingsSplit[7].equalsIgnoreCase("true")){
							displayRelays = true;
						}else{
							displayRelays = false;
						}
					}else{
						displayRelays = false;
					}
					if(storedSettingsSplit.length > 9)
					{
						if(storedSettingsSplit[9].equalsIgnoreCase("true")){
							pwm = true;
							channelType = "Channel ";
						}else{
							pwm = false;
							channelType = "Relay ";
						}
					}
					if(storedSettingsSplit.length > 10){
						if(storedSettingsSplit[10].equalsIgnoreCase("true")){
							linearActuator = true;
							channelType = "Actuator ";
						}else{
							linearActuator = false;
						}
					}
					if(storedSettingsSplit.length > 11){
						if(storedSettingsSplit[11].equalsIgnoreCase("true")){
							displayMacros = true;
						}else{
							displayMacros = false;
						}
					}else{
						displayMacros = false;
					}
					if(storedSettingsSplit.length > 12){
						webi = storedSettingsSplit[12].equalsIgnoreCase("true");
					}
					if(storedSettingsSplit.length > 13){
						lockConfig = storedSettingsSplit[13].equalsIgnoreCase("true");
					}else{
						lockConfig = false;
					}
					if(storedSettingsSplit.length > 14){
						lockConfigPin = storedSettingsSplit[14];
					}else{
						lockConfigPin = "1234";
					}
					if(storedSettingsSplit.length > 15){
						lockAccess = storedSettingsSplit[15].equalsIgnoreCase("true");
					}else{
						lockAccess = false;
					}
					if(storedSettingsSplit.length > 16){
						lockAccessPin = storedSettingsSplit[16];
					}else{
						lockAccessPin = "1234";
					}
					
				}
				
				//Initialize gloabal view objects
				relayNames = new String[numberOfRelays];
				relayNamesEditTextArray = new EditText[numberOfRelays];
				momentaryCheckBoxArray = new CheckBox[numberOfRelays];			


				
				relayNames = (cPanel.getStoredString(deviceMacAddress+"Names")).split(";");
				System.out.println(Arrays.toString(relayNames));
				
				momentaryIntArray = new int[numberOfRelays];
				String[] momentaryString = (cPanel.getStoredString(deviceMacAddress+"Momentary")).split(";");
				for(int i = 0; i<numberOfRelays; i++){
//					System.out.println(momentaryString[i]);
					if (momentaryString[i].equals("1")){
//						System.out.println("momentaryString[i] = 1|0");
						momentaryIntArray[i] = 1;
					}else{
						if(momentaryString[i].equals("0")){
							momentaryIntArray[i] = 0;
						}else{
							momentaryIntArray[i] = 0;
						}
					}
				}


		
	}

	private RelativeLayout settingsTable()
	{
		RelativeLayout sTable = new RelativeLayout(this);
		sTable.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		sTable.setFocusable(true);
		sTable.setFocusableInTouchMode(true);
		sTable.setPadding(20,50,0,0);
		sTable.setBackgroundResource(R.drawable.background);
		
		//sTable.addView(title());
		// set the bottom button layouts
		RelativeLayout.LayoutParams bottomButtonParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, 100);
		bottomButtonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		sTable.addView(saveButton(), bottomButtonParams);
		
		RelativeLayout.LayoutParams scrollViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		scrollViewParams.addRule(RelativeLayout.BELOW,LayoutParams.FILL_PARENT);
		scrollViewParams.addRule(RelativeLayout.ABOVE, sButton.getId());

		sTable.addView(sView(), scrollViewParams);
		
		return sTable;
	}

	/*
	public RelativeLayout title(){
		titleTable = new RelativeLayout(this);
		titleTable.setBackgroundResource(R.drawable.top_bar);
		titleTable.setId(1);
		
//		table.setLayoutParams(new LayoutParams(displayWidth,(int) convertPixelsToDp(248, this)));
		
		final TextView tView = new TextView(this);
//		tView.setPadding(15, 70, 0, 0);
		tView.setText("Edit Configuration\n\n"+deviceMacAddress);
		tView.setTypeface(font);
		tView.setTextSize(24);
		tView.setTextColor(Color.BLACK);
		RelativeLayout.LayoutParams titleLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		
		titleLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
		
		titleTable.addView(tView,titleLayoutParams);
		
		return titleTable;
	}*/
	
	private ScrollView sView(){
		ScrollView scrollView = new ScrollView(this);
		scrollView.setFadingEdgeLength(0);
		scrollView.addView(scrollTable());
		scrollView.setLayoutParams(new ViewGroup.LayoutParams(displayWidth-10, LayoutParams.MATCH_PARENT));
		
//		scrollView.setOnTouchListener(new View.OnTouchListener() {
			
//			public boolean onTouch(View v, MotionEvent event) {
//				if(gDetector.onTouchEvent(event)){
//					
//					//Save entered settings
//					saveSettings();
//					
//					//Switch to AD input Settings
//					Intent adSettingsIntent = new Intent(getApplicationContext(), ADSetupActivity.class);
//					adSettingsIntent.putExtra("MAC", deviceMacAddress);
//					startActivity(adSettingsIntent);
//					finish();
//				}
//				return false;
//			}
//		});
		
		return scrollView;
	}
	
	private ImageView saveButton(){
		sButton = new ImageView(this);
		sButton.setId(2);
		if(currentapiVersion>=11){
			sButton.setImageResource(R.drawable.animationxmlsavesettings);
			//sButton.setBackgroundResource(R.drawable.bottom_bar);
			sButton.setPadding(0, 10, 0, 10);
			sButton.setMinimumHeight(120);
			sButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			
			saveButtonAnimation = (AnimationDrawable)sButton.getDrawable();
			saveButtonAnimation.setEnterFadeDuration(1500);
			saveButtonAnimation.setExitFadeDuration(1500);
			
			sButton.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					saveSettings();
					Intent deviceListIntent = new Intent(getApplicationContext(), DeviceListActivity.class);
					startActivity(deviceListIntent);
					finish();
				}
				
			});
		}else{
			sButton.setImageResource(R.drawable.bottom_bar_button);
			//sButton.setBackgroundResource(R.drawable.bottom_bar);
			sButton.setPadding(0, 10, 0, 10);
			sButton.setMinimumHeight(120);
			sButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			
			sButton.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					saveSettings();
					Intent deviceListIntent = new Intent(getApplicationContext(), DeviceListActivity.class);
					startActivity(deviceListIntent);
					finish();
				}
				
			});
		}
		
		

		return sButton;
	}
	
	public void saveSettings(){
		
		if(bluetooth){
			//Save Bluetooth Device Info
			if(!pwm){
				if(!linearActuator){
					cPanel.saveString(bluetoothAddress, bluetoothAddress+";"+bluetoothName+";"+"Bluetooth"+";"+String.valueOf(numberOfRelays)+";"+nameEditText.getText().toString()+";"+"Bluetooth"+";"+inputsCheckBox.isChecked()+";"+relaysCheckBox.isChecked()+";"+winet+";"+pwm+";"+linearActuator+";"+macrosCheckBox.isChecked()+";"+webi+";"+lockConfigCheckBox.isChecked()+";"+configLockPinEditText.getText().toString()+";"+lockAccessCheckBox.isChecked()+";"+lockAccessPinEditText.getText().toString());
				}else{
					cPanel.saveString(bluetoothAddress, bluetoothAddress+";"+bluetoothName+";"+"Bluetooth"+";"+String.valueOf(numberOfRelays)+";"+nameEditText.getText().toString()+";"+"Bluetooth"+";"+"false"+";"+"false"+";"+winet+";"+pwm+";"+linearActuator+";"+macrosCheckBox.isChecked()+";"+webi+";"+lockConfigCheckBox.isChecked()+";"+configLockPinEditText.getText().toString()+";"+lockAccessCheckBox.isChecked()+";"+lockAccessPinEditText.getText().toString());
				}
			}else{
				cPanel.saveString(bluetoothAddress, bluetoothAddress+";"+bluetoothName+";"+"Bluetooth"+";"+String.valueOf(numberOfRelays)+";"+nameEditText.getText().toString()+";"+"Bluetooth"+";"+"false"+";"+"false"+";"+winet+";"+pwm+";"+linearActuator+";"+macrosCheckBox.isChecked()+";"+webi+";"+lockConfigCheckBox.isChecked()+";"+configLockPinEditText.getText().toString()+";"+lockAccessCheckBox.isChecked()+";"+lockAccessPinEditText.getText().toString());
			}
			
		}else{
			//Save Network Device Info
			if(ssidEditText.getText().toString().equals("Enter Network")){
				Toast toast = Toast.makeText(getBaseContext(), "Please Enter Network Device is associated with", Toast.LENGTH_LONG);
				toast.show();
				return;
			}

			if(!pwm){
				if(!linearActuator){
					cPanel.saveString(deviceMacAddress, deviceMacAddress +";"+ipEditText.getText().toString()+";"+portEditText.getText().toString()+";"+String.valueOf(numberOfRelays)+";"+nameEditText.getText().toString()+";"+ssidEditText.getText().toString()+";"+inputsCheckBox.isChecked()+";"+relaysCheckBox.isChecked()+";"+winet+";"+pwm+";"+linearActuator+";"+macrosCheckBox.isChecked()+";"+webi+";"+lockConfigCheckBox.isChecked()+";"+configLockPinEditText.getText().toString()+";"+lockAccessCheckBox.isChecked()+";"+lockAccessPinEditText.getText().toString());
				}else{
					cPanel.saveString(deviceMacAddress, deviceMacAddress +";"+ipEditText.getText().toString()+";"+portEditText.getText().toString()+";"+String.valueOf(numberOfRelays)+";"+nameEditText.getText().toString()+";"+ssidEditText.getText().toString()+";"+false+";"+false+";"+winet+";"+pwm+";"+linearActuator+";"+macrosCheckBox.isChecked()+";"+webi+";"+lockConfigCheckBox.isChecked()+";"+configLockPinEditText.getText().toString()+";"+lockAccessCheckBox.isChecked()+";"+lockAccessPinEditText.getText().toString());
				}
			}else{
				cPanel.saveString(deviceMacAddress, deviceMacAddress +";"+ipEditText.getText().toString()+";"+portEditText.getText().toString()+";"+String.valueOf(numberOfRelays)+";"+nameEditText.getText().toString()+";"+ssidEditText.getText().toString()+";"+false+";"+false+";"+winet+";"+pwm+";"+linearActuator+";"+macrosCheckBox.isChecked()+";"+webi+";"+lockConfigCheckBox.isChecked()+";"+configLockPinEditText.getText().toString()+";"+lockAccessCheckBox.isChecked()+";"+lockAccessPinEditText.getText().toString());
			}
			

		}
		//Save Relay Names and momentary checks
		String names = null;
		for(int i = 0; i < relayNamesEditTextArray.length; i++){
			if (names == null){
				names = (relayNamesEditTextArray[i].getText().toString()+";");
			}else{
				names = (names + relayNamesEditTextArray[i].getText().toString() + ";");
			}
		}
		System.out.println(deviceMacAddress+"Names"+" : "+names);
		cPanel.saveString(deviceMacAddress+"Names", names);
		
		String momentarys = null;
		for(int i = 0; i < momentaryCheckBoxArray.length; i++){
			if (momentarys == null){
				if (momentaryCheckBoxArray[i].isChecked()){
					momentarys = "1;";
				}else{
					momentarys = "0;";
				}
			}else{
				if(momentaryCheckBoxArray[i].isChecked()){
					momentarys = (momentarys + "1;");						
				}else{
					momentarys = (momentarys + "0;");
				}
			}
		}
		System.out.println("Momentaries: "+momentarys);
		cPanel.saveString(deviceMacAddress+"Momentary", momentarys);
		
	}
	
	private TableLayout scrollTable(){
		subTable = new TableLayout(this);
		System.out.println("subTable Created");
		
		subTable.addView(deviceNameRow());
		
		if(!bluetooth){
			subTable.addView(deviceIPRow());
			subTable.addView(devicePort());
			subTable.addView(ssidRow());
		}
		subTable.addView(lockConfigCheckRow());
		subTable.addView(lockConfigPinEditRow());
		subTable.addView(lockAccessCheckRow());
		subTable.addView(lockAccessPinEditRow());
		if(!pwm){
			if(!linearActuator){
				subTable.addView(displayInputsCheck());
				//subTable.addView(inputButtonRow());
				subTable.addView(displayMacrosCheck());
				//subTable.addView(macroButtonRow());
				subTable.addView(displayRelaysCheck());
				subTable.addView(numberOfRelaysRow());
				
			}else{
				subTable.addView(numberOfActuatorsRow());
			}
		}else{
			subTable.addView(numberOfPWMChannelsRow());
		}
		editTextsTable();
		
		
		return subTable;
	}
	
	private TableRow deviceNameRow(){
		TableRow dName = new TableRow(this);
		
		TextView tView = new TextView(this);
		
		tView.setText(R.string.device_name);
		tView.setTextSize(subTextSize);
		tView.setTextColor(textColor);
		tView.setTextSize(subTextSize);
		tView.setGravity(Gravity.CENTER_HORIZONTAL);
		
		nameEditText = new EditText(this);
		nameEditText.setBackgroundResource(R.drawable.textbox);
		nameEditText.getBackground().setAlpha(180);
		nameEditText.setTextColor(Color.WHITE);

		
		nameEditText.setWidth(buttonWidth);
		//nameEditText.setHeight(20);

		//nameEditText.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));

		nameEditText.setSingleLine();
		nameEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
		
		if(deviceName == null){
			deviceName = "n/a";
		}
		
		if(deviceName == "n/a"){
			nameEditText.setText(R.string.enter_name);
		}else{
			nameEditText.setText(deviceName);
		}

		nameEditText.setTextSize(subTextSize);

		dName.addView(tView);
		dName.addView(nameEditText);
		
		return dName;
	}

	private TableRow deviceIPRow(){
		TableRow tableRow = new TableRow(this);
		
		TextView tView = new TextView(this);
		tView.setText(R.string.default_IP_address);
		tView.setTextColor(textColor);
		tView.setTextSize(subTextSize);
		tView.setGravity(Gravity.CENTER_HORIZONTAL);


		ipEditText = new EditText(this);
		ipEditText.setWidth(buttonWidth);
		ipEditText.setSingleLine();
		ipEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
		ipEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
		
		ipEditText.setBackgroundResource(R.drawable.textbox);
		ipEditText.getBackground().setAlpha(180);
		ipEditText.setTextColor(Color.WHITE);
		ipEditText.setTextSize(subTextSize);
		
		if(sentIPAddress == null){
			ipEditText.setText(ipAddress);
		}else{
			ipEditText.setText(sentIPAddress);
		}
		tableRow.addView(tView);
		tableRow.addView(ipEditText);
		
		return tableRow;
	}
	
	private TableRow devicePort(){
		TableRow tableRow = new TableRow(this);
		
		TextView tView = new TextView(this);
		tView.setText(R.string.port_number);
		tView.setTextColor(textColor);
		tView.setTextSize(subTextSize);
		tView.setGravity(Gravity.CENTER_HORIZONTAL);


		portEditText = new EditText(this);
		portEditText.setWidth(buttonWidth);
		portEditText.setSingleLine();
		portEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
		portEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
		
		portEditText.setBackgroundResource(R.drawable.textbox);
		portEditText.getBackground().setAlpha(180);
		portEditText.setTextColor(Color.WHITE);
		portEditText.setTextSize(subTextSize);
		
		if(portNumber == 0){
			portEditText.setText(portNumber);
		}else{
			portEditText.setText(String.valueOf(portNumber));
		}
		tableRow.addView(tView);
		tableRow.addView(portEditText);
		
		return tableRow;
		
	}
	
	private TableRow ssidRow(){
		TableRow tableRow = new TableRow(this);
		
		TextView tView = new TextView(this);
		tView.setText(R.string.network_name);
		tView.setTextColor(textColor);
		tView.setTextSize(subTextSize);
		tView.setGravity(Gravity.CENTER_HORIZONTAL);


		ssidEditText = new EditText(this);
		ssidEditText.setBackgroundResource(R.drawable.textbox);
		ssidEditText.getBackground().setAlpha(180);
		ssidEditText.setTextColor(Color.WHITE);
		ssidEditText.setTextSize(subTextSize);

		
		ssidEditText.setWidth(buttonWidth);
		ssidEditText.setSingleLine();
		ssidEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
		
		if(ssid == "n/a" | ssid ==" "){
			ssidEditText.setText(R.string.enter_network);
		}else{
			ssidEditText.setText(ssid);
		}
		tableRow.addView(tView);
		tableRow.addView(ssidEditText);
		
		return tableRow;
	}
	
	private TableRow lockConfigCheckRow(){
		TableRow tRow = new TableRow(this);
		
		TextView tView = new TextView(this);
		tView.setText(R.string.lock_config);
		tView.setTextColor(textColor);
		tView.setTextSize(subTextSize);
		tView.setGravity(Gravity.CENTER_HORIZONTAL);


		lockConfigCheckBox = new CheckBox(this);
		if (lockConfig){
			lockConfigCheckBox.setChecked(false);
			//lockConfigCheckBox.setChecked(true);
		}else{
			lockConfigCheckBox.setChecked(false);
		}

		lockConfigCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1 == true){
					lockConfigPinRow.setVisibility(View.VISIBLE);
				}else{
					lockConfigPinRow.setVisibility(View.GONE);
				}
			}
				
			});


			
		tRow.addView(tView);
		tRow.addView(lockConfigCheckBox);
		
		return tRow;
	}
	
	private TableRow lockConfigPinEditRow(){
		lockConfigPinRow = new TableRow(this);
		TextView tView = new TextView(this);
		tView.setText(R.string.lock_config_pin);
		tView.setTextColor(textColor);
		tView.setTextSize(subTextSize);
		tView.setGravity(Gravity.CENTER_HORIZONTAL);


		configLockPinEditText = new EditText(this);
		configLockPinEditText.setBackgroundResource(R.drawable.textbox);
		configLockPinEditText.getBackground().setAlpha(180);
		configLockPinEditText.setTextColor(Color.WHITE);
		configLockPinEditText.setTextSize(subTextSize);
		
		configLockPinEditText.setWidth(buttonWidth);
		configLockPinEditText.setSingleLine();
		configLockPinEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
		configLockPinEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
		
		configLockPinEditText.setText(lockConfigPin);
		
		lockConfigPinRow.addView(tView);
		lockConfigPinRow.addView(configLockPinEditText);
		
		if(!lockConfig){
			lockConfigPinRow.setVisibility(View.GONE);
		}
		
		return lockConfigPinRow;
	}
	
	private TableRow lockAccessCheckRow(){
		TableRow tRow = new TableRow(this);
		
		TextView tView = new TextView(this);
		tView.setText(R.string.lock_access);
		tView.setTextColor(textColor);
		tView.setTextSize(subTextSize);
		tView.setGravity(Gravity.CENTER_HORIZONTAL);


		lockAccessCheckBox = new CheckBox(this);
		if (lockAccess){
			lockAccessCheckBox.setChecked(true);
		}else{
			lockAccessCheckBox.setChecked(false);
		}


		lockAccessCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1 == true){
					lockAccessPinRow.setVisibility(View.VISIBLE);
				}else{
					lockAccessPinRow.setVisibility(View.GONE);
				}
			}

			});

			
		tRow.addView(tView);
		tRow.addView(lockAccessCheckBox);
		
		return tRow;
	}

	
	private TableRow lockAccessPinEditRow(){
		lockAccessPinRow = new TableRow(this);
		TextView tView = new TextView(this);
		tView.setText(R.string.Lock_Access_Pin);
		tView.setTextColor(textColor);
		tView.setTextSize(subTextSize);
		tView.setGravity(Gravity.CENTER_HORIZONTAL);


		lockAccessPinEditText = new EditText(this);
		lockAccessPinEditText.setBackgroundResource(R.drawable.textbox);
		lockAccessPinEditText.getBackground().setAlpha(180);
		lockAccessPinEditText.setTextColor(Color.WHITE);
		lockAccessPinEditText.setTextSize(subTextSize);
		
		lockAccessPinEditText.setWidth(buttonWidth);
		lockAccessPinEditText.setSingleLine();
		lockAccessPinEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
		lockAccessPinEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
		
		lockAccessPinEditText.setText(lockAccessPin);
		
		lockAccessPinRow.addView(tView);
		lockAccessPinRow.addView(lockAccessPinEditText);
		
		if(!lockAccess){
			lockAccessPinRow.setVisibility(View.GONE);
		}
		
		return lockAccessPinRow;
	}

	
	private TableRow displayInputsCheck(){
		TableRow tRow = new TableRow(this);
		
		TextView tView = new TextView(this);
		tView.setText(R.string.display_inputs);
		tView.setTextColor(textColor);
		tView.setTextSize(subTextSize);
		tView.setGravity(Gravity.CENTER_HORIZONTAL);


		inputsCheckBox = new CheckBox(this);
		if (displayInputs){
			inputsCheckBox.setChecked(true);
		}else{
			inputsCheckBox.setChecked(false);
		}

		/*inputsCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1 == true){
					inputSetupRow.setVisibility(View.VISIBLE);
				}else{
					inputSetupRow.setVisibility(View.GONE);
				}
			}
				
			});

		 */
			
		tRow.addView(tView);
		tRow.addView(inputsCheckBox);
		
		return tRow;
		
	}

	/*
	private TableRow inputButtonRow(){
		inputSetupRow = new TableRow(this);
		
		TextView tView = new TextView(this);
		tView.setText(R.string.a_or_d_setup);
		tView.setTextColor(textColor);
		tView.setTextSize(subTextSize);
		tView.setGravity(Gravity.CENTER_HORIZONTAL);


		inputsSetupButton = new Button(this);
		inputsSetupButton.setBackgroundResource(R.drawable.textbox);
		inputsSetupButton.setTextColor(textColor);
		inputsSetupButton.setText(R.string.setup);
		inputsSetupButton.setTextSize(subTextSize);
		inputsSetupButton.setWidth(100);
		inputsSetupButton.setOnClickListener(new OnClickListener(){

			public void onClick(View arg0) {
				saveSettings();
				Intent adSettingsIntent = new Intent(getApplicationContext(), ADSetupActivity.class);
				adSettingsIntent.putExtra("MAC", deviceMacAddress);
				startActivity(adSettingsIntent);
				finish();
			}
		});
		
		inputSetupRow.addView(tView);
		inputSetupRow.addView(inputsSetupButton);
		
		if(!displayInputs){
			inputSetupRow.setVisibility(View.GONE);
		}
		
		return inputSetupRow;
	}

	 */
	
	private TableRow displayRelaysCheck(){
		
		TableRow tRow = new TableRow(this);
		
		TextView tView = new TextView(this);
		tView.setText(R.string.display_relays);
		tView.setTextColor(textColor);
		tView.setTextSize(subTextSize);
		tView.setGravity(Gravity.CENTER_HORIZONTAL);


		relaysCheckBox = new CheckBox(this);
		
		if(displayRelays){
			relaysCheckBox.setChecked(true);
		}else{
			relaysCheckBox.setChecked(false);
		}
		relaysCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1 == true){
					editRelayNamesTable.setVisibility(View.VISIBLE);
					numRelaysRow.setVisibility(View.VISIBLE);
					displayRelays = true;
				}else{
					editRelayNamesTable.setVisibility(View.GONE);
					numRelaysRow.setVisibility(View.GONE);
					displayRelays = false;
				}
				
			}
			
		});
		
		tRow.addView(tView);
		tRow.addView(relaysCheckBox);
		
		return tRow;
		
		
	}
	
	private TableRow displayMacrosCheck(){
		TableRow tRow = new TableRow(this);
		
		TextView tView = new TextView(this);
		tView.setText(R.string.display_macros);
		tView.setTextColor(textColor);
		tView.setTextSize(subTextSize);
		tView.setGravity(Gravity.CENTER_HORIZONTAL);


		macrosCheckBox = new CheckBox(this);
		if(displayMacros){
			macrosCheckBox.setChecked(true);
		}else{
			macrosCheckBox.setChecked(false);
		}

		/*
		macrosCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1 == true){
					macroSetupRow.setVisibility(View.VISIBLE);
				}else{
					macroSetupRow.setVisibility(View.GONE);
				}
				
			}
			
		});

		 */
		
		tRow.addView(tView);
		tRow.addView(macrosCheckBox);
		
		return tRow;		
	}

	/*
	private TableRow macroButtonRow(){
		macroSetupRow = new TableRow(this);
		
		TextView tView = new TextView(this);
		tView.setText(R.string.macro_setup);
		tView.setTextColor(textColor);
		tView.setTextSize(subTextSize);
		tView.setGravity(Gravity.CENTER_HORIZONTAL);


		macroSetupButton = new Button(this);
		macroSetupButton.setBackgroundResource(R.drawable.textbox);
		macroSetupButton.setTextColor(textColor);
		macroSetupButton.setText(R.string.setup);
		macroSetupButton.setTextSize(subTextSize);
		macroSetupButton.setWidth(buttonWidth);
		macroSetupButton.setOnClickListener(new OnClickListener(){

			public void onClick(View arg0) {
				saveSettings();
				Intent macroSettingsIntent = new Intent(getApplicationContext(), MacroSetupActivity.class);
				macroSettingsIntent.putExtra("MAC", deviceMacAddress);
				startActivity(macroSettingsIntent);
				finish();
			}
		});
		
		macroSetupRow.addView(tView);
		macroSetupRow.addView(macroSetupButton);
		
		if(!displayMacros){
			macroSetupRow.setVisibility(View.GONE);
		}
		
		return macroSetupRow;
	}

	 */
	
	private TableRow numberOfRelaysRow(){
		numRelaysRow = new TableRow(this);
//		numRelaysRow.setMinimumHeight(80);
		
		TextView tView = new TextView(this);
		tView.setText(R.string.number_of_relays);
		tView.setTextColor(textColor);
		tView.setTextSize(subTextSize);
		tView.setGravity(Gravity.CENTER_HORIZONTAL);


		numberOfRelaysSpinner = new Spinner(this);
		
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.numOfRelays, R.layout.spinner_item);

		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		numberOfRelaysSpinner.setAdapter(adapter);

		//numberOfRelaysSpinner.setLayoutParams(new LayoutParams(80,30));

		numberOfRelaysSpinner.setOnItemSelectedListener(new SpinnerListener());
		numberOfRelaysSpinner.setBackgroundResource(R.drawable.spinner_box);
		numberOfRelaysSpinner.getBackground().setAlpha(180);




		int tempInt = adapter.getPosition(String.valueOf(numberOfRelays));
		numberOfRelaysSpinner.setSelection(tempInt);


		numRelaysRow.addView(tView);
		numRelaysRow.addView(numberOfRelaysSpinner);
		
		if(!displayRelays){
			numRelaysRow.setVisibility(View.GONE);
		}
		
		System.out.println("numRelaysRow id: "+numRelaysRow);
		
		return numRelaysRow;
	}

	
	private TableRow numberOfPWMChannelsRow(){
		numPWMChannelsRow = new TableRow(this);
		
		TextView tView = new TextView(this);
		tView.setText(R.string.number_of_channels);
		tView.setTextColor(textColor);
		tView.setTextSize(subTextSize);
		tView.setGravity(Gravity.CENTER_HORIZONTAL);


		numberOfPWMChannelsSpinner = new Spinner(this);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.numOfPWMChannels, R.layout.spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		numberOfPWMChannelsSpinner.setAdapter(adapter);
		numberOfPWMChannelsSpinner.setOnItemSelectedListener(new PWMSpinnerListener());
		numberOfPWMChannelsSpinner.setBackgroundResource(R.drawable.spinner_box);
		numberOfPWMChannelsSpinner.getBackground().setAlpha(180);
		
		int tempInt = adapter.getPosition(String.valueOf(numberOfRelays));
		numberOfPWMChannelsSpinner.setSelection(tempInt);
		
		numPWMChannelsRow.addView(tView);
		numPWMChannelsRow.addView(numberOfPWMChannelsSpinner);
		
		return numPWMChannelsRow;
	}
	
	private TableRow numberOfActuatorsRow(){
		numActuatorsRow = new TableRow(this);
		TextView tView = new TextView(this);
		tView.setText(R.string.number_of_actuators);
		tView.setTextColor(textColor);
		tView.setTextSize(subTextSize);
		tView.setGravity(Gravity.CENTER_HORIZONTAL);


		numberOfActuatorsSpinner = new Spinner(this);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.numOfActuators, R.layout.spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		numberOfActuatorsSpinner.setAdapter(adapter);
		numberOfActuatorsSpinner.setOnItemSelectedListener(new ActuatorSpinnerListener());
		numberOfActuatorsSpinner.setBackgroundResource(R.drawable.spinner_box);
		numberOfActuatorsSpinner.getBackground().setAlpha(180);
		
		int tempInt = adapter.getPosition(String.valueOf(numberOfRelays));
		numberOfActuatorsSpinner.setSelection(tempInt);
		
		numActuatorsRow.addView(tView);
		numActuatorsRow.addView(numberOfActuatorsSpinner);
		
		return numActuatorsRow;
		
	}
	
	private class SpinnerListener implements OnItemSelectedListener{

		public void onItemSelected(AdapterView<?> parent,
        		View view, int pos, long id) {
			System.out.println("Spinner listener called");
			numberOfRelays = Integer.parseInt(parent.getItemAtPosition(pos).toString());
			momentaryIntArray = new int[numberOfRelays];
			relayNames = new String[numberOfRelays];
			
			//populate relayNames string array with stored names
			String[] namesSplit = (cPanel.getStoredString(deviceMacAddress+"Names")).split(";");
			
			if(namesSplit.length < relayNames.length){
				for(int i = 0; i < namesSplit.length; i++){
					relayNames[i] = namesSplit[i];
				}
			}else{
				for(int i = 0; i < relayNames.length; i++){
					relayNames[i] = namesSplit[i];
				}
			}
			
			
			String[] momentaryString = (cPanel.getStoredString(deviceMacAddress+"Momentary")).split(";");
			for(int i = 0; i<numberOfRelays; i++){
				if(momentaryString.length <= i){
					momentaryIntArray[i] = 0;
				}else{
				if (momentaryString[i].equals("1")){
//					System.out.println("momentaryString[i] = 1|0");
					momentaryIntArray[i] = 1;
				}else{
					if(momentaryString[i].equals("0")){
						momentaryIntArray[i] = 0;
					}else{
						momentaryIntArray[i] = 0;
					}
				}
				}
			}
//			relayNames = new String[numberOfRelays];

			relayNamesEditTextArray = new EditText[numberOfRelays];
			momentaryCheckBoxArray = new CheckBox[numberOfRelays];

			editTextsTable();
			
		}

		public void onNothingSelected(AdapterView<?> arg0) {
		}
		
	}
	
	private class PWMSpinnerListener implements OnItemSelectedListener{

		public void onItemSelected(AdapterView<?> parent,
        		View view, int pos, long id) {
			numberOfRelays = Integer.parseInt(parent.getItemAtPosition(pos).toString());
			momentaryIntArray = new int[numberOfRelays];
			relayNames = new String[numberOfRelays];
			
			//populate relayNames string array with stored names
			String[] namesSplit = (cPanel.getStoredString(deviceMacAddress+"Names")).split(";");
			
			if(namesSplit.length < relayNames.length){
				for(int i = 0; i < namesSplit.length; i++){
					relayNames[i] = namesSplit[i];
				}
			}else{
				for(int i = 0; i < relayNames.length; i++){
					relayNames[i] = namesSplit[i];
				}
			}
			
			
			String[] momentaryString = (cPanel.getStoredString(deviceMacAddress+"Momentary")).split(";");
			for(int i = 0; i<numberOfRelays; i++){
				if(momentaryString.length <= i){
					momentaryIntArray[i] = 0;
				}else{
				if (momentaryString[i].equals("1")){
//					System.out.println("momentaryString[i] = 1|0");
					momentaryIntArray[i] = 1;
				}else{
					if(momentaryString[i].equals("0")){
						momentaryIntArray[i] = 0;
					}else{
						momentaryIntArray[i] = 0;
					}
				}
				}
			}
//			relayNames = new String[numberOfRelays];

			relayNamesEditTextArray = new EditText[numberOfRelays];
			momentaryCheckBoxArray = new CheckBox[numberOfRelays];

			editTextsTable();
			
		}

		public void onNothingSelected(AdapterView<?> arg0) {
			
		}
		
	}
	
	private class ActuatorSpinnerListener implements OnItemSelectedListener{

		public void onItemSelected(AdapterView<?> parent,
        		View view, int pos, long id) {
			numberOfRelays = Integer.parseInt(parent.getItemAtPosition(pos).toString());
			momentaryIntArray = new int[numberOfRelays];
			relayNames = new String[numberOfRelays];
			
			String[] namesSplit = (cPanel.getStoredString(deviceMacAddress+"Names")).split(";");
			
			if(namesSplit.length < relayNames.length){
				for(int i = 0; i < namesSplit.length; i++){
					relayNames[i] = namesSplit[i];
				}
			}else{
				for(int i = 0; i < relayNames.length; i++){
					relayNames[i] = namesSplit[i];
				}
			}
			
			String[] momentaryString = (cPanel.getStoredString(deviceMacAddress+"Momentary")).split(";");
			for(int i = 0; i<numberOfRelays; i++){
				if(momentaryString.length <= i){
					momentaryIntArray[i] = 0;
				}else{
				if (momentaryString[i].equals("1")){
					momentaryIntArray[i] = 1;
				}else{
					if(momentaryString[i].equals("0")){
						momentaryIntArray[i] = 0;
					}else{
						momentaryIntArray[i] = 0;
					}
				}
				}
			}

			relayNamesEditTextArray = new EditText[numberOfRelays];
			momentaryCheckBoxArray = new CheckBox[numberOfRelays];

			editTextsTable();
			
		}

		public void onNothingSelected(AdapterView<?> arg0) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	private void editTextsTable(){
		if (editRelayNamesTable == null){
			editRelayNamesTable = new TableLayout(this);
		}else{
			subTable.removeView(editRelayNamesTable);
			editRelayNamesTable = new TableLayout(this);
		}
		for(int i = 0; i < numberOfRelays; i++){
			TableRow tableRow = new TableRow(this);
			TableRow tableRow1 = new TableRow(this);
			TextView tView = new TextView(this);

			tableRow.setGravity(Gravity.CENTER_HORIZONTAL);
			tableRow1.setGravity(Gravity.CENTER_HORIZONTAL);

			tView.setText(channelType+ (i+1));
			tView.setGravity(Gravity.RIGHT);
			tView.setPadding(16,0,16,0);

			System.out.println("setting tView text to: "+channelType+ (i+1));
			
			
			tView.setTextSize(subTextSize);
			tView.setTextColor(textColor);

			if(pwm || linearActuator){
				tView.setWidth(textBoxWidth); //** text width **
			}

			relayNamesEditTextArray[i] = new EditText(this);
			relayNamesEditTextArray[i].setSingleLine();
			relayNamesEditTextArray[i].setImeOptions(EditorInfo.IME_ACTION_DONE);
			relayNamesEditTextArray[i].setWidth(textBoxWidth);
			
			relayNamesEditTextArray[i].setBackgroundResource(R.drawable.textbox);
			relayNamesEditTextArray[i].getBackground().setAlpha(180);
			relayNamesEditTextArray[i].setTextColor(Color.WHITE);
			
			if(relayNames[i] != null){
				if(relayNames[i].equalsIgnoreCase("n/a")){
					relayNamesEditTextArray[i].setText(channelType+(i+1));
					System.out.println("settings relayNamesEditTextArray text to: "+channelType+(i+1));
				}else{
					relayNamesEditTextArray[i].setText(relayNames[i]);
				}
			}else{
				System.out.println("no stored name for relay"+i);
				relayNamesEditTextArray[i].setText(channelType+(i+1));
			}
			TextView tView1 = new TextView(this);
			tView1.setText(R.string.button_is_momentary);
			tView1.setTextColor(Color.GREEN);
			tView1.setTextSize(10);
			tView1.setGravity(Gravity.RIGHT);
			tView1.setPadding(24,0,8,0);

			momentaryCheckBoxArray[i] = new CheckBox(this);
			if(momentaryIntArray[i] == 0){
				momentaryCheckBoxArray[i].setChecked(false);
			}else{
				momentaryCheckBoxArray[i].setChecked(true);
			}
			tableRow1.addView(tView1);
			tableRow1.addView(momentaryCheckBoxArray[i]);
			if(pwm || linearActuator){
				tableRow1.setVisibility(View.GONE);
			}
			
			
			tableRow.addView(tView);
			tableRow.addView(relayNamesEditTextArray[i]);
			
			editRelayNamesTable.addView(tableRow);
			editRelayNamesTable.addView(tableRow1);
			
		}
		System.out.println("editRelayNamesTable built");
		if(subTable == null){
			System.out.println("subTable is null");
		}
		subTable.addView(editRelayNamesTable);
		
		if(!displayRelays){
			editRelayNamesTable.setVisibility(View.GONE);
		}
		if(pwm || linearActuator){
			editRelayNamesTable.setVisibility(View.VISIBLE);
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
	}

	@Override
	protected void onResume() {
		super.onResume();
	}
	
	public class MyGestureDetector extends SimpleOnGestureListener
    {
    	@Override
    	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY){
    		if(e1 != null && e2 != null){
    			if(e1.getX()-e2.getX() > 150){
        			return true;
        		}
    		}
    		
    		
			return false;
    		
    	}
    }
	

}
