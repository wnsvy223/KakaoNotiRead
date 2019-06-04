package com.example.wnsvy.kakaonotiread;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;

import com.example.wnsvy.kakaonotiread.Common.CommonApplication;

import java.util.ArrayList;
import java.util.List;

import static android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME;

public class MainActivity extends AppCompatActivity {


    public SeekBar ttsSpeechRate;
    public SeekBar ttsVolume;
    public SeekBar ttsTone;
    public Spinner spinner;
    private TextToSpeech textToSpeech;
    public SharedPreferences sharedPreferences;
    public AudioManager audioManager;
    public FloatingActionButton floatingActionButton;
    public int vol = 0;
    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener
            = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange){
                case AudioManager.AUDIOFOCUS_GAIN: //오디오 포커스 영구 획득
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                            AudioManager.FLAG_PLAY_SOUND);
                    break;
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT: // 오디오 포커스 일시 획득(15초 내외)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,0,AudioManager.FLAG_PLAY_SOUND);
                    //재생중인 미디어 볼륨 처리
                    break;
                case AudioManager.AUDIOFOCUS_LOSS: // 오디오 포커스 영구 손실
                    //재생중인 미디어 일시정지 처리
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT: // 오디오 포커스 일시 손실(15초 내외)
                    //재생중인 미디어 일시정지 처리
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: // 오디오 포커스 일시 손실 (45초 내외)
                    //재생중인 미디어 뮤트 처리 또는 최소화
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,0,AudioManager.FLAG_PLAY_SOUND);
                    break;
            }
        }
    };
    public  List<TextToSpeech.EngineInfo> engineInfoList;
    public List<String > listInstalledEnginesPackage;
    public List<String> listInstalledEnginesName;
    public int ttsEngineNum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initView();

        textToSpeech = CommonApplication.getInstance().getTextToSpeech();

        // TTS 읽기 기능 미디어플레이어 재생시 중지 및 재시작 처리
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                audioManager.requestAudioFocus(audioFocusChangeListener,
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                // TTS 읽기 시작하면 audioFocusChangeListener 의 AUDIOFOCUS_GAIN_TRANSIENT 조건 수행으로 미디어 볼륨 0
            }

            @Override
            public void onDone(String utteranceId) {
                audioManager.abandonAudioFocus(audioFocusChangeListener);
                // TTS 읽기 끝나면 audioFocusChangeListener 제거하여 다시 미디어 재생
            }

            @Override
            public void onError(String utteranceId) {

            }
        });

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "읽기 서비스 시작", Snackbar.LENGTH_LONG).setAction("", null).show();
                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            }
        });

        sharedPreferences = getSharedPreferences("tts",MODE_PRIVATE );
        int ttsSpeechRateValue = sharedPreferences.getInt("ttsSpeechRate", 0);
        int ttsToneValue = sharedPreferences.getInt("ttsTone", 0);
        String defaultEngine = sharedPreferences.getString("ttsEngine","");
        ttsEngineNum = sharedPreferences.getInt("ttsEngineNum",0);

        if(ttsSpeechRateValue != 0 || ttsToneValue != 0 || sharedPreferences != null) {
            ttsSpeechRate.setProgress(ttsSpeechRateValue);
            ttsTone.setProgress(ttsToneValue);
            textToSpeech.setPitch(ttsToneValue * 0.1f);
            textToSpeech.setSpeechRate(ttsSpeechRateValue * 0.1f);
            textToSpeech.setEngineByPackageName(defaultEngine);
        }
        setSeekBar(ttsSpeechRate);
        setSeekBar(ttsTone);
        setSeekBar(ttsVolume);
        setTTSEngine();
    }

    public void initView(){
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int curVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        ttsSpeechRate = findViewById(R.id.ttsSpeechRate);
        ttsVolume = findViewById(R.id.ttsVolume);
        ttsTone = findViewById(R.id.ttsTone);
        ttsSpeechRate.setMax(20);
        ttsVolume.setMax(maxVolume);
        ttsVolume.setProgress(curVolume);
        ttsTone.setMax(20);
        floatingActionButton = findViewById(R.id.fab);
        spinner = findViewById(R.id.spinner);
        setCollapsingToolbarLayout();
    }

    public void setTTSEngine(){
        engineInfoList = textToSpeech.getEngines(); // 설치된 TTS 엔진 목록 리스트
        listInstalledEnginesPackage = new ArrayList<>(); // TTS 엔진 패키지명 리스트
        listInstalledEnginesName = new ArrayList<>(); // TTS 엔진 이름 리스트
        for(int i=0; i<engineInfoList.size(); i++){
            listInstalledEnginesName.add(engineInfoList.get(i).label);
            listInstalledEnginesPackage.add(engineInfoList.get(i).name);
        }
        ArrayAdapter arrayAdapter = new ArrayAdapter(this,android.R.layout.simple_spinner_item , listInstalledEnginesName);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(arrayAdapter);
        spinner.setSelection(ttsEngineNum,false); // 리스너 이전에 setSelection()을 호출해야 이전에 선택한 값을 저장하여 스피너상태 유지됨.
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(spinner.getSelectedItemPosition() >= 0){
                    textToSpeech.setEngineByPackageName(listInstalledEnginesPackage.get(i)); // 선택한 패키지명의 TTS 엔진 적용
                    sharedPreferences.edit().putString("ttsEngine", listInstalledEnginesPackage.get(i)).apply();
                    sharedPreferences.edit().putInt("ttsEngineNum", i).apply();
                    // 설정값 유지를 위한 sharedPreferences 세팅
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    public float getConvertValue(int intVal){
        float floatVal = 0.0f;
        floatVal = 0.1f * intVal;
        return floatVal;
    }

    public void setSeekBar(SeekBar seekBar){

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) { // 사용자 요청에 의해 시크바 값이 변경될 경우
                    Bundle ttsParam = new Bundle();
                    ttsParam.putFloat(KEY_PARAM_VOLUME, 1.0f); // tts volume set 0 to 1 float 값

                    switch (seekBar.getId()) {
                        case R.id.ttsSpeechRate:
                            textToSpeech.setSpeechRate(getConvertValue(progress));
                            sharedPreferences.edit().putInt("ttsSpeechRate", progress).apply();
                            textToSpeech.speak("읽기 속도 값은" + progress + "입니다", TextToSpeech.QUEUE_FLUSH, ttsParam, "1");
                            Log.d("읽기속도", String.valueOf(getConvertValue(progress)));
                            break;
                        case R.id.ttsVolume:
                            textToSpeech.speak("읽기 볼륨 값은" + progress + "입니다", TextToSpeech.QUEUE_FLUSH, ttsParam, "1");
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                            vol = progress;
                            Log.d("읽기볼륨", String.valueOf(getConvertValue(progress)));
                            break;
                        case R.id.ttsTone:
                            textToSpeech.setPitch(getConvertValue(progress));
                            sharedPreferences.edit().putInt("ttsTone", progress).apply();
                            textToSpeech.speak("읽기 톤 값은" + progress + "입니다", TextToSpeech.QUEUE_FLUSH, ttsParam, "1");
                            Log.d("읽기톤", String.valueOf(getConvertValue(progress)));
                            break;
                        default:
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(textToSpeech != null){
           //textToSpeech.stop();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(textToSpeech != null){
            //textToSpeech.stop();
        }
    }

    private void setCollapsingToolbarLayout(){
        final CollapsingToolbarLayout collapsingToolbarLayout = findViewById(R.id.toolbar_layout);
        collapsingToolbarLayout.setTitle("설정");
        collapsingToolbarLayout.setExpandedTitleTextColor(ColorStateList.valueOf(Color.BLACK));
        final int myDrawable = R.drawable.kakao2;
        final ImageView imageView = findViewById(R.id.backImage);
        if(imageView != null){
            imageView.setImageResource(myDrawable);
        }
        AppBarLayout.OnOffsetChangedListener onOffsetChangedListener = new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if(collapsingToolbarLayout.getHeight() + verticalOffset < 2 * ViewCompat.getMinimumHeight(collapsingToolbarLayout)) {
                    // collapsed
                    imageView.animate().alpha(0.0f).setDuration(400);
                } else {
                    // extended
                    imageView.animate().alpha(1f).setDuration(400);    // 1.0f means opaque
                }
            }
        };

        final  AppBarLayout appBarLayout = findViewById(R.id.app_bar);
        appBarLayout.addOnOffsetChangedListener(onOffsetChangedListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
