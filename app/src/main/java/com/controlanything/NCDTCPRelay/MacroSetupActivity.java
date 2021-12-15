package com.controlanything.NCDTCPRelay;

import java.util.ArrayList;
import java.util.Arrays;

import android.R.drawable;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class MacroSetupActivity extends Activity {
	
	Typeface font;
	int currentapiVersion = android.os.Build.VERSION.SDK_INT;
	AnimationDrawable saveButtonAnimation;
	ControlPanel cPanel;
	
	//Global Views
	RelativeLayout titleTable;
	ImageView sButton;
	Button aButton;
	EditText[] nameEditTexts;
	ScrollView scrollView;
	TableRow[] tableRows;
	TextView bottomText;
	
	//Global Variables
	String deviceMacAddress;
	ArrayList<String> macroIDs;
	ArrayList<String> macros;
	
	//Global Variables for Views
	int textColor = Color.WHITE;
	int subTextSize = 20;
	int textBoxWidth = 270;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		deviceMacAddress = extras.getString("MAC");
		
		cPanel = ((ControlPanel)getApplicationContext());
		
		macroIDs = new ArrayList<String>();
		macros = new ArrayList<String>();
		
		getStoredSettings();
		
		font = Typeface.createFromAsset(getAssets(), "fonts/neuropolxfree.ttf");
		
		setContentView(mTable());

		if(currentapiVersion>=11){
			saveButtonAnimation.start();
		}
	}
	
	private RelativeLayout mTable(){
		//Create and Define Master Layout that will hold all views.
		RelativeLayout table = new RelativeLayout(this);
		table.setBackgroundResource(R.drawable.background);
		table.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		table.setFocusable(true);
		table.setFocusableInTouchMode(true);
		
		//Add Title View to Main View
		table.addView(title());
		
		
		
		//Set layout rules for bottom button
		RelativeLayout.LayoutParams bottomButtonParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 171);
		bottomButtonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		//Add button to master view
		table.addView(saveButton(), bottomButtonParams);
		
		//Set layout rules for add button
		RelativeLayout.LayoutParams addButtonParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, 171);
		addButtonParams.addRule(RelativeLayout.ABOVE, sButton.getId());
		table.addView(addButton(), addButtonParams);
		
		//Set layout rules for Bottom Text
		RelativeLayout.LayoutParams bottomTextParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		bottomTextParams.addRule(RelativeLayout.ABOVE, aButton.getId());
		table.addView(bottomText(), bottomTextParams);
		
		//Set layout rules for ScrollView
		RelativeLayout.LayoutParams scrollViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		scrollViewParams.addRule(RelativeLayout.BELOW, titleTable.getId());
		scrollViewParams.addRule(RelativeLayout.ABOVE, bottomText.getId());
		//Add ScrollView to master view.
		table.addView(sView(), scrollViewParams);
		
		table.setFocusable(true);
		
		return table;
		
	}
	
	private RelativeLayout title(){
		titleTable = new RelativeLayout(this);
		//titleTable.setBackgroundResource(R.drawable.top_bar);
		titleTable.setId(1);
				
		final TextView tView = new TextView(this);
		tView.setText("Macro Configuration\n\n"+deviceMacAddress);
		tView.setTypeface(font);
		tView.setTextSize(24);
		tView.setTextColor(Color.BLACK);
		RelativeLayout.LayoutParams titleLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		
		titleLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
		
		titleTable.addView(tView,titleLayoutParams);
		
		return titleTable;
	}
	
	private ScrollView sView(){
		scrollView = new ScrollView(this);
		scrollView.setFadingEdgeLength(0);	
		scrollView.addView(scrollTable());
		return scrollView;
	}
	
	private TextView bottomText(){
		bottomText = new TextView(this);
		bottomText.setText("Click Add Hold Macros to Edit/Remove");
		bottomText.setTextColor(textColor);
		bottomText.setTextSize(12);
		bottomText.setId(4);
		return bottomText;
		
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
	
	private Button addButton(){
		aButton = new Button(this);
		aButton.setId(3);
		
		if(currentapiVersion>=11){
			aButton.setPadding(0, 10, 0, 10);
			aButton.setMinimumHeight(120);
			aButton.setText("Add Macro");
			
			aButton.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					addMacro();
				}
				
			});
		}else{
			aButton.setPadding(0, 10, 0, 10);
			aButton.setMinimumHeight(120);
			aButton.setText("Add Macro");
			
			aButton.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					addMacro();
				}
				
			});
		}
		
		return aButton;
		
	}
	
	private TableLayout scrollTable(){
		TableLayout subTable = new TableLayout(this);
		subTable.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		
		String[] macroNames = new String[macros.size()];
		
		for(int i = 0; i < macroNames.length; i++){
			macroNames[i] = macros.get(i);
		}

		if(macroNames.length > 0){
			if(macroNames.length == 1 && macroNames[0].equalsIgnoreCase("n/a")){
				System.out.println("No Stored Macros");
				TableRow tRow = new TableRow(this);
				TextView tView = new TextView(this);
				tView.setText("Click Add Button Below to add Macro");
				tView.setTextColor(textColor);
				tView.setTextSize(subTextSize);
				tRow.addView(tView);
				subTable.addView(tRow);
			}else{
				tableRows = new TableRow[macroNames.length];
				nameEditTexts = new EditText[macroNames.length];

				for(int i = 0; i < macroNames.length; i++){
					tableRows[i] = new TableRow(this);
					tableRows[i].setTag(macroIDs.get(i));

					TextView tView = new TextView(this);
					tView.setText("Macro "+(i+1)+ " Name: ");
					tView.setTextColor(textColor);
					tView.setTextSize(subTextSize);

					nameEditTexts[i] = new EditText(this);
					nameEditTexts[i].setBackgroundResource(R.drawable.textbox);
					nameEditTexts[i].getBackground().setAlpha(180);
					nameEditTexts[i].setTextColor(textColor);
					nameEditTexts[i].setWidth(textBoxWidth);
					nameEditTexts[i].setSingleLine();
					nameEditTexts[i].setImeOptions(EditorInfo.IME_ACTION_DONE);
					nameEditTexts[i].setTag(tableRows[i].getTag());
					nameEditTexts[i].setOnEditorActionListener(new OnEditorActionListener(){

						public boolean onEditorAction(TextView v, int actionId,
								KeyEvent event) {
							if(actionId == EditorInfo.IME_ACTION_DONE){
								TableRow tRow = (TableRow)v.getParent();
								System.out.println("Done Button pressed on: "+tRow.getTag());
								for(int i = 0; i < macroIDs.size(); i++){
									if(macroIDs.get(i).equalsIgnoreCase(tRow.getTag().toString())){
										macros.set(i, v.getText().toString());
									}
								}
							}
							return false;
						}
						
					});
					if(macroNames[i] != null){
						nameEditTexts[i].setText(macroNames[i]);
					}else{
						nameEditTexts[i].setText("Macro "+(i+1));
					}

					tableRows[i].addView(tView);
					tableRows[i].addView(nameEditTexts[i]);
					
					System.out.println("Assigning tag "+tableRows[i].getTag()+" to TableRow");
					tableRows[i].setOnClickListener(new View.OnClickListener() {

						public void onClick(View v) {
							final String macroID = (String)v.getTag();
							System.out.println(macroID+" clicked");

						}
					});
					tableRows[i].setOnLongClickListener(new View.OnLongClickListener() {

						public boolean onLongClick(View v) {
							System.out.println("Table Row Long Clicked");
							final String macroID = (String)v.getTag();

							final AlertDialog.Builder removeDeviceAlert = new AlertDialog.Builder(MacroSetupActivity.this);
							removeDeviceAlert.setTitle("Edit/Remove");
							removeDeviceAlert.setCancelable(true);
							removeDeviceAlert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog, int which) {


								}
							});
							removeDeviceAlert.setItems(R.array.MacroAlertDialogOptions, new DialogInterface.OnClickListener() {
								
								public void onClick(DialogInterface dialog, int which) {
									//Edit Macro
									if(which == 0){
										Intent macroSetupIntent = new Intent(getApplicationContext(), MacroConfigurationActivity.class);
										macroSetupIntent.putExtra("MAC", deviceMacAddress);
										macroSetupIntent.putExtra("MACROID", macroID);
										startActivity(macroSetupIntent);
										finish();
									}
									//Remove Macro
									if(which == 1){
										deleteMacro(macroID);
									}
									
								}
							});
							AlertDialog removeDeviceDialog = removeDeviceAlert.create();
							removeDeviceDialog.show();

							return true;
						}
					});
					tableRows[i].setOnTouchListener(new View.OnTouchListener() {

						public boolean onTouch(View v, MotionEvent event) {
							if(event.getAction() == MotionEvent.ACTION_DOWN){
								v.setBackgroundResource(drawable.list_selector_background);
							}
							return false;
						}
					});
					subTable.addView(tableRows[i]);
				}
			}
		}else{
			System.out.println("No Stored Macros");
			TableRow tRow = new TableRow(this);
			TextView tView = new TextView(this);
			tView.setText("Click Add Button Below to add Macro");
			tView.setTextColor(textColor);
			tView.setTextSize(subTextSize);
			tRow.addView(tView);
			subTable.addView(tRow);
		}
		return subTable;
	}
	
	private void getStoredSettings(){
		if(cPanel.getStoredString(deviceMacAddress+"Macros").equalsIgnoreCase("n/a")){
			System.out.println("no stored Macros");
			return;
		}
		
		//get stored Macro IDs
		String[] storedMacroIDs = cPanel.getStoredString(deviceMacAddress+"Macros").split(";");
		
		
		
		//Clear the macroID array and macros array
		macroIDs.clear();
		macros.clear();
		
		//Populate macroID array
		for(String macroID : storedMacroIDs){
			macroIDs.add(macroID);
		}
		
		//Get names of each macro using macroIDs
		for(int i = 0; i < macroIDs.size(); i++){
			String[] macroInfo = cPanel.getStoredString(deviceMacAddress+macroIDs.get(i)).split("~");
			if(macroInfo.length == 0){
				macros.add("Enter Name");
			}else{
				macros.add(macroInfo[0]);
			}
			
		}
		
	}
	
	private void addMacro(){
		
		int currentValue = 0;
		for(int i = 0; i < macroIDs.size(); i++){
			String subString = macroIDs.get(i).substring(5);
			int value = Integer.parseInt(subString);
			if(value > currentValue){
				currentValue = value;
			}
		}
		int macroIDNumber = currentValue+1;
		
		macroIDs.add("Macro"+macroIDNumber);
		System.out.println("Adding Macro"+macroIDs.size());
		macros.add("Enter Name");

		scrollView.removeAllViews();
		scrollView.addView(scrollTable());
	}
	
	private void deleteMacro(String macroID){
		System.out.println("Removing "+macroID);
		if(!cPanel.getStoredString(macroID).equalsIgnoreCase("n/a")){
			cPanel.deleteString(macroID);
		}
		
		for(int i = 0; i < macroIDs.size(); i++){
			System.out.println("Comparing "+macroIDs.get(i) +" to " + macroID);
			if(macroIDs.get(i).equalsIgnoreCase(macroID)){
				System.out.println("Found it, removing now");
				macroIDs.remove(i);
				macros.remove(i);
			}
		}
		
		
		scrollView.removeAllViews();
		scrollView.addView(scrollTable());
	}
	
	private void saveSettings(){
		if(nameEditTexts != null){
			StringBuilder sBuilder = new StringBuilder();
			for(int i = 0; i < macroIDs.size(); i++){
				String macroID = nameEditTexts[i].getTag().toString();
				//Build stored Macros String
				if(sBuilder.length() == 0){
					sBuilder.append(macroID);
				}else{
					sBuilder.append(";"+macroID);
				}
				
				//Build stored Macro string for each macro
				if(cPanel.getStoredString(deviceMacAddress+macroID).equalsIgnoreCase("n/a")){
					cPanel.saveString(deviceMacAddress+macroID, nameEditTexts[i].getText().toString()+"~");
				}else{
					System.out.println("Stored String for "+macroID+cPanel.getStoredString(deviceMacAddress+"Macro"+(i+1)));
					String[] currentString = cPanel.getStoredString(deviceMacAddress+macroID).split("~");
					if(currentString.length < 2){
						cPanel.saveString(deviceMacAddress+macroID, nameEditTexts[i].getText().toString()+"~");
						System.out.println("1saveSettings saving this String for "+macroID+ nameEditTexts[i].getText().toString()+"~");
					}else{
						System.out.println(Arrays.toString(currentString));
						cPanel.saveString(deviceMacAddress+macroID, nameEditTexts[i].getText().toString()+"~"+currentString[1]);
						System.out.println("2saveSettings saving this String for "+macroID+ nameEditTexts[i].getText().toString()+"~");
					}
					
				}
			}
			if(!sBuilder.toString().contains(";")){
				sBuilder.append(";");
			}
			cPanel.saveString(deviceMacAddress+"Macros", sBuilder.toString());
			System.out.println("saveSettings saving String: "+sBuilder.toString());
		}else{
			cPanel.saveString(deviceMacAddress+"Macros", "n/a");
		}
		
		
		
	}

	@Override
	protected void onPause() {
		saveSettings();
		super.onPause();
	}
	
	
	
}
