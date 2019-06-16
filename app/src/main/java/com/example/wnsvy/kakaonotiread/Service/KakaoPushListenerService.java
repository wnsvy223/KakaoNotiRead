package com.example.wnsvy.kakaonotiread.Service;

import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.Log;

import com.example.wnsvy.kakaonotiread.Common.CommonApplication;
import com.example.wnsvy.kakaonotiread.Model.Users;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import io.realm.Realm;

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
                            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC),
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
    private Realm realm;
    private SharedPreferences sharedPreferences;


    @Override
    public void onCreate() {
        super.onCreate();

        sharedPreferences = getSharedPreferences("tts", MODE_PRIVATE);
        sharedPreferences.edit().putBoolean("NLSRunning", true).apply(); // 노티 서비스 시작 설정값

        textToSpeech = CommonApplication.getInstance().getTextToSpeech();
        audioManager = (AudioManager)getSystemService(getApplicationContext().AUDIO_SERVICE);

        // TTS 읽기 기능 미디어플레이어 재생시 중지 및 재시작 처리
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                /*
                audioManager.requestAudioFocus(new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT) // 8.0 이후
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build())
                        .setAcceptsDelayedFocusGain(true)
                        .setWillPauseWhenDucked(true)
                        .setOnAudioFocusChangeListener(audioFocusChangeListener)
                        .build());
               */
                audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);  // 8.0 이전
            }

            @Override
            public void onDone(String utteranceId) {
                /*
                audioManager.requestAudioFocus(new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)   // 8.0 이후
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build())
                        .setAcceptsDelayedFocusGain(true)
                        .setWillPauseWhenDucked(true)
                        .setOnAudioFocusChangeListener(audioFocusChangeListener)
                        .build());
                */
                audioManager.abandonAudioFocus(audioFocusChangeListener); // 8.0 이전
            }

            @Override
            public void onError(String utteranceId) {

            }
        });
        realm = Realm.getDefaultInstance();
    }



    @Override
    public void onDestroy() {
        super.onDestroy();

        if(textToSpeech != null){
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
        realm.close();
        sharedPreferences.edit().putBoolean("NLSRunning", false).apply(); // 노티 서비스 중지 설정값
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn, RankingMap rankingMap) {
        super.onNotificationPosted(sbn, rankingMap);

        final String packageName = sbn.getPackageName();
        if (!TextUtils.isEmpty(packageName)) {
            switch (packageName){
                case "com.kakao.talk":
                    // 카카오톡 메시지
                    saveNotificationToRealm(sbn,"KakaoTalk");
                    break;
                case "com.samsung.android.messaging":
                    // 삼성 MMS 메시지
                    saveNotificationToRealm(sbn,"MMS");
                    break;

                default:
            }
        }
    }

    private void saveNotificationToRealm(StatusBarNotification sbn, final String type){
        Bundle kakaoPushData = sbn.getNotification().extras; // 노티로 넘어오는 푸시메시지 Bundle 데이터
        boolean isMute = sharedPreferences.getBoolean("ttsMuteState", false);
        final String title = kakaoPushData.getString("android.title"); // 카톡푸시메시지 방의 제목
        final String text = kakaoPushData.getString("android.text"); // 카톡푸시메시지 내용
        final String room = kakaoPushData.getString("android.subText"); // 카톡푸시 그룹채팅 방의 제목
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS", Locale.KOREA);
        final String timeStamp = simpleDateFormat.format(calendar.getTime()); // 타임 스탬프

        if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(text)) {
            Log.d(TAG, String.valueOf(kakaoPushData));
            Bundle ttsParam = new Bundle();
            ttsParam.putFloat(KEY_PARAM_VOLUME, 2.0f);
            if(!isMute) {
                textToSpeech.speak(title + "님으로부터 온 메시지는" + text + "입니다", TextToSpeech.QUEUE_FLUSH, ttsParam, "1");
            }

            realm.executeTransaction(new Realm.Transaction() { // 푸시 메시지 날아오면 Realm DB에 값 넣음.
                @Override
                public void execute(Realm realm) {
                    Users users = realm.createObject(Users.class);
                    users.setType(type);
                    users.setUserId(title);
                    users.setMessage(text);
                    users.setTimeStamp(timeStamp);
                    if(room == null){
                        users.setRoom(title);
                    }else{
                        users.setRoom(room);
                    }
                    users.setRead(false);
                }
            });
        }
    }
}

