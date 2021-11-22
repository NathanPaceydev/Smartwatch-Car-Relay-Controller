package com.controlanything.NCDTCPRelay;

import java.util.Arrays;

import com.controlanything.NCDTCPRelay.R;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsoluteLayout;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class ADSetupActivity extends Activity {
	
	//Global Variables for Views
		int textColor = Color.WHITE;
		int subTextSize = 20;
		int textBoxWidth = 270;
		
	//Global Views
	//RelativeLayout titleTable;
		
	String[] storedNames;
	int[] inputTypes;
	
	String deviceMacAddress;
	Typeface font;
	ControlPanel cPanel;
	int displayHeight;
	int displayWidth;
	int totalWidth;
	
	
	EditText[] inputNames;
	Spinner[] inputTypeSpinnerArray;
	CheckBox[] pBarChecks;
	EditText vRefEditText;
	ImageView sButton;
	
	String[] iNames;
	int[] iTypes;
	String vRef;
	
	GestureDetector gDetector;
	AnimationDrawable saveButtonAnimation;
	int currentapiVersion = android.os.Build.VERSION.SDK_INT;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		cPanel = ((ControlPanel)getApplicationContext());
		
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		deviceMacAddress = extras.getString("MAC");
		
		font = Typeface.createFromAsset(getAssets(), "fonts/neuropolxfree.ttf");
		displayHeight = this.getWindow().getWindowManager().getDefaultDisplay().getHeight();
		displayHeight = (displayHeight - getStatusBarHeight());
		displayWidth = this.getWindow().getWindowManager().getDefaultDisplay().getWidth();
		
		gDetector = new GestureDetector(getApplicationContext(), new MyGestureDetector());
		
		inputNames = new EditText[8];
		vRefEditText = new EditText(this);
		iNames = new String[8];
		inputTypeSpinnerArray = new Spinner[8];
		iTypes = new int[8];
		
		getStoredInfo();
		
		setContentView(mTable());
		if(currentapiVersion>=11){
			saveButtonAnimation.start();
		}
		
		
		
	}

	
	
	public void getStoredInfo(){
		
		String storedSettings = cPanel.getStoredString(deviceMacAddress+"Inputs");
		
		if(storedSettings != "n/a"){
			//TODO set all strings to this info.
			String[] storedSettingsSplit = storedSettings.split(";");
			storedNames = new String[8];
			inputTypes = new int[8];
			for(int i = 0; i<8; i++){
				String[] deviceInfo = storedSettingsSplit[i].split("~");
				iNames[i]=deviceInfo[0];
				iTypes[i]=Integer.parseInt(deviceInfo[1]);
			}
			if(storedSettingsSplit.length>8){
				if(storedSettingsSplit[8]!=null){
					vRef = storedSettingsSplit[8];
				}
				
			}
			
		}
		
	}
	
	public RelativeLayout mTable(){
		//Create and Define Master Layout that will hold all views.
		RelativeLayout table = new RelativeLayout(this);
		table.setBackgroundResource(R.drawable.background);
		table.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		table.setFocusable(true);
		table.setFocusableInTouchMode(true);
		
		//Add Title View to Main View
		//table.addView(title());
		
		
		
		//Set layout rules for bottom button
		RelativeLayout.LayoutParams bottomButtonParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 171);
		bottomButtonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		//Add button to master view
		table.addView(saveButton(), bottomButtonParams);
		
		//Set layout rules for ScrollView
		RelativeLayout.LayoutParams scrollViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		//scrollViewParams.addRule(RelativeLayout.BELOW, titleTable.getId());
		scrollViewParams.addRule(RelativeLayout.ABOVE, sButton.getId());
		//Add ScrollView to master view.
		table.addView(sView(), scrollViewParams);
		
		table.setFocusable(true);
		
		return table;
		
	}

	/*
	public RelativeLayout title(){
		titleTable = new RelativeLayout(this);
		titleTable.setBackgroundResource(R.drawable.top_bar);
		titleTable.setId(1);
		
//		table.setLayoutParams(new LayoutParams(displayWidth,(int) convertPixelsToDp(248, this)));
		
		final TextView tView = new TextView(this);
//		tView.setPadding(15, 70, 0, 0);
		tView.setText("AD SetUp");

		//tView.setText("A/D Configuration\n\n"+deviceMacAddress);
		tView.setTypeface(font);
		tView.setTextSize(24);
		tView.setTextColor(Color.WHITE);
		RelativeLayout.LayoutParams titleLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		
		titleLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
		
		titleTable.addView(tView,titleLayoutParams);
		
		return titleTable;
	}

	 */
	
	private ScrollView sView(){
		ScrollView scrollView = new ScrollView(this);
		scrollView.setFadingEdgeLength(0);
//		scrollView.setPadding(0, 0, 0, 171);
		
		scrollView.addView(scrollTable());
		
		
//		scrollView.setOnTouchListener(new View.OnTouchListener() {
//			
//			public boolean onTouch(View v, MotionEvent event) {
//				if(gDetector.onTouchEvent(event)){
//					
//					//Save entered settings
//					saveSettings();
//					
//					//Switch to Relay Settings
//					Intent relaySettingsIntent = new Intent(getApplicationContext(), SettingsPageActivity.class);
//					relaySettingsIntent.putExtra("MAC", deviceMacAddress);
//					startActivity(relaySettingsIntent);
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
			sButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 171));
			
			saveButtonAnimation = (AnimationDrawable)sButton.getDrawable();
			saveButtonAnimation.setEnterFadeDuration(1500);
			saveButtonAnimation.setExitFadeDuration(1500);
			
			sButton.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					saveSettings();
					Intent mainSettings = new Intent(getApplicationContext(), SettingsPageActivity.class);
					mainSettings.putExtra("MAC", deviceMacAddress);
					startActivity(mainSettings);
					finish();
				}
				
			});
		}else{
			sButton.setImageResource(R.drawable.bottom_bar_button);
			//sButton.setBackgroundResource(R.drawable.bottom_bar);
			sButton.setPadding(0, 10, 0, 10);
			sButton.setMinimumHeight(120);
			sButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 171));
			
			sButton.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					saveSettings();
					Intent mainSettings = new Intent(getApplicationContext(), SettingsPageActivity.class);
					mainSettings.putExtra("MAC", deviceMacAddress);
					startActivity(mainSettings);
					finish();
				}
				
			});
		}
		
		return sButton;
	}
	
	private TableLayout scrollTable(){
		TableLayout subTable = new TableLayout(this);
		TableRow vRefRow = new TableRow(this);
		TextView vRefText = new TextView(this);
		vRefText.setText("Voltage Reference");
		vRefText.setTextColor(Color.WHITE);
		vRefText.setTextSize(subTextSize);
		
		vRefEditText.setBackgroundResource(R.drawable.textbox);
		vRefEditText.getBackground().setAlpha(180);
		vRefEditText.setTextColor(Color.WHITE);
		vRefEditText.setWidth(textBoxWidth);
		vRefEditText.setSingleLine();
		vRefEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
		if(vRef != null){
			vRefEditText.setText(vRef);
		}else{
			vRefEditText.setText("4.926");
		}
		vRefRow.addView(vRefText);
		vRefRow.addView(vRefEditText);
		subTable.addView(vRefRow);
		int totalWidth = vRefRow.getLayoutParams().width;
		
		
		for(int i = 0; i < 8; i++){
			
			//Name Row
			TableRow nameRow = new TableRow(this);
			TextView tView = new TextView(this);			
			tView.setText("Input "+(i+1));
			tView.setTextColor(Color.WHITE);
			tView.setTextSize(subTextSize);			
			
			inputNames[i] = new EditText(this);
			inputNames[i].setBackgroundResource(R.drawable.textbox);
			inputNames[i].getBackground().setAlpha(180);
			inputNames[i].setTextColor(Color.WHITE);
			inputNames[i].setWidth(textBoxWidth);
			inputNames[i].setSingleLine();
			inputNames[i].setImeOptions(EditorInfo.IME_ACTION_DONE);
			
			if(iNames[i] != null){
				inputNames[i].setText(iNames[i]);
			}
			else{
				inputNames[i].setText(tView.getText().toString());
			}
			nameRow.setPadding(0, 20, 0, 0);
			nameRow.addView(tView);
			nameRow.addView(inputNames[i]);
			subTable.addView(nameRow);
			
			//Input TypeRow
			TableRow typeRow = new TableRow(this);
			TextView tViewTrash = new TextView(this);
			tViewTrash.setText("Input "+(i+1)+" Type");
			tViewTrash.setTextColor(Color.WHITE);
			tViewTrash.setTextSize(subTextSize);
			
			inputTypeSpinnerArray[i] = new Spinner(this);
			ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
					this, R.array.inputType, R.layout.spinner_item);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			inputTypeSpinnerArray[i].setAdapter(adapter);
			inputTypeSpinnerArray[i].setSelection(iTypes[i]);
			inputTypeSpinnerArray[i].setOnItemSelectedListener(new SpinnerListener());
			inputTypeSpinnerArray[i].setBackgroundResource(R.drawable.spinner_box);
			inputTypeSpinnerArray[i].getBackground().setAlpha(180);
//			inputTypeSpinnerArray[i].setLayoutParams(new LayoutParams(textBoxWidth, 75));
//			inputTypeSpinnerArray[i].setX(246);
			typeRow.addView(tViewTrash);
			typeRow.addView(inputTypeSpinnerArray[i]);
			
			subTable.addView(typeRow);
			
		}
		
		return subTable;
	}
	
	private class SpinnerListener implements OnItemSelectedListener{

		public void onItemSelected(AdapterView<?> parent,
        		View view, int pos, long id) {
			
		}
		public void onNothingSelected(AdapterView<?> arg0) {
		}
	}
	
	public void saveSettings(){
		//TODO save settings.
		String stringToSave = null;
		for(int i = 0; i < inputNames.length; i++){
			if(i == 0){
				stringToSave = inputNames[i].getText().toString()+"~"+inputTypeSpinnerArray[i].getSelectedItemPosition()+";";
			}else{
				stringToSave = stringToSave+inputNames[i].getText().toString()+"~"+inputTypeSpinnerArray[i].getSelectedItemPosition()+";";
			}
		}
		vRef = vRefEditText.getText().toString();
		cPanel.saveString(deviceMacAddress+"Inputs", stringToSave+vRef);
		
	}
	
	public int getStatusBarHeight() {
	  	  int result = 0;
	  	  int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
	  	  if (resourceId > 0) {
	  	      result = getResources().getDimensionPixelSize(resourceId);
	  	  }
	  	  return result;
	  	}
	
	public class MyGestureDetector extends SimpleOnGestureListener
    {
    	@Override
    	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY){
    		if(e1 != null && e2 != null){
    			if(e2.getX()-e1.getX() > 150){
        			return true;
        		}
    		}
    		
    		
			return false;
    		
    	}
    }

}
