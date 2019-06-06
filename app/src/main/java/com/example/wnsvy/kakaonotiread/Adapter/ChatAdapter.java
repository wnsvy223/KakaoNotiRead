package com.example.wnsvy.kakaonotiread.Adapter;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.example.wnsvy.kakaonotiread.Model.Users;
import com.example.wnsvy.kakaonotiread.R;
import de.hdodenhof.circleimageview.CircleImageView;
import io.realm.Realm;
import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;
import static android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME;

public class ChatAdapter extends RealmRecyclerViewAdapter<Users, ChatAdapter.ViewHolder> {
    private Context context;;
    private RealmResults<Users> realmResults;
    private TextToSpeech textToSpeech;
    private Realm realm;

    class ViewHolder extends  RecyclerView.ViewHolder{

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
        }
    }

    public ChatAdapter(@Nullable RealmResults<Users> data, boolean autoUpdate, Context context, TextToSpeech textToSpeech, Realm realm) {
        super(data, autoUpdate);
        setHasStableIds(true);
        this.context = context;
        this.realmResults = data;
        this.textToSpeech = textToSpeech;
        this.realm = realm;
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
            Glide.with(context).load(R.drawable.kakaotalk).override(150, 150).into(viewHolder.circleImageView);
            if(users.isRead()){
                viewHolder.isRead.setVisibility(View.INVISIBLE);
            }else{
                viewHolder.isRead.setText("1");
            }

            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                        Bundle ttsParam = new Bundle();
                        ttsParam.putFloat(KEY_PARAM_VOLUME, 2.0f);
                        textToSpeech.speak(users.getUserId() + "님으로부터 온 메시지는" + users.getMessage() + "입니다", TextToSpeech.QUEUE_FLUSH, ttsParam, "1");
                        RealmResults<Users> test = realm.where(Users.class).equalTo("timeStamp",users.getTimeStamp()).findAll();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return realmResults.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

}
