package com.example.wnsvy.kakaonotiread.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;

import com.example.wnsvy.kakaonotiread.Adapter.ChatAdapter;
import com.example.wnsvy.kakaonotiread.Adapter.MessageAdapter;
import com.example.wnsvy.kakaonotiread.Common.CommonApplication;
import com.example.wnsvy.kakaonotiread.Model.Users;
import com.example.wnsvy.kakaonotiread.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME;

public class ChatActivity extends AppCompatActivity {

    private Realm realm;
    public RecyclerView recyclerView;
    private TextToSpeech textToSpeech;
    private AudioManager audioManager;
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
    public LinearLayoutManager linearLayoutManager;
    public RealmResults<Users> realmResults;
    public RealmResults<Users> allResults;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initView();

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) { // 초기화
                if (status == TextToSpeech.SUCCESS) {
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
                    }
                }
            }
        });

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
        String room = intent.getStringExtra("room");
        realm = Realm.getDefaultInstance();
        realmResults = realm.where(Users.class).equalTo("room",room).sort("timeStamp",Sort.DESCENDING).limit(10).findAll();
        //인탠트로 넘어온 유저의 메시지 전체 조회(동기방식으로 받아와서 사이즈값을 UI스레드에서 받아 스크롤 이동 메소드 실행되도록 함)
        allResults = realm.where(Users.class).equalTo("room",room).sort("timeStamp",Sort.DESCENDING).findAll();

        ChatAdapter chatAdapter = new ChatAdapter(realmResults,true, this, textToSpeech, realm);
        linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(chatAdapter);
        recyclerView.scrollToPosition(0);

        // 채팅 메시지 스크롤 리스너( 메시지 추가 로딩 )
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                int totalItemCount = linearLayoutManager.getItemCount(); // 전체 아이템 갯수 (화면에 로딩된 전체 데이터)
                if(!recyclerView.canScrollVertically(-1)){ // 스크롤이 뷰 최상단에 도착하면
                    loadMore(totalItemCount); // 메시지 로딩
                }
            }
        });
    }


    public void initView(){
        recyclerView = findViewById(R.id.recyclerView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
        //textToSpeech.shutdown();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(textToSpeech != null){
            //textToSpeech.stop();
        }
    }


    public void loadMore(int position){
        if(position == allResults.size()){
            Toast.makeText(getApplicationContext(),"모든 메시지가 로딩 되었습니다.",Toast.LENGTH_SHORT).show();
        }else {
            Intent intent = getIntent();
            String room = intent.getStringExtra("room");
            realmResults = realm.where(Users.class).equalTo("room", room).sort("timeStamp", Sort.DESCENDING).limit(position + 20).findAll();
            ChatAdapter chatAdapter = new ChatAdapter(realmResults, true, this, textToSpeech, realm);
            recyclerView.setAdapter(chatAdapter);
            recyclerView.scrollToPosition(realmResults.size() - 5);
        }
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
