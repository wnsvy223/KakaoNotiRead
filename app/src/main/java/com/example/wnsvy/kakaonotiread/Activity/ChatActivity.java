package com.example.wnsvy.kakaonotiread.Activity;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.example.wnsvy.kakaonotiread.Adapter.ChatAdapter;
import com.example.wnsvy.kakaonotiread.Adapter.MessageAdapter;
import com.example.wnsvy.kakaonotiread.Common.CommonApplication;
import com.example.wnsvy.kakaonotiread.Model.Users;
import com.example.wnsvy.kakaonotiread.R;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class ChatActivity extends AppCompatActivity {

    private Realm realm;
    public RecyclerView recyclerView;
    public TextToSpeech textToSpeech;
    public AudioManager audioManager;
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerView = findViewById(R.id.recyclerView);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
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

        Intent intent = getIntent();
        String id = intent.getStringExtra("id");
        realm = Realm.getDefaultInstance();
        RealmResults<Users> realmResults = realm.where(Users.class).equalTo("userId",id).sort("timeStamp",Sort.ASCENDING).findAll();
        //인탠트로 넘어온 유저의 메시지 전체 조회(동기방식으로 받아와서 사이즈값을 UI스레드에서 받아 스크롤 이동 메소드 실행되도록 함)

        ChatAdapter chatAdapter = new ChatAdapter(realmResults,true, this, textToSpeech);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(chatAdapter);
        recyclerView.scrollToPosition(realmResults.size() - 1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
    }

    // RealmResults<Users> to List<Model> 변환 메소드
    public List<Users> getModelLIst(){
        List<Users> list = new ArrayList<>();
        try {
            realm = Realm.getDefaultInstance();
            RealmResults<Users> results = realm.where(Users.class).sort("timeStamp",Sort.DESCENDING).findAllAsync();
            list.addAll(realm.copyFromRealm(results));
        } finally {
            if (realm != null) {
                realm.close();
            }
        }
        return list;
    }

}
