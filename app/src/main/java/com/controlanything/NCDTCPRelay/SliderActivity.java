package com.controlanything.NCDTCPRelay;
import static java.lang.Math.abs;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Arrays;


//TODO add functional code lol
public class SliderActivity extends Activity{

    final int MAXTIME = 5000; // global time var
    final int BANKNUMBER = 1; // weird number that makes relay work

    // declare attributes
    String deviceMacAddressSlider; // string for Mac address
    private TextView relayNameTextView, movingTextView;
    private SeekBar seekBar;

    // Strings for Key down saves
    private final String KEY_SEEKBARLOCATION = "seekBarLocation";
    private final String KEY_PROG = "progress";
    private final String KEY_PRORPRIOR = "priorProgress";
    private final String KEY_DIRECTION = "savedDirection";
    public String SHARED_PREFS = "sharedPrefs";

    // declare variables
    private int sliderProgress;
    private boolean direction;
    private float prog;
    private float progPrior;

    String[] relayNames;
    ControlPanel cPanel;
    private Vibrator myVib;
    int numberOfRelays;
    public int[] relayStatusArray;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cPanel = ((ControlPanel)getApplicationContext());

        relayStatusArray = new int [numberOfRelays];


        // get the intents
        Intent intent = getIntent();
        String relayName = intent.getStringExtra("RelayName");
        SHARED_PREFS = relayName;

        deviceMacAddressSlider = intent.getStringExtra("MAC"); //get the MAC address
        final int relayNumber = intent.getIntExtra("RelayNumber",-1);


        // print the intents
        System.out.println("** Slider Activity **"+"\nName: "+relayName+"\nMAC: "+deviceMacAddressSlider);

        // set the layout to slider_activity
        setContentView(R.layout.slider_activity);

        // get the relay name textview
        relayNameTextView = (TextView) findViewById(R.id.relayName);

        movingTextView= (TextView) findViewById(R.id.movingText);

        relayNameTextView.setText(relayName);

        // set the custom slider resources
        final Drawable thumb = getResources().getDrawable(R.drawable.custom_thumb);
        final Drawable thumb1 = getResources().getDrawable(R.drawable.custom_thumb1);

        // get back button
        final ImageButton cancelButton = (ImageButton) findViewById(R.id.BackButton);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setThumb(thumb1);

        loadData();
        updateViews();

        if(direction){
            seekBar.getProgressDrawable().setColorFilter(getResources().getColor(R.color.magnaBlue), android.graphics.PorterDuff.Mode.SRC_IN);
            seekBar.setThumb(thumb1);
        }
        else{
            seekBar.getProgressDrawable().setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_IN);
            seekBar.setThumb(thumb);
        }


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int lastprogress = (int)progPrior;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                prog = progress;
                if(direction) {
                    //seekBar.setThumb(thumb1);
                    if (progress <= lastprogress) {
                        seekBar.setProgress(lastprogress);
                    } else {
                        lastprogress = progress;

                        //direction = !direction;
                    }
                }
                else{
                    //seekBar.setThumb(thumb);
                    if (progress >= lastprogress) {
                        seekBar.setProgress(lastprogress);
                    } else {
                        lastprogress = progress;
                        //direction = !direction;
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if(prog != progPrior){
                    movingTextView.setText("Started");

                    //clickRelay(relayNumber,BANKNUMBER);
                }
                //   textView2.setText("" + 0);
            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
                if(prog != progPrior){
                    direction = !direction;
                    String movingText = "Moving to " + (int)((abs(prog)/4*100)) + "%";
                    movingTextView.setText(movingText);

                    // TODO make this  work dawg
                    clickRelay(relayNumber,BANKNUMBER);
                    //seekBar.setThumb(thumb);
                }
                //textView3.setText("" + direction);
                //textView1.setText(""+ (int)(abs(prog-progPrior)/4*100) + "");
                seekBar.setEnabled(false);

                movingTextView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        movingTextView.setText("Ready");

                        //clickRelay(relayNumber,BANKNUMBER);//TODO make work

                        // textView2.setText("" + (long) (MAXTIME*((abs(prog-progPrior)/4))));
                        seekBar.getProgressDrawable().setColorFilter(Color.BLACK, android.graphics.PorterDuff.Mode.SRC_IN);
                        progPrior = prog;
                        seekBar.setEnabled(true);
                        if(direction){
                            seekBar.setThumb(thumb1);
                            seekBar.getProgressDrawable().setColorFilter(getResources().getColor(R.color.magnaBlue), android.graphics.PorterDuff.Mode.SRC_IN);

                        }
                        else{
                            seekBar.setThumb(thumb);
                            seekBar.getProgressDrawable().setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_IN);
                        }
                    }
                }, (long) (MAXTIME*((abs(prog-progPrior)/4))));

                //progPrior = prog;
                //   runit((int)(prog/4));
            }
        });

        System.out.println(prog +", " + progPrior + ", " + direction + ", " + seekBar.getProgress());


        // on click listener for back button
        cancelButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                Intent relayControl = new Intent(getApplicationContext(), RelayControlActivity.class);
                startActivity(relayControl);
                saveData();
                finish();
                //DeviceListActivity.super.onBackPressed();//send back to home screen
            }
        });


    }



    // TODO
    // fake button click method
    // method called for speech recogintion -> assumes click relay
    // updated to momentary
    public void clickRelay(final int relayNumber, final int bankNumber) {

        System.out.println("*** ClICKED ***"); // test print

        System.out.println("***"+relayNumber+"***"); // test print


        int[] returnedStatus = (cPanel.TurnOnRelayFusion((relayNumber + ((bankNumber-1)*8)), bankNumber));
        if(returnedStatus[0] != 260){
            //myVib.vibrate(50);
            updateButtonTextFusion(bankNumber, returnedStatus);
        }

        int[] returnedStatus2 = (cPanel.TurnOffRelayFusion((relayNumber - ((bankNumber-1)*8)), bankNumber));

        if(returnedStatus2[0] != 260){
            //myVib.vibrate(50);
            updateButtonTextFusion(bankNumber, returnedStatus);
        }

    }



