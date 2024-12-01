package com.example.persona.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.persona.R;
import com.example.persona.models.Conversation;

import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Conversation conversation);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(Conversation conversation);
    }

    private List<Conversation> conversationList;
    private OnItemClickListener listener;
    private OnItemLongClickListener longClickListener;

    public ConversationAdapter(List<Conversation> conversationList, OnItemClickListener listener, OnItemLongClickListener longClickListener) {
        this.conversationList = conversationList;
        this.listener = listener;
        this.longClickListener = longClickListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView nameTextView;
        public TextView lastMessageTextView;
        public ImageView avatarImageView;

        public ViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.conversationName);
            lastMessageTextView = itemView.findViewById(R.id.conversationLastMessage);
            avatarImageView = itemView.findViewById(R.id.conversationAvatar);
        }

        public void bind(final Conversation conversation, final OnItemClickListener listener, final OnItemLongClickListener longClickListener) {
            nameTextView.setText(conversation.getOtherUserUid());
            lastMessageTextView.setText(conversation.getLastMessage());
            avatarImageView.setImageResource(conversation.getAvatarResource());

            itemView.setOnClickListener(v -> listener.onItemClick(conversation));
            itemView.setOnLongClickListener(v -> {
                longClickListener.onItemLongClick(conversation);
                return true;
            });
        }
    }

    @Override
    public ConversationAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.conversation_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ConversationAdapter.ViewHolder holder, int position) {
        holder.bind(conversationList.get(position), listener, longClickListener);
    }

    @Override
    public int getItemCount() {
        return conversationList.size();
    }
}
