package com.example.wnsvy.kakaonotiread.Service;

import android.media.AudioManager;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.TextUtils;
import android.util.Log;

import com.example.wnsvy.kakaonotiread.Common.CommonApplication;

import static android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME;

public class KakaoPushListenerService extends NotificationListenerService {

    public static final String TAG = "카톡푸시";
    private TextToSpeech textToSpeech;
    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener
            = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange){
                case AudioManager.AUDIOFOCUS_GAIN:
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                            AudioManager.FLAG_PLAY_SOUND);
                    break;
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                    //재생중인 미디어 볼륨 처리
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    //재생중인 미디어 일시정지 처리
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    //재생중인 미디어 일시정지 처리
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    //재생중인 미디어 뮤트 처리 또는 최소화
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,0,AudioManager.FLAG_PLAY_SOUND);
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        textToSpeech = CommonApplication.getInstance().getTextToSpeech();
        audioManager = (AudioManager)getSystemService(getApplicationContext().AUDIO_SERVICE);

        // TTS 읽기 기능 미디어플레이어 재생시 중지 및 재시작 처리
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                audioManager.requestAudioFocus(audioFocusChangeListener,
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            }

            @Override
            public void onDone(String utteranceId) {
                audioManager.abandonAudioFocus(audioFocusChangeListener);
            }

            @Override
            public void onError(String utteranceId) {

            }
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(textToSpeech != null){
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn, RankingMap rankingMap) {
        super.onNotificationPosted(sbn, rankingMap);

        final String packageName = sbn.getPackageName();
        if (!TextUtils.isEmpty(packageName) && packageName.equals("com.kakao.talk")) {
            Log.d(TAG, String.valueOf(sbn.getNotification().extras));
            Bundle kakaoPushData = sbn.getNotification().extras; // 노티로 넘어오는 푸시메시지 Bundle 데이터

            String title =  kakaoPushData.getString("android.title"); // 카톡푸시메시지 방의 제목
            String text = kakaoPushData.getString("android.text"); // 카톡푸시메시지 내용

            if(!TextUtils.isEmpty(title) && !TextUtils.isEmpty(text)) {
                Bundle ttsParam = new Bundle();
                ttsParam.putFloat(KEY_PARAM_VOLUME, 2.0f);

                textToSpeech.speak(title + "님으로부터 온 메시지는" + text + "입니다", TextToSpeech.QUEUE_FLUSH, ttsParam, "1");
                //tts.speak(title + "님으로부터 온 메시지는" + text + "입니다", TextToSpeech.QUEUE_FLUSH, ttsParam, "1");
            }
            // 푸시 메시지 읽을때 음악, 동영상 재생 중지 기능 추가

        }
    }
}

