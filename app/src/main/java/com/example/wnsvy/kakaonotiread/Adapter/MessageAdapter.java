package com.example.wnsvy.kakaonotiread.Adapter;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.wnsvy.kakaonotiread.Activity.ChatActivity;
import com.example.wnsvy.kakaonotiread.Model.Users;
import com.example.wnsvy.kakaonotiread.R;

import de.hdodenhof.circleimageview.CircleImageView;
import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;

public class MessageAdapter extends RealmRecyclerViewAdapter<Users, MessageAdapter.ViewHolder>{
    private Context context;
    private RealmResults realmResults;

    class ViewHolder extends  RecyclerView.ViewHolder{

        private TextView userId;
        private TextView message;
        private TextView timeStamp;
        private CircleImageView circleImageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userId = itemView.findViewById(R.id.id);
            message = itemView.findViewById(R.id.message);
            timeStamp = itemView.findViewById(R.id.timeStamp);
            circleImageView = itemView.findViewById(R.id.imgView);
        }
    }

    public MessageAdapter(@Nullable RealmResults<Users> data, boolean autoUpdate, Context context) {
        super(data, autoUpdate);
        setHasStableIds(true);
        this.context = context;
        this.realmResults = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.list_user_item, viewGroup, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int position) {
        if(viewHolder.getAdapterPosition() != RecyclerView.NO_POSITION) {
            final Users users = getItem(viewHolder.getAdapterPosition());
            viewHolder.userId.setText(users.getUserId());
            viewHolder.message.setText(users.getMessage());
            viewHolder.timeStamp.setText(users.getTimeStamp());
            Glide.with(context).load(R.drawable.kakaotalk).override(150, 150).into(viewHolder.circleImageView);

            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                        Intent intent = new Intent(context, ChatActivity.class);
                        intent.putExtra("id", users.getUserId());
                        context.startActivity(intent);
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
