package firebase.hucloud.com.firemessenger.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import firebase.hucloud.com.firemessenger.R;
import firebase.hucloud.com.firemessenger.customviews.RoundedImageView;
import firebase.hucloud.com.firemessenger.models.Chat;
import firebase.hucloud.com.firemessenger.views.ChatFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatHolder> {

    private ArrayList<Chat> mChatList;
    private SimpleDateFormat sdf = new SimpleDateFormat("MM/dd\naa hh:mm");
    private ChatFragment mChatFragment;
    public ChatListAdapter() {
        mChatList = new ArrayList<>();
    }

    public void setFragment(ChatFragment chatFragment) {
        this.mChatFragment = chatFragment;
    }

    public void addItem(Chat chat) {
        mChatList.add(chat);
        notifyDataSetChanged();
    }

    public void updateItem(Chat chat) {
        int changedItemPosition = getItemPosition(chat.getChatId());
        if ( changedItemPosition > -1 ) {
            mChatList.set(changedItemPosition ,chat);
            notifyItemChanged(changedItemPosition);
        }
    }

    public Chat getItem(int position) {
        return this.mChatList.get(position);
    }

    private int getItemPosition(String chatId) {
        int position = 0;
        for ( Chat currItem : mChatList ) {
            if ( currItem.getChatId().equals(chatId)) {
                return position;
            }
            position++;
        }
        return -1;
    }

    public Chat getItem(String chatId) {
        return getItem(getItemPosition(chatId));
    }

    @Override
    public ChatHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_chat_item, parent, false);
        return new ChatHolder(view);
    }

    @Override
    public void onBindViewHolder(ChatHolder holder, int position) {

        final Chat item = getItem(position);

        // chatThumbnailView
        if ( item.getLastMessage() != null ) {
            holder.lastMessageView.setText(item.getLastMessage().getMessageText());
            holder.lastMessageDateView.setText(sdf.format(item.getLastMessage().getMessageDate()));
        }
        holder.titleView.setText(item.getTitle());
        holder.rootView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if ( mChatFragment != null ) {
                    mChatFragment.leaveChat(item);
                }
                return true;
            }
        });

        if (item.getTotalUnreadCount() > 0 )
            holder.totalUnreadCountView.setText(String.valueOf(item.getTotalUnreadCount()));
        else
            holder.totalUnreadCountView.setText("");
    }

    @Override
    public int getItemCount() {
        return mChatList.size();
    }

    public static class ChatHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.thumb)
        RoundedImageView chatThumbnailView;

        @BindView(R.id.title)
        TextView titleView;

        @BindView(R.id.lastmessage)
        TextView lastMessageView;

        @BindView(R.id.totalUnreadCount)
        TextView totalUnreadCountView;

        @BindView(R.id.lastMsgDate)
        TextView lastMessageDateView;

        @BindView(R.id.rootView)
        LinearLayout rootView;


        public ChatHolder(View v) {
            super(v);
            ButterKnife.bind(this, v);
        }
    }


}
