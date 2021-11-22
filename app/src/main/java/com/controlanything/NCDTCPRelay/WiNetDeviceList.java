package com.controlanything.NCDTCPRelay;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.controlanything.NCDTCPRelay.R;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class WiNetDeviceList extends Activity{
	
	//Global Objects
	Intent callingIntent;
	ControlPanel cPanel;
	Bundle extras;
	public ArrayAdapter<String> devicesArrayAdapter;
	Typeface font;
	AnimationDrawable saveButtonAnimation;
	
	//global variables
	String wiNetIP;
	String httpRequest = "/cgi-bin/getDeviceList.cgi?19";
	int currentapiVersion = android.os.Build.VERSION.SDK_INT;
	String wiNetMac;
	String savedDevices;
	boolean pwm=false;
	boolean linearActuator=false;
	
	//global Views
	ListView listView;
	RelativeLayout titleTextView;
	ImageView bottomButton;
	
	
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		cPanel = ((ControlPanel)getApplicationContext());
		savedDevices = cPanel.getStoredString("savedDevices");
		
		font = Typeface.createFromAsset(getAssets(), "fonts/neuropolxfree.ttf");
		
		callingIntent = getIntent();
		extras = callingIntent.getExtras();
		wiNetIP = extras.getString("IP");	
		wiNetMac = extras.getString("MAC");
		String request = "http://"+wiNetIP+httpRequest;
		
		devicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
		devicesArrayAdapter = getWiNetDevicesInfo(request);
		listView = new ListView(this);
		listView.setAdapter(devicesArrayAdapter);
		listView.setOnItemClickListener(deviceSelectedListener);
		
		
		setContentView(mainView());
		if(currentapiVersion>=11){
			saveButtonAnimation.start();
		}
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}
	
	public RelativeLayout mainView(){
		RelativeLayout rLayout = new RelativeLayout(this);
		rLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		rLayout.setBackgroundResource(R.drawable.background);
		
		rLayout.addView(title());
		
		//Set layout rules for bottom button
		RelativeLayout.LayoutParams bottomButtonParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 171);
		bottomButtonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		//Add button to master view
		rLayout.addView(exitButton(), bottomButtonParams);
		
		//Set layout rules for ScrollView
		RelativeLayout.LayoutParams scrollViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		scrollViewParams.addRule(RelativeLayout.BELOW, titleTextView.getId());
		scrollViewParams.addRule(RelativeLayout.ABOVE, bottomButton.getId());
		//Add ScrollView to master view.
		rLayout.addView(listView, scrollViewParams);
		
		
		
		return rLayout;
	}
	
	public RelativeLayout title(){
		titleTextView = new RelativeLayout(this);
		titleTextView.setBackgroundResource(R.drawable.top_bar);

		TextView tView = new TextView(this);
		tView.setText("Scanning");
		tView.setTextColor(Color.BLACK);
		tView.setTextSize(30);
		tView.setTypeface(font);

		titleTextView.setId(1);
		
		RelativeLayout.LayoutParams titleLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		
		titleLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
		
		titleTextView.addView(tView, titleLayoutParams);
		
		return titleTextView;
	}
	
	public ImageView exitButton(){
		
		bottomButton = new ImageView(this);
		bottomButton.setId(2);
		
		if(currentapiVersion>=11){
			bottomButton.setImageResource(R.drawable.animationxmlexit);
			bottomButton.setBackgroundResource(0);
			bottomButton.setPadding(0, 10, 0, 10);
			bottomButton.setMinimumHeight(120);
			bottomButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			bottomButton.setPadding(0, 10, 0, 10);
			bottomButton.setBackgroundResource(R.drawable.bottom_bar);
			
			saveButtonAnimation = (AnimationDrawable)bottomButton.getDrawable();
			saveButtonAnimation.setEnterFadeDuration(750);
			saveButtonAnimation.setExitFadeDuration(750);
			
			
			bottomButton.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					Intent listViewIntent = new Intent(getApplicationContext(), DeviceListActivity.class);
					startActivity(listViewIntent);
					
				}
				
			});
		}else{
			bottomButton.setImageResource(R.drawable.bottom_bar_exit);
			bottomButton.setBackgroundResource(0);
			bottomButton.setPadding(0, 10, 0, 10);
			bottomButton.setMinimumHeight(120);
			bottomButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			bottomButton.setPadding(0, 10, 0, 10);
			bottomButton.setBackgroundResource(R.drawable.bottom_bar);
			
			bottomButton.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					Intent listViewIntent = new Intent(getApplicationContext(), DeviceListActivity.class);
					startActivity(listViewIntent);
					
				}
				
			});
		}
		
		
		
		return bottomButton;
		
	}

	public ArrayAdapter<String> getWiNetDevicesInfo(String request){
		
		HttpClient httpclient = new DefaultHttpClient();
		HttpResponse response;
		String responseString = null;
		
		
		
		ArrayAdapter<String> tempAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
		
		try {
			HttpGet httpget = new HttpGet(request);
			response = httpclient.execute(httpget);
			StatusLine statusLine = response.getStatusLine();
			
			if(statusLine.getStatusCode() == HttpStatus.SC_OK)
			{
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				response.getEntity().writeTo(out);
				
				out.close();
				responseString = out.toString();
				String r = URLDecoder.decode(responseString, "UTF-8");
				responseString = r;
				
				if(responseString != null){
					responseString = responseString.substring(2,responseString.length()-3);
					
					String[] devices = responseString.split("', '");
					System.out.println("response String = "+responseString);
					
					
					for(int i = 0; i < devices.length; i++){
						String individualDeviceInfo = devices[i];
						String[] individualDeviceInfoSplit = individualDeviceInfo.split("~");
						String[] deviceNameSplit = individualDeviceInfoSplit[1].split(":");
						String deviceName = deviceNameSplit[0];
						if(!devicePresent(individualDeviceInfoSplit[0])){
							devicesArrayAdapter.add("Mac/Ser: "+individualDeviceInfoSplit[0]+"\nDevice Type: "+individualDeviceInfoSplit[4]+"\nName: "+deviceName+"\nPort: "+individualDeviceInfoSplit[3]);
						}
					}
					if(devicesArrayAdapter.isEmpty()){
						devicesArrayAdapter.add("No New Devices On WiNet");
					}
					return devicesArrayAdapter;
				}
			}else{
				System.out.println("HTTPStatus not SC_OK, result was "+statusLine.getReasonPhrase());
				devicesArrayAdapter.add("No Devices");
				return devicesArrayAdapter;
			}
			
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			devicesArrayAdapter.add("No Devices");
			return devicesArrayAdapter;
		} catch (IOException e) {
			e.printStackTrace();
			devicesArrayAdapter.add("No Devices");
			return devicesArrayAdapter;
		}
		devicesArrayAdapter.add("No Devices");
		return devicesArrayAdapter;
		
	}
	
	private OnItemClickListener deviceSelectedListener = new OnItemClickListener(){

		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			String[] deviceInfo = ((TextView) arg1).getText().toString().split("\n");
			
			String port = deviceInfo[3].substring(6);
			String shortenedMac = deviceInfo[0].substring(9);
			String deviceName = deviceInfo[2].substring(6);
			
			
			
			
			cPanel.winet = true;
			cPanel.winetMac = shortenedMac;
			if(cPanel.connect(wiNetIP, Integer.parseInt(port))){
				pwm = cPanel.checkPWM();
				linearActuator = cPanel.checkActuator();
			}else{
				Toast toast = Toast.makeText(getBaseContext(), "Could not Communicate with controller", Toast.LENGTH_LONG);
				toast.show();
				return;
			}
			
			if (cPanel.getStoredString("savedDevices") != "n/a"){
				cPanel.saveString("savedDevices", cPanel.getStoredString("savedDevices") +  shortenedMac + ";");
			}
			else{
				cPanel.saveString("savedDevices", shortenedMac + ";");
			}
			
			WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			
			cPanel.saveString(shortenedMac, shortenedMac+";"+wiNetIP+";"+port+";"+"1"+";"+deviceName+";"+wifiInfo.getSSID());
			
			cPanel.saveString(shortenedMac+"-wiNetMac", wiNetMac);
			
			Intent settingsPageIntent = new Intent(getApplicationContext(), SettingsPageActivity.class);
			settingsPageIntent.putExtra("IP", wiNetIP);
			
			settingsPageIntent.putExtra("PORT", port);
			settingsPageIntent.putExtra("MAC", shortenedMac);
			settingsPageIntent.putExtra("WINET", "true");
			if(pwm){
				settingsPageIntent.putExtra("PWM", "true");
			}
			if(linearActuator){
				settingsPageIntent.putExtra("ACTUATOR", linearActuator);
			}
			
			
			startActivity(settingsPageIntent);
			finish();
			return;
		}
		
	};
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	public boolean devicePresent(String mac){
		//get stored devices
		if(savedDevices.contains(mac)){
			return true;
		}
		
		
		return false;
		
	}
	
	

}
