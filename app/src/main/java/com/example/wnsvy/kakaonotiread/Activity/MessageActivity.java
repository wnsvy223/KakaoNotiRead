package com.example.wnsvy.kakaonotiread.Activity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.wnsvy.kakaonotiread.Adapter.MessageAdapter;
import com.example.wnsvy.kakaonotiread.Model.Users;
import com.example.wnsvy.kakaonotiread.R;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class MessageActivity extends AppCompatActivity {

    private Realm realm;
    public RecyclerView recyclerView;
    private RealmResults<Users> result;
    public MessageAdapter messageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });

        setCollapsingToolbarLayout();
        recyclerView = findViewById(R.id.recyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);

        realm = Realm.getDefaultInstance();
        RealmResults<Users> results = realm.where(Users.class).sort("timeStamp",Sort.DESCENDING).distinct("room").findAll();
        // 타임스탬프로 내림차순 정렬 후 방이름 중복 제거 : 각 채팅방의 마지막 메시지만 출력하기 위함

        messageAdapter = new MessageAdapter(results,true, this, realm);
        messageAdapter.setHasStableIds(true);
        recyclerView.setAdapter(messageAdapter);
    }

    private void setCollapsingToolbarLayout(){
        final CollapsingToolbarLayout collapsingToolbarLayout = findViewById(R.id.toolbar_layout);
        collapsingToolbarLayout.setTitle("메시지");
        collapsingToolbarLayout.setExpandedTitleTextColor(ColorStateList.valueOf(Color.BLACK));
        final int myDrawable = R.drawable.kakao;
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1){
            messageAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
    }

}
