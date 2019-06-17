package com.example.wnsvy.kakaonotiread.Activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.wnsvy.kakaonotiread.Adapter.ChatAdapter;
import com.example.wnsvy.kakaonotiread.Interface.RecyclerViewClickListener;
import com.example.wnsvy.kakaonotiread.Interface.RecyclerViewTouchListener;
import com.example.wnsvy.kakaonotiread.Model.Users;
import com.example.wnsvy.kakaonotiread.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

import static android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME;

public class ChatActivity extends AppCompatActivity{

    private Realm realm;
    public RecyclerView recyclerView;
    private TextToSpeech textToSpeech;
    private AudioManager audioManager;
    public LinearLayoutManager linearLayoutManager;
    public RealmResults<Users> realmResults;
    public RealmResults<Users> allResults;
    public ActionBar actionBar;
    private ChatAdapter chatAdapter;
    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener
            = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange){
                case AudioManager.AUDIOFOCUS_GAIN: //오디오 포커스 영구 획득
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC),
                            AudioManager.FLAG_PLAY_SOUND);
                    break;
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT: // 오디오 포커스 일시 획득(15초 내외)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC),
                            AudioManager.FLAG_PLAY_SOUND);
                    //재생중인 미디어 볼륨 처리
                    break;
                case AudioManager.AUDIOFOCUS_LOSS: // 오디오 포커스 영구 손실
                    //재생중인 미디어 일시정지 처리
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT: // 오디오 포커스 일시 손실(15초 내외)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,0,AudioManager.FLAG_PLAY_SOUND);
                    //재생중인 미디어 일시정지 처리
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: // 오디오 포커스 일시 손실 (45초 내외)
                    //재생중인 미디어 뮤트 처리 또는 최소화
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,0,AudioManager.FLAG_PLAY_SOUND);
                    break;
            }
        }
    };
    public ActionMode actionMode;
    public ActionMode.Callback actionModeCallback = new ActionMode.Callback(){

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater menuInflater = mode.getMenuInflater();
            menuInflater.inflate(R.menu.menu_chat, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int id = item.getItemId();
            switch (id){
                case R.id.action_read:
                    RealmResults<Users> results = realm.where(Users.class).equalTo("isSelected",true).sort("timeStamp",Sort.DESCENDING).findAll(); // 선택된 메시지 쿼리
                    speechSeletedMessage(results); // 선택된 메시지 읽기
                    return  true;

                case R.id.action_pause:
                    SharedPreferences sharedPreferences = getSharedPreferences("selectMode",MODE_PRIVATE );
                    boolean isSelectMode = sharedPreferences.getBoolean("isSelectMode",false);
                    if(isSelectMode) {
                        speechMessage(""); // 빈 메시지 읽게해서 오디오 포커싱이 바로 미디어로 이동하게 하여 멈춤효과
                    }
                    return  true;

                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
        }
    };


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


        Intent intent = getIntent();
        String room = intent.getStringExtra("room");
        actionBar.setTitle(room);
        realm = Realm.getDefaultInstance();
        realmResults = realm.where(Users.class).equalTo("room",room).sort("timeStamp",Sort.DESCENDING).limit(10).findAll();
        //인탠트로 넘어온 유저의 메시지 전체 조회(동기방식으로 받아와서 사이즈값을 UI스레드에서 받아 스크롤 이동 메소드 실행되도록 함)
        allResults = realm.where(Users.class).equalTo("room",room).sort("timeStamp",Sort.DESCENDING).findAll();

        SharedPreferences sharedPreferences = getSharedPreferences("selectMode",MODE_PRIVATE );
        boolean isSelectMode = sharedPreferences.getBoolean("isSelectMode",false);
        chatAdapter = new ChatAdapter(realmResults,true, this, textToSpeech);
        chatAdapter.setHasStableIds(true);
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

        recyclerView.addOnItemTouchListener(new RecyclerViewTouchListener(getApplicationContext(), recyclerView, new RecyclerViewClickListener() {
            @Override
            public void onClick(View view, int position) {

            }

            @Override
            public void onLongClick(View view, int position) {
                actionMode = startActionMode(actionModeCallback);
            }
        }));

        RealmResults<Users> unReadAllResults = realm.where(Users.class).equalTo("room",room).equalTo("isRead",false).sort("timeStamp",Sort.DESCENDING).findAll();
        showDialog(unReadAllResults); // 액티비티 진입시 읽지 않은 전체 메시지 읽을지 묻는 다이얼로그 호출
    }


    public void initView(){
        recyclerView = findViewById(R.id.recyclerView);
        actionBar = getSupportActionBar();
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
        isSelectedReset(); // 채팅방 화면을 나갈때 해당 방의 메시지들 선택값 모두 false로 리셋
    }

    public void isSelectedReset(){
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                for(int i=0; i<allResults.size(); i++){
                    allResults.get(i).setSelected(false);
                }
            }
        });
        SharedPreferences sharedPreferences = getSharedPreferences("selectMode",MODE_PRIVATE );
        sharedPreferences.edit().putBoolean("isSelectMode",false).apply();
    }

    @Override
    public void onBackPressed() {
        SharedPreferences sharedPreferences = getSharedPreferences("selectMode",MODE_PRIVATE );
        boolean isSelectMode = sharedPreferences.getBoolean("isSelectMode",false);
        if(isSelectMode){
            isSelectedReset(); // 선택모드 상태면 선택모드 상태 해제
        }else{
            super.onBackPressed(); // 선택모드 상태가 아니면 종료
            finish();
        }
    }

    public void loadMore(int position){
        if(position == allResults.size()){
            Toast.makeText(getApplicationContext(),"모든 메시지가 로딩 되었습니다.",Toast.LENGTH_SHORT).show();
        }else {
            Intent intent = getIntent();
            String room = intent.getStringExtra("room");
            RealmResults<Users> loadMoreResults = realm.where(Users.class).equalTo("room", room).sort("timeStamp", Sort.DESCENDING).limit(position + 20).findAll();
            // 해당 채팅방 메시지의 현재 메시지에 +20해서 추가 로딩
            SharedPreferences sharedPreferences = getSharedPreferences("selectMode",MODE_PRIVATE );
            boolean isSelectMode = sharedPreferences.getBoolean("isSelectMode",false);
            // 추가 로딩 시 선택모드 풀리지 않도록 롱클릭 시에 sharedPreferences로 저장한값을 가져온뒤 어댑터에 넘겨줌 (true로 넘어감)
            chatAdapter = new ChatAdapter(loadMoreResults, true, this, textToSpeech);
            recyclerView.setAdapter(chatAdapter);
            recyclerView.scrollToPosition(loadMoreResults.size() - position);
        }
    }


    public void speechMessage(String message){
        Bundle ttsParam = new Bundle();
        ttsParam.putFloat(KEY_PARAM_VOLUME, 0.5f);
        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, ttsParam, "1");
    }

    public void speechSeletedMessage(final RealmResults<Users> results){
        for(int i=0; i<results.size(); i++){
                Bundle ttsParam = new Bundle();
                ttsParam.putFloat(KEY_PARAM_VOLUME, 0.5f);
                textToSpeech.speak(results.get(i).getUserId() + "님으로부터 온" + (i+1) + "번" + "메시지는" + results.get(i).getMessage() + "입니다", TextToSpeech.QUEUE_ADD, ttsParam, "1");
                // QUEUE_FLUSH : 큐에 데이터 비운뒤 넣음.
                // QUEUE_ADD : 큐에 순차적으로 쌓음
        }

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(@NonNull Realm realm) {
                for (Users users : results) {
                    users.setRead(true);
                }
            }
        });
    }


    public void showDialog(final RealmResults<Users> results){
     if(results.size() > 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogCustom);
            builder.setTitle("메시지 읽기");
            builder.setMessage("해당 채팅방의 모든 메시지를 읽어 보시겠습니까?");
            builder.setPositiveButton("확인",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            speechSeletedMessage(results);
                        }
                    });
            builder.setNegativeButton("취소",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
            builder.show();
        }
    }

    // RealmResults<Users> to List<Model> 변환 메소드
    public List<Users> getModelLIst(){
        List<Users> list = new ArrayList<>();
        try {
            realm = Realm.getDefaultInstance();
            RealmResults<Users> results = realm.where(Users.class).sort("timeStamp",Sort.DESCENDING).findAllAsync(); // 비동기 쿼리
            list.addAll(realm.copyFromRealm(results));
        } finally {
            if (realm != null) {
                realm.close();
            }
        }
        return list;
    }

}
