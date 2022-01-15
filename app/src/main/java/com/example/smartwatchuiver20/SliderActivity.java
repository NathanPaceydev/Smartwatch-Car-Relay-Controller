package com.example.smartwatchuiver20;

import static java.lang.Math.abs;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

public class SliderActivity extends Activity {
    final int MAXTIME = 5000;

    private ImageView menuLogo;
    private SeekBar slider;
    private Button backButton;
    private Button speechButton;
    private TextView statusText;
    private float prog;
    private float progPrior;
    BluetoothInterface btHandler = new BluetoothInterface();

    int relayNumber;

    public static final String KEY_SEEKBARLOCATION = "seekBarLocation";
    public static final String KEY_PROG = "progress";
    public static final String KEY_PRORPRIOR = "priorProgress";

    public static final String KEY_DIRECTION = "savedDirection";
    public String SHARED_PREFS = "sharedPrefs";

    private static final int SPEECH_RECOGNIZER_REQUEST_CODE = 0;

    private int sliderProgress;
    private boolean direction;

    private int numberTicks;

    public int finalRelayNumber;
    private Locale currentLocal = null;




    //Resources res = getResources();
    //Drawable shape = ResourcesCompat.getDrawable(res, R.drawable.custom_thumb, getTheme());
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // overridePendingTransition(R.anim.fade, R.anim.fadeout);
        setContentView(R.layout.slider_act);

        currentLocal = Locale.getDefault();


        Intent intent = getIntent();
        int relayNumber = 0;
        int logoID = intent.getIntExtra("HeaderIMG", R.drawable.admin_pass_activity);
        String preferenceName = intent.getStringExtra("SHAREDPREFNAME");
        SHARED_PREFS = preferenceName;

        Drawable thumb = getDrawable(R.drawable.custom_thumb);
        Drawable thumb1 = getDrawable(R.drawable.custom_thumb1);
        Drawable logo = getDrawable(logoID);


        backButton = findViewById(R.id.backButton);
        speechButton = findViewById(R.id.textToSpeechButton);

        statusText = findViewById(R.id.statusText);
        slider = findViewById(R.id.slider);
        menuLogo = findViewById(R.id.headerLogo);
        menuLogo.setImageDrawable(logo);
        slider.setThumb(thumb1);

        if (logoID == R.drawable.liftgate_button){
            numberTicks = 1;
            slider.setMax(numberTicks);
            relayNumber = 0;
        }
        else if (logoID == R.drawable.doors_button){
            numberTicks = 4;
            relayNumber = 1;
        }
        else if (logoID == R.drawable.hood_button){
            numberTicks = 1;
            slider.setMax(numberTicks);
            relayNumber = 2;
        }

        loadData();
        updateViews();

