package com.example.wnsvy.kakaonotiread.Common;

import android.app.Application;
import android.speech.tts.TextToSpeech;
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
                    textToSpeech.setLanguage(Locale.KOREAN); // 언어
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
