package firebase.hucloud.com.firemessenger.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import firebase.hucloud.com.firemessenger.R;
import firebase.hucloud.com.firemessenger.customviews.RoundedImageView;
import firebase.hucloud.com.firemessenger.models.Chat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * 해당 파일은 소유권은 신휴창에게 있습니다.
 * 현재 오픈 소스로 공개중인 버전은 AGPL을 따르는 오픈 소스 프로젝트이며,
 * 소스 코드를 수정하셔서 사용하는 경우에는 반드시 동일한 라이센스로 소스 코드를 공개하여야 합니다.
 * 만약 HUCLOUD를 상업적으로 이용하실 경우에는 라이센스를 구매하여 사용하셔야 합니다.
 * email : huttchang@gmail.com
 * 프로젝트명    : firebaseMsg
 * 작성 및 소유자 : hucloud
 * 최초 생성일   : 2017. 8. 22.
 */
public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatHolder> {

    private ArrayList<Chat> mChatList;
    private SimpleDateFormat sdf = new SimpleDateFormat("MM/dd\naa hh:mm");
    public ChatListAdapter() {
        mChatList = new ArrayList<>();
    }

    public void addItem(Chat chat) {
        mChatList.add(chat);
        notifyDataSetChanged();
    }

    public Chat getItem(int position) {
        return this.mChatList.get(position);
    }

    @Override
    public ChatHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_chat_item, parent, false);
        return new ChatHolder(view);
    }

    @Override
    public void onBindViewHolder(ChatHolder holder, int position) {

        Chat item = getItem(position);

        // chatThumbnailView

        holder.lastMessageView.setText(item.getLastMessage().getMessageText());
        holder.titleView.setText(item.getTitle());
        holder.lastMessageDateView.setText(sdf.format(item.getCreateDate()));
        if (item.getTotalUnreadCount() > 0 )
            holder.totalUnreadCountView.setText(String.valueOf(item.getTotalUnreadCount()));
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


        public ChatHolder(View v) {
            super(v);
            ButterKnife.bind(this, v);
        }
    }


}
