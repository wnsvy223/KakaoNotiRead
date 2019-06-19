package com.example.wnsvy.kakaonotiread.Adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.wnsvy.kakaonotiread.Model.Users;
import com.example.wnsvy.kakaonotiread.R;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import io.realm.Realm;
import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;
import io.realm.Sort;

import static android.content.Context.MODE_PRIVATE;
import static android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME;

public class ChatAdapter extends RealmRecyclerViewAdapter<Users, ChatAdapter.ViewHolder>{
    private Context context;;
    private RealmResults<Users> realmResults;
    private TextToSpeech textToSpeech;

    class ViewHolder extends  RecyclerView.ViewHolder implements View.OnClickListener{

        private TextView userId;
        private TextView message;
        private TextView timeStamp;
        private CircleImageView circleImageView;
        private TextView isRead;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userId = itemView.findViewById(R.id.id);
            message = itemView.findViewById(R.id.my_chat_view);
            timeStamp = itemView.findViewById(R.id.tvTime);
            circleImageView = itemView.findViewById(R.id.ivUser);
            isRead = itemView.findViewById(R.id.isRead);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Realm realm = Realm.getDefaultInstance();
            final Users users = getItem(getAdapterPosition());
            SharedPreferences sharedPreferences = context.getSharedPreferences("selectMode",MODE_PRIVATE );
            boolean isSelectMode = sharedPreferences.getBoolean("isSelectMode",false);
            if(isSelectMode) { // 선택모드일때 클릭 시 해당 row의 선택값 변경
                if(!users.isSelected()){
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(@NonNull Realm realm) {
                            Users user = realm.where(Users.class).equalTo("timeStamp", users.getTimeStamp()).findFirst();
                            if (user != null) {
                                user.setSelected(true); // 선택
                            }
                        }
                    });
                }else{
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(@NonNull Realm realm) {
                            Users user = realm.where(Users.class).equalTo("timeStamp", users.getTimeStamp()).findFirst();
                            if (user != null) {
                                user.setSelected(false); // 해제
                            }
                        }
                    });
                }
            }else {
                speechTTS(users.getUserId(), users.getMessage()); // 선택한 메시지 읽기
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(@NonNull Realm realm) {
                        Users user = realm.where(Users.class).equalTo("timeStamp", users.getTimeStamp()).findFirst();
                        if (user != null) {
                            user.setRead(true);
                        }
                        // 클릭한 메시지의 타임스탬프(고유값이므로)로 위치를 찾아 해당 위치의 isRead값 true로 업데이트
                    }
                });
            }
        }

    }

    public ChatAdapter(@Nullable RealmResults<Users> data, boolean autoUpdate, Context context, TextToSpeech textToSpeech) {
        super(data, autoUpdate);
        setHasStableIds(true);
        this.context = context;
        this.realmResults = data;
        this.textToSpeech = textToSpeech;
    }

    @NonNull
    @Override
    public ChatAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.list_message_item, viewGroup, false);
        return new ChatAdapter.ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {

        if(viewHolder.getAdapterPosition() != RecyclerView.NO_POSITION) {
            final Users users = getItem(position);
            viewHolder.userId.setText(users.getUserId());
            viewHolder.message.setText(users.getMessage());
            viewHolder.timeStamp.setText(users.getTimeStamp());
            switch (users.getType()){
                case "KakaoTalk":
                    Glide.with(context).load(R.drawable.kakaotalk).override(150, 150).into(viewHolder.circleImageView);
                    break;
                case "MMS":
                    Glide.with(context).load(R.drawable.icon_mms).override(150, 150).into(viewHolder.circleImageView);
                    break;
                default:
            }

            // 메시지 읽음/읽지않음 처리
            if(users.isRead()){
                viewHolder.isRead.setVisibility(View.INVISIBLE);
            }else{
                viewHolder.isRead.setText("읽지않음");
            }

            // 메시지 롱 클릭 선택 처리
            if(users.isSelected()){
                highlightView(viewHolder);
            }else{
                unhighlightView(viewHolder);
            }

        }
    }

    private void speechTTS(String userId,String message){
        Bundle ttsParam = new Bundle();
        ttsParam.putFloat(KEY_PARAM_VOLUME, 0.5f);
        textToSpeech.speak(userId + "님으로부터 온 메시지는" + message + "입니다", TextToSpeech.QUEUE_FLUSH, ttsParam, "1");
        // QUEUE_FLUSH : 큐에 데이터 비운뒤 넣음.
        // QUEUE_ADD : 큐에 순차적으로 쌓음
    }

    private void highlightView(ViewHolder holder) {
        holder.itemView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.darker_gray)); // 회색 하이라이트 처리
    }

    private void unhighlightView(ViewHolder holder) {
        holder.itemView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent)); // 원래 상태로 변경
    }

    @Override
    public int getItemCount() {
        return realmResults.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).hashCode();
    }

}
