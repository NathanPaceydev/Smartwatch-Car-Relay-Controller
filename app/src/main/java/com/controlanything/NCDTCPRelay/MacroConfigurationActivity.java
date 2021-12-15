package com.controlanything.NCDTCPRelay;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableRow;
import android.widget.TextView;

public class MacroConfigurationActivity extends Activity{
	
	Typeface font;
	int currentapiVersion = android.os.Build.VERSION.SDK_INT;
	AnimationDrawable saveButtonAnimation;
	ControlPanel cPanel;
	
	//Global Views
	RelativeLayout titleTable;
	ImageView sButton;
	Button addDownActionbutton;
	Button addDownDelayButton;
	Button addUpActionButton;
	Button addUpDelayButton;
	EditText[] nameEditTexts;
	ScrollView scrollView;
	TableRow[] tableRows;
	TextView bottomText;
	ListView downActionListView;
	ListView upActionListView;
	
	//Global Variables
	String deviceMacAddress;
	String macroID;
	String macroName;
	ArrayList<ActionObject> downActionList;
	ArrayList<ActionObject> upActionList;
	ActionAdapter downActionAdapter;
	ActionAdapter upActionAdapter;
	
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
		macroID = extras.getString("MACROID");
				
		cPanel = ((ControlPanel)getApplicationContext());
		
		downActionList =  new ArrayList<ActionObject>();
		
		upActionList =  new ArrayList<ActionObject>();
		
		
		getStoredSettings();
		
		font = Typeface.createFromAsset(getAssets(), "fonts/neuropolxfree.ttf");
		
		setContentView(mTable());
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
		
		//Set layout rules for mainActionView layout
		RelativeLayout.LayoutParams actionViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
		actionViewParams.addRule(RelativeLayout.ABOVE, sButton.getId());
		actionViewParams.addRule(RelativeLayout.BELOW, titleTable.getId());
		table.addView(mainActionView(), actionViewParams);

		table.setFocusable(true);
		