        if(direction){
            slider.getProgressDrawable().setColorFilter(getResources().getColor(R.color.magnaBlue), android.graphics.PorterDuff.Mode.SRC_IN);
            slider.setThumb(thumb1);
        }
        else{
            slider.getProgressDrawable().setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_IN);
            slider.setThumb(thumb);
        }


        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveData();
                finish();
            }
        });

        //speech onClick listener
        speechButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSpeech();

            }
        });


        finalRelayNumber = relayNumber;
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int lastprogress = (int)progPrior;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                prog = progress;
                if(direction) {
                    if (progress <= lastprogress) {
                        seekBar.setProgress(lastprogress);
                    } else {
                        lastprogress = progress;
                    }
                }
                else{
                    if (progress >= lastprogress) {
                        seekBar.setProgress(lastprogress);
                    } else {
                        lastprogress = progress;
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(prog != progPrior){
                    direction = !direction;
                    statusText.setText(getString(R.string.moving));
                    btHandler.TurnOnRelayFusion(finalRelayNumber,1);
                    btHandler.TurnOffRelayFusion(finalRelayNumber,1);
                }
                backButton.setEnabled(false);
                seekBar.setEnabled(false);
                statusText.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        statusText.setText(getString(R.string.moving));
                        seekBar.getProgressDrawable().setColorFilter(Color.BLACK, android.graphics.PorterDuff.Mode.SRC_IN);
                        if(prog != progPrior) {
                            btHandler.TurnOnRelayFusion(finalRelayNumber, 1);
                            btHandler.TurnOffRelayFusion(finalRelayNumber, 1);
                        }
                        progPrior = prog;
                        statusText.setText(getString(R.string.ready));
                        seekBar.setEnabled(true);
                        backButton.setEnabled(true);
                        if(direction){
                            seekBar.setThumb(thumb1);
                            seekBar.getProgressDrawable().setColorFilter(getResources().getColor(R.color.magnaBlue), android.graphics.PorterDuff.Mode.SRC_IN);
                        }
                        else{
                            seekBar.setThumb(thumb);
                            seekBar.getProgressDrawable().setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_IN);
                        }
                    }
                }, (long) (MAXTIME*((abs(prog-progPrior)/slider.getMax()))));
            }
        });
    }

    // method to show Toast messages on the main UI thread instead of locally
    public void showToast(final String message) {
        final Handler mHandler = new Handler(); // set Handler

        new Thread(new Runnable() { // add new thread
            @Override
            public void run () {
                // Perform long-running task here
                // (like audio buffering).
                // You may want to update a progress
                // bar every second, so use a handler:

                mHandler.post(new Runnable() {
                    @Override
                    public void run () {
                        // make the Toast
                        Toast newToast = Toast.makeText(SliderActivity.this, message.toString(),Toast.LENGTH_LONG);
                        newToast.show();
                    }
                });
            }
        }).start();
    }

    // speech recognition code
    private void startSpeech() {

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        //intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de_DE"); // extra lang option
        // ** set the voice control to the default Language **
        //TODO change the speech to text language to the selected language

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLocal.getDisplayLanguage());

        System.out.println("Default Language: "+currentLocal.toString()); //print the language

        try{
            startActivityForResult(intent, SPEECH_RECOGNIZER_REQUEST_CODE);

        }catch (ActivityNotFoundException ignored) {
        }

    }

    @Override
    // speech activity result
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode, data);


        if (requestCode == SPEECH_RECOGNIZER_REQUEST_CODE){
            if(resultCode == RESULT_OK){
                List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String recognizeTxt = results.get(0);

                System.out.println("You Said: "+recognizeTxt);

                speechControl(recognizeTxt);

            }
        }
    }

    private void speechControl(String voiceText){

        // keywords to activate slider and relay
        String[] openKeywords = {"turn on", "open", "start", "forward"};
        String[] closeKeywords = {"close", "back", "backwards"};

        // if direction is forward and user says open keywords set slider to max
        // and call the relay control method
        if(direction) {
            if (voiceText.toLowerCase(Locale.ROOT).contains(openKeywords[0]) || voiceText.toLowerCase(Locale.ROOT).contains(openKeywords[1]) || voiceText.toLowerCase(Locale.ROOT).contains(openKeywords[2]) || voiceText.toLowerCase(Locale.ROOT).contains(openKeywords[3])){
                slider.setProgress(numberTicks);
                relayControl();
            }
            else{
                showToast("Say \"open\" to open"); // call toast method
            }
        }

        // else, direction backwards and user user says close keywords call relay control method
        else {
            if (voiceText.toLowerCase(Locale.ROOT).contains(closeKeywords[0]) || voiceText.toLowerCase(Locale.ROOT).contains(closeKeywords[1]) || voiceText.toLowerCase(Locale.ROOT).contains(closeKeywords[2])){
                slider.setProgress(0);
                relayControl();
            }
            else{
                showToast("Say \"close\" to close"); // call toast method
            }
        }

    }


    private void relayControl(){
        Drawable thumb = getDrawable(R.drawable.custom_thumb);
        Drawable thumb1 = getDrawable(R.drawable.custom_thumb1);

        direction = !direction;
        statusText.setText(getString(R.string.moving));
        btHandler.TurnOnRelayFusion(finalRelayNumber,1);
        btHandler.TurnOffRelayFusion(finalRelayNumber,1);


            /*
            if (direction) {

               // statusText.setText(getString(R.string.moving));
                //slider.setThumb(thumb1);
                //slider.getProgressDrawable().setColorFilter(getResources().getColor(R.color.magnaBlue), android.graphics.PorterDuff.Mode.SRC_IN);


                //btHandler.TurnOnRelayFusion(finalRelayNumber,1);
                //btHandler.TurnOffRelayFusion(finalRelayNumber,1);

                slider.setProgress(numberTicks);
                direction = !direction;
               // statusText.setText(getString(R.string.ready));

            } else {
                //statusText.setText(getString(R.string.moving));

                //slider.setThumb(thumb);
                //slider.getProgressDrawable().setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_IN);

                slider.setProgress(0);
                direction = !direction;
                //statusText.setText(getString(R.string.ready));
            }

             */

        backButton.setEnabled(false);
        slider.setEnabled(false);

        statusText.postDelayed(new Runnable() {
            @Override
            public void run() {
                statusText.setText(getString(R.string.moving));
                slider.getProgressDrawable().setColorFilter(Color.BLACK, android.graphics.PorterDuff.Mode.SRC_IN);

                if(prog != progPrior) {
                    btHandler.TurnOnRelayFusion(finalRelayNumber, 1);
                    btHandler.TurnOffRelayFusion(finalRelayNumber, 1);
                }

                progPrior = prog;
                statusText.setText(getString(R.string.ready));
                slider.setEnabled(true);
                backButton.setEnabled(true);
                if(direction){
                    slider.setThumb(thumb1);
                    slider.getProgressDrawable().setColorFilter(getResources().getColor(R.color.magnaBlue), android.graphics.PorterDuff.Mode.SRC_IN);
                }
                else{
                    slider.setThumb(thumb);
                    slider.getProgressDrawable().setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_IN);
                }
            }
        }, (long) (MAXTIME*((abs(prog-progPrior)/slider.getMax()))));
    }



    public void saveData(){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putInt(KEY_SEEKBARLOCATION, slider.getProgress());
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
        slider.setProgress(sliderProgress);
    }

    public void runSlider(String progress) throws InterruptedException {
    }


    @Override
    protected void onResume() {
        if (currentLocal.getLanguage() != Locale.getDefault().getLanguage()) {
            currentLocal = Locale.getDefault();
            recreate();
        }
        currentLocal = getResources().getConfiguration().locale;
        System.out.println(currentLocal);
        super.onResume();
    }

}