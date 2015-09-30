package com.aware.plugin.queuetracescollector.app;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aware.plugin.queuetracescollector.R;

import java.util.List;

/**
 * Created by researcher on 22/06/15.
 */
public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    private List<Message> messageList;

    public MessagesAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    @Override
    public void onBindViewHolder(MessageViewHolder contactViewHolder, int i) {
        Message message = messageList.get(i);
        contactViewHolder.vAlias.setText(message.getAlias());
        contactViewHolder.vMessage.setText(message.getMessage());
        contactViewHolder.vTime.setText(message.getNormalTime());
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.messages_cell, viewGroup, false);

        return new MessageViewHolder(itemView);
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        protected TextView vAlias;
        protected TextView vMessage;
        protected TextView vTime;

        public MessageViewHolder(View v) {
            super(v);
            vAlias =  (TextView) v.findViewById(R.id.messages_cell_alias);
            vMessage = (TextView)  v.findViewById(R.id.messages_cell_message);
            vTime = (TextView)  v.findViewById(R.id.messages_cell_time);
        }
    }
}