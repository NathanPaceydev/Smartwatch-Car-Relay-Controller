package com.controlanything.NCDTCPRelay;
import static java.lang.Math.abs;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;


//TODO add functional code lol
public class SliderActivity extends Activity{

    // declare attributes
    String deviceMacAddressSlider; // string for Mac address
    private TextView relayNameTextView, movingTextView;
    private SeekBar seekBar;

    final int MAXTIME = 5000; // global time var
    private boolean direction = true;
    private float prog = 0;
    private float progPrior = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get the intents
        Intent intent = getIntent();
        String relayName = intent.getStringExtra("RelayName");
        deviceMacAddressSlider = intent.getStringExtra("MAC");

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

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int lastprogress = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                prog = progress;
                if(direction) {
                    //seekBar.setThumb(thumb1);
                    if (progress < lastprogress) {
                        seekBar.setProgress(lastprogress);
                    } else {
                        lastprogress = progress;
                        //direction = !direction;
                    }
                }
                else{
                    //seekBar.setThumb(thumb);
                    if (progress > lastprogress) {
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
                }
                //   textView2.setText("" + 0);
            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
                if(prog != progPrior){
                    direction = !direction;
                    String movingText = "Moving to " + (int)((abs(prog)/4*100)) + "%";
                    movingTextView.setText(movingText);
                    //seekBar.setThumb(thumb);
                }
                //textView3.setText("" + direction);
                //textView1.setText(""+ (int)(abs(prog-progPrior)/4*100) + "");
                seekBar.setEnabled(false);
                movingTextView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        movingTextView.setText("Ready");
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

        // on click listener for back button
        cancelButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                Intent relayControl = new Intent(getApplicationContext(), RelayControlActivity.class);
                startActivity(relayControl);
                finish();
                //DeviceListActivity.super.onBackPressed();//send back to home screen
            }
        });




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