		return table;
		
	}
	
	private RelativeLayout title(){
		titleTable = new RelativeLayout(this);
		//titleTable.setBackgroundResource(R.drawable.top_bar);
		titleTable.setId(1);
				
		final TextView tView = new TextView(this);
		tView.setText(macroName);
		tView.setTypeface(font);
		tView.setTextSize(24);
		tView.setTextColor(Color.BLACK);
		RelativeLayout.LayoutParams titleLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		
		titleLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
		
		titleTable.addView(tView,titleLayoutParams);
		
		return titleTable;
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
//					saveSettings();
					Intent macroSettings = new Intent(getApplicationContext(), MacroSetupActivity.class);
					macroSettings.putExtra("MAC", deviceMacAddress);
					startActivity(macroSettings);
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
//					saveSettings();
					Intent macroSettings = new Intent(getApplicationContext(), MacroSetupActivity.class);
					macroSettings.putExtra("MAC", deviceMacAddress);
					startActivity(macroSettings);
					finish();
				}
				
			});
		}
		
		return sButton;
	}
	
	private LinearLayout mainActionView(){
		LinearLayout lLayout = new LinearLayout(this);
		lLayout.setOrientation(LinearLayout.VERTICAL);
		lLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
		
		LinearLayout.LayoutParams downActionViewParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		downActionViewParams.weight = 1;
		lLayout.addView(downActionLayout(), downActionViewParams);
		
		LinearLayout.LayoutParams upActionViewParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		upActionViewParams.weight = 1;
		lLayout.addView(upActionLayout(), upActionViewParams);
		
		return lLayout;
		
	}
	
	private RelativeLayout downActionLayout(){
		RelativeLayout rLayout = new RelativeLayout(this);
		
		RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		buttonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		rLayout.addView(addDownActionButton(), buttonParams);
		
		RelativeLayout.LayoutParams listViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		listViewParams.addRule(RelativeLayout.ABOVE, addDownActionbutton.getId());
		listViewParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		rLayout.addView(downActionListView(), listViewParams);
		
		return rLayout;
		
	}
	
	private ListView downActionListView(){
		downActionListView = new ListView(this);
		downActionListView.setId(5);
		downActionAdapter = new ActionAdapter(this,downActionListView.getId() ,downActionList);
		downActionListView.setAdapter(downActionAdapter);
		downActionListView.setOnItemClickListener(new OnItemClickListener(){

			public void onItemClick(AdapterView<?> arg0, View v, int index,
					long arg3) {
				TextView tView1 = (TextView)((ViewGroup)v).getChildAt(2);
				TextView tView2 = (TextView)((ViewGroup)v).getChildAt(3);
				String command = tView1.getText().toString();
				String delay = tView2.getText().toString();
				ActionAdapter adapter = downActionAdapter;
				editCommandDialog(command, delay, adapter, index).show();				
			}
			
		});
		downActionListView.setOnItemLongClickListener(new OnItemLongClickListener(){

			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int index, long arg3) {
				removeCommandDialog(downActionAdapter, index).show();;
				return true;
			}
			
		});
		
		return downActionListView;
	}
	
	private Button addDownActionButton(){
		addDownActionbutton = new Button(this);
		addDownActionbutton.setId(3);
		addDownActionbutton.setText("Add Down Action");
		addDownActionbutton.setPadding(0, 10, 0, 10);
		addDownActionbutton.setMinimumHeight(120);
		addDownActionbutton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				ActionObject newAction = new ActionObject("254,108,1", 50);
				downActionAdapter.add(newAction);
			}
		});
		
		return addDownActionbutton;
	}
	
	private RelativeLayout upActionLayout(){
		RelativeLayout rLayout = new RelativeLayout(this);
		
		RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		buttonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		rLayout.addView(addUpActionButton(), buttonParams);
		
		RelativeLayout.LayoutParams listViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		listViewParams.addRule(RelativeLayout.ABOVE, addUpActionButton.getId());
		listViewParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		rLayout.addView(upActionListView(), listViewParams);
		
		return rLayout;
		
	}
	
	private ListView upActionListView(){
		upActionListView = new ListView(this);
		upActionListView.setId(6);
		upActionAdapter = new ActionAdapter(this, upActionListView.getId(), upActionList);
		upActionListView.setAdapter(upActionAdapter);
		upActionListView.setOnItemClickListener(new OnItemClickListener(){

			public void onItemClick(AdapterView<?> arg0, View v, int index,
					long arg3) {
				TextView tView1 = (TextView)((ViewGroup)v).getChildAt(2);
				TextView tView2 = (TextView)((ViewGroup)v).getChildAt(3);
				String command = tView1.getText().toString();
				String delay = tView2.getText().toString();
				ActionAdapter adapter = upActionAdapter;
				editCommandDialog(command, delay, adapter, index).show();				
			}
			
		});
		upActionListView.setOnItemLongClickListener(new OnItemLongClickListener(){

			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int index, long arg3) {
				removeCommandDialog(upActionAdapter, index).show();
				return true;
			}
			
		});
		
		return upActionListView;
	}
	
	
	
	
	private Button addUpActionButton(){
		addUpActionButton = new Button(this);
		addUpActionButton.setId(4);
		addUpActionButton.setText("Add Up Action");
		addUpActionButton.setPadding(0, 10, 0, 10);
		addUpActionButton.setMinimumHeight(120);
		addUpActionButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				ActionObject newAction = new ActionObject("254,100,1", 50);
				upActionAdapter.add(newAction);
				
			}
		});
		
		return addUpActionButton;
	}
	
	private AlertDialog editCommandDialog(String command, String delay, final ActionAdapter adapter, final int index){
		AlertDialog.Builder aDialogBuilder = new AlertDialog.Builder(this);
		LayoutInflater inflater = this.getLayoutInflater();
		final View v = inflater.inflate(R.layout.edit_command_alert_dialog, null);
		
		aDialogBuilder.setView(v);
		
		final EditText commandEdit = (EditText)((ViewGroup)v).getChildAt(1);
		final EditText delayEdit = (EditText)((ViewGroup)v).getChildAt(3);
				
		commandEdit.setText(command);
		delayEdit.setText(delay);
		
		aDialogBuilder.setTitle("Edit Command");
		aDialogBuilder.setMessage("Delimit command bytes by ,");
		
		aDialogBuilder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				System.out.println("Save new Command");
				
				String newCommand = commandEdit.getText().toString();
				
				
				int newDelay = Integer.parseInt(delayEdit.getText().toString());
				
				adapter.getItem(index).setActionCommand(newCommand);
				adapter.getItem(index).setActionDelay(newDelay);
				
			}
		}); 
		aDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				System.out.println("Cancel editing command");
				
			}
		});
		return aDialogBuilder.create();
	}
	
	private AlertDialog removeCommandDialog(final ActionAdapter adapter, final int index){
		AlertDialog.Builder aBuilder = new AlertDialog.Builder(this);
		aBuilder.setTitle("Remove Action?");
		aBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				adapter.remove(adapter.getItem(index));
				
			}
		});
		aBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				
				
			}
		});
		return aBuilder.create();
		
	}
	
	private void getStoredSettings(){
		String[] storedSettings = cPanel.getStoredString(deviceMacAddress+macroID).split("~");
		macroName = storedSettings[0];
		if(storedSettings.length == 1){
			return;
		}
		String[] actions = storedSettings[1].split(";");

		if(!storedSettings[1].startsWith(";")){
			String[] downCommands = actions[0].split("-");
			for(String downCommand : downCommands){
				String[] command = downCommand.split("#");
				String commandAction = command[0];
				int commandDelay = 0;
				if(!(command.length == 1 ||command[1] == null)){
					commandDelay = Integer.parseInt(command[1]);
				}
				ActionObject action = new ActionObject(commandAction, commandDelay);
				downActionList.add(action);
			}
			if(actions.length>1){
				String[] upCommands = actions[1].split("-");
				for(String upCommand : upCommands){
					String[] command = upCommand.split("#");
					String commandAction = command[0];
					int commandDelay = 0;
					if(!(command.length == 1 ||command[1] == null)){
						commandDelay = Integer.parseInt(command[1]);
					}
					ActionObject action = new ActionObject(commandAction, commandDelay);
					upActionList.add(action);
				}
			}


		}else{
			if(actions.length != 0){
				if(actions[0].contains("-")){
					String[] upCommands = actions[0].split("-");
					for(String upCommand : upCommands){
						String[] command = upCommand.split("#");
						String commandAction = command[0];
						int commandDelay = 0;
						if(!(command.length == 1 || command[1] == null)){
							commandDelay = Integer.parseInt(command[1]);
						}
						
						ActionObject action = new ActionObject(commandAction, commandDelay);
						upActionList.add(action);
					}
				}
			}
			
			
		}



		
	}	
	
	
	private void saveSettings(){
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append(macroName+"~");
		
		for(ActionObject a : downActionList){
			sBuilder.append(a.getActionCommand()+"#");
			sBuilder.append(a.getActionDelay()+"-");
		}
		sBuilder.append(";");
		for(ActionObject a : upActionList){
			sBuilder.append(a.getActionCommand()+"#");
			sBuilder.append(a.getActionDelay()+"-");
		}
		System.out.println("Saving String: "+sBuilder.toString());
		cPanel.saveString(deviceMacAddress+macroID, sBuilder.toString());
		Intent macroSetupIntent = new Intent(getApplicationContext(), MacroSetupActivity.class);
		macroSetupIntent.putExtra("MAC", deviceMacAddress);
		startActivity(macroSetupIntent);
		finish();
		
	}
	

	@Override
	protected void onPause() {
		super.onPause();
		saveSettings();
	}
	
}