// TODO GOD Help pls
    // might not be needed
    private void updateButtonTextFusion(int bankNumber, int[] status) {

        if(status != null){

            if(numberOfRelays < 8){

                for(int i = 0; i < numberOfRelays; i++){
                    relayStatusArray[i+((bankNumber-1)*8)] = status[i];
                }

                for (int i = 0; i < numberOfRelays; i++) {

                    if (relayStatusArray[i+((bankNumber-1)*8)] != 0){
                        //relayButtons[i + ((bankNumber-1)*8)].setImageResource(R.drawable.blue_button_no_glow);

                    }
                    else {
                        //relayButtons[i + ((bankNumber-1)*8)].setImageResource(R.drawable.button_dead);
                    }
                }

            }else{

                for(int i = 0; i < 8; i++){
                    relayStatusArray[i+((bankNumber-1)*8)] = status[i];
                }

                for (int i = 0; i < 8; i++) {

                    if (relayStatusArray[i+((bankNumber-1)*8)] != 0){
                        //relayButtons[i + ((bankNumber-1)*8)].setImageResource(R.drawable.blue_button_no_glow);

                    }
                    else {
                        //relayButtons[i + ((bankNumber-1)*8)].setImageResource(R.drawable.button_dead);
                    }
                }

            }

            System.out.println("Current relay's status "+Arrays.toString(relayStatusArray));
        }else{
            //changeTitleToRed();
        }
    }

    public void changeTitleToRed(){
        System.out.println("Connection Lost");
    }





        public void saveData(){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        System.out.println("SAVED " + prog +", " + progPrior + ", " + direction + ", " + seekBar.getProgress());

        editor.putInt(KEY_SEEKBARLOCATION, seekBar.getProgress());
        editor.putFloat(KEY_PROG, prog);
        editor.putFloat(KEY_PRORPRIOR, progPrior);
        editor.putBoolean(KEY_DIRECTION, direction);
        editor.apply();
    }

    public void loadData(){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        sliderProgress = sharedPreferences.getInt(KEY_SEEKBARLOCATION, 0);
        prog = sharedPreferences.getFloat(KEY_PROG, 0);
        progPrior = sharedPreferences.getFloat(KEY_PRORPRIOR, 0);
        direction = sharedPreferences.getBoolean(KEY_DIRECTION,true);

    }

    public void updateViews(){
        seekBar.setProgress(sliderProgress);
    }



    public void runit(int mf){
        new java.util.Timer().schedule(
                new java.util.TimerTask(){
                    @Override
                    public void run(){
                        movingTextView.setText("Ended");
                    }
                },
                (long) (MAXTIME*((abs(prog-progPrior)/4)))
        );
    }




}
