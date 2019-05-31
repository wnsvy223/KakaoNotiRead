package com.example.wnsvy.kakaonotiread.Activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.example.wnsvy.kakaonotiread.Adapter.ChatAdapter;
import com.example.wnsvy.kakaonotiread.Adapter.MessageAdapter;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerView = findViewById(R.id.recyclerView);

        Intent intent = getIntent();
        String id = intent.getStringExtra("id");
        realm = Realm.getDefaultInstance();
        RealmResults<Users> realmResults = realm.where(Users.class).equalTo("userId",id).sort("timeStamp",Sort.ASCENDING).findAll();
        //인탠트로 넘어온 유저의 메시지 전체 조회(동기방식으로 받아와서 사이즈값을 UI스레드에서 받아 스크롤 이동 메소드 실행되도록 함)

        ChatAdapter chatAdapter = new ChatAdapter(realmResults,true, this, "none");
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);
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
