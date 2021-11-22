package com.controlanything.NCDTCPRelay;

import com.controlanything.NCDTCPRelay.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class EventListActivity extends Activity{
	
	ControlPanel cPanel;
	Typeface font;
	AnimationDrawable saveButtonAnimation;
	
	//Global Views
	//RelativeLayout title;
	EditText eventName;
	
	//Global Variables
	int currentapiVersion = android.os.Build.VERSION.SDK_INT;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		cPanel = ((ControlPanel)getApplicationContext());
		font = Typeface.createFromAsset(getAssets(), "fonts/neuropolxfree.ttf");
		setContentView(mainView());
		
		if(currentapiVersion>=11){
			saveButtonAnimation.start();
		}
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		finish();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}
	
	@Override
	public boolean onKeyDown(int keycode, KeyEvent e) {
	    switch(keycode) {
	        case KeyEvent.KEYCODE_BACK:
	        	Intent eventListIntent = new Intent(getBaseContext(), DeviceListActivity.class);
				startActivity(eventListIntent);
				finish();
	    }
		return true;
	}
	
	public RelativeLayout mainView(){
		RelativeLayout rLayout = new RelativeLayout(this);
		rLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		rLayout.setLongClickable(true);
		rLayout.setOnLongClickListener(longClickListener);

		rLayout.setBackgroundResource(R.drawable.background);
		
		//rLayout.addView(title());
		
		RelativeLayout.LayoutParams scrollViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		//scrollViewParams.addRule(RelativeLayout.BELOW, title.getId());
		scrollViewParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);

		rLayout.addView(scrollView(), scrollViewParams);
		
		RelativeLayout.LayoutParams bottomButtonParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 171);
		bottomButtonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		rLayout.addView(deviceListButton(), bottomButtonParams);	
		
		return rLayout;
	}

	/*
	public RelativeLayout title(){
		title = new RelativeLayout(this);
		title.setBackgroundResource(R.drawable.top_bar);
		title.setId(1);
		
		TextView tView = new TextView(this);
		tView.setText("Events");
		tView.setTypeface(font);
		tView.setTextSize(30);
		tView.setTextColor(Color.BLACK);
		RelativeLayout.LayoutParams titleLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		
		titleLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
		title.addView(tView,titleLayoutParams);
		
		return title;
	}

	 */
	
	public ScrollView scrollView(){
		ScrollView sView = new ScrollView(this);
		sView.setPadding(0, 0, 0, 171);
		sView.setOnLongClickListener(longClickListener);
//		sView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
		
		sView.addView(eventsList());
		
		return sView;
	}
	
	public ListView eventsList(){
		ListView lView = new ListView(this);
		String[] storedEvents = cPanel.getStoredString("EVENTS").split(";");
		
		ArrayAdapter<String> eventListAdapter =  new ArrayAdapter<String>(this, R.layout.device_name);
		lView.setAdapter(eventListAdapter);
		lView.setOnItemClickListener(eventSelected);
		lView.setOnItemLongClickListener(eventLongClick);
		
		if(storedEvents[0].equalsIgnoreCase("n/a")){
			eventListAdapter.add("Click and hold to add new Event");
		}else{
			for(int i = 0; i<storedEvents.length; i++){
				eventListAdapter.add(storedEvents[i]);
			}
		}
		return lView;
		
	}
	
	public ImageView deviceListButton(){
		ImageView bottomButton = new ImageView(this);
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
					finish();
					
				}
				
			});
		}
		
		return bottomButton;
	}
	
	private OnItemClickListener eventSelected = new OnItemClickListener(){
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) 
		{
			String eventName = ((TextView) arg1).getText().toString();
			System.out.println(eventName+" clicked");
		}
	};
	
	private OnItemLongClickListener eventLongClick = new OnItemLongClickListener(){

		public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
				int arg2, long arg3) {
			
			return false;
		}
		
	};
	
	private OnLongClickListener longClickListener = new OnLongClickListener(){

		public boolean onLongClick(View arg0) {
			System.out.println("Long Click");
			return false;
		}
		
	};
	
	
}
