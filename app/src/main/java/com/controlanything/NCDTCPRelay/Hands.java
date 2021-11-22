package com.controlanything.NCDTCPRelay;

import com.controlanything.NCDTCPRelay.R;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class Hands extends Activity {
	
	ControlPanel cPanel;
	String ipAddress;
	int port;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_hands);
		Intent i = getIntent();
		String mac = i.getStringExtra("MAC");
		
		cPanel = ((ControlPanel)getApplicationContext());
		
		String[] storedInfo = cPanel.getStoredString(mac).split(";");
		ipAddress = storedInfo[1];
		port = Integer.parseInt(storedInfo[2]);
		
		cPanel.connect(ipAddress, port);
		
		Button startButton = (Button) findViewById(R.id.start);
		Button stopButton = (Button) findViewById(R.id.stop);
		startButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				cPanel.cycleRelays("start");
				
			}
		});
		
		stopButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				cPanel.cycleRelays("stop");
				Intent listView = new Intent(getApplicationContext(), DeviceListActivity.class);
				startActivity(listView);
				if(cPanel.connected == true){
					cPanel.disconnect();
				}
				finish();
				
			}
		});
	}

}
