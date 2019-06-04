package com.example.wnsvy.kakaonotiread.Common;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

import io.realm.Realm;

import static android.speech.tts.TextToSpeech.ERROR;

public class CommonApplication extends Application {

    private static volatile CommonApplication appInstance = null;
    private static TextToSpeech textToSpeech;

    @Override
    public void onCreate() {
        super.onCreate();

        Realm.init(this); // Realm 초기화
        appInstance = this;
        CommonApplication.textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) { // 초기화
                if (status != ERROR) {
                    SharedPreferences sharedPreferences = getSharedPreferences("tts",MODE_PRIVATE );
                    AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    if(sharedPreferences != null) {
                        int ttsSpeechRateValue = sharedPreferences.getInt("ttsSpeechRate", 0);
                        int ttsToneValue = sharedPreferences.getInt("ttsTone", 0);
                        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                        String defaultEngine = sharedPreferences.getString("ttsEngine", "");
                        textToSpeech.setLanguage(Locale.KOREAN); // 언어
                        textToSpeech.setPitch(ttsToneValue * 0.1f);
                        textToSpeech.setSpeechRate(ttsSpeechRateValue * 0.1f);
                        textToSpeech.setEngineByPackageName(defaultEngine);
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
                        Log.d("초기값 : ", "톤 : " + ttsToneValue + "속도 : " + ttsSpeechRateValue + "엔지 : " + defaultEngine + "볼륨 : " + currentVolume);
                    }
                }
            }
        });
    }

    public static CommonApplication getInstance() {
        if (appInstance == null) {
            throw new IllegalStateException("this application does not inherit ApplicationContext");
        }
        return appInstance;
    }

    public TextToSpeech getTextToSpeech() {
        return textToSpeech;
    }

    public void setTextToSpeechRate(Float speechRate) {
        textToSpeech.setSpeechRate(speechRate); // 읽는 속도
    }

    public void setTextToSpeechPitch(Float pitch) {
        textToSpeech.setPitch(pitch); // 음성톤
    }

    public void setTextToSpeechLocale(Locale locale) {
        textToSpeech.setLanguage(locale); // 언어
    }

}
