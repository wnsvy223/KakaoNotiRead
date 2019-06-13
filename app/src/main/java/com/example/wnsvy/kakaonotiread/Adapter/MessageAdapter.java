package com.example.wnsvy.kakaonotiread.Adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.wnsvy.kakaonotiread.Activity.ChatActivity;
import com.example.wnsvy.kakaonotiread.Model.Users;
import com.example.wnsvy.kakaonotiread.R;

import de.hdodenhof.circleimageview.CircleImageView;
import io.realm.Realm;
import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;
import io.realm.Sort;

public class MessageAdapter extends RealmRecyclerViewAdapter<Users, MessageAdapter.ViewHolder>{
    private Context context;
    private RealmResults realmResults;
    private Realm realm;

    class ViewHolder extends  RecyclerView.ViewHolder{

        private TextView room;
        private TextView message;
        private TextView timeStamp;
        private TextView badgeCount;
        private CircleImageView circleImageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            room = itemView.findViewById(R.id.id);
            message = itemView.findViewById(R.id.message);
            timeStamp = itemView.findViewById(R.id.timeStamp);
            badgeCount = itemView.findViewById(R.id.badgecount);
            circleImageView = itemView.findViewById(R.id.imgView);
        }
    }

    public MessageAdapter(@Nullable RealmResults<Users> data, boolean autoUpdate, Context context, Realm realm) {
        super(data, autoUpdate);
        setHasStableIds(true);
        this.context = context;
        this.realmResults = data;
        this.realm = realm;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.list_user_item, viewGroup, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        if(viewHolder.getAdapterPosition() != RecyclerView.NO_POSITION) {
            final Users users = getItem(position);
            viewHolder.room.setText(users.getRoom());
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

            RealmResults<Users> res = realm.where(Users.class).sort("timeStamp",Sort.DESCENDING).equalTo("room",users.getRoom()).equalTo("isRead",false).findAll();
            // 읽지 않은 메시지 쿼리
            if(res.size() <= 0){
                viewHolder.badgeCount.setVisibility(View.INVISIBLE);
            }else{
                viewHolder.badgeCount.setText(String.valueOf(res.size()));
            }

            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                        Intent intent = new Intent(context, ChatActivity.class);
                        intent.putExtra("room", users.getRoom());
                        //context.startActivity(intent);
                        ((Activity)context).startActivityForResult(intent, 1);
                        // ChatActivity에서 메시지를 읽고 카운트값을 줄인후 갱신을 위해 호출( => 이 방법보다 콜백인터페이스가 정상적인 방법이라고 함)
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

    @Override
    public long getItemId(int position) {
        return getItem(position).hashCode();
    }
}
