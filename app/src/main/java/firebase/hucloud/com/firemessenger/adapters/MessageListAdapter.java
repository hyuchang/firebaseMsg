package firebase.hucloud.com.firemessenger.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import firebase.hucloud.com.firemessenger.R;
import firebase.hucloud.com.firemessenger.customviews.RoundedImageView;
import firebase.hucloud.com.firemessenger.models.Message;
import firebase.hucloud.com.firemessenger.models.PhotoMessage;
import firebase.hucloud.com.firemessenger.models.TextMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class MessageListAdapter extends RecyclerView.Adapter<MessageListAdapter.MessageViewHolder>{


    private ArrayList<Message> mMessageList;

    private SimpleDateFormat messageDateFormat = new SimpleDateFormat("MM/dd a\n hh:mm");

    private String userId;

    public MessageListAdapter() {
        mMessageList = new ArrayList<>();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public void addItem(Message item) {
        mMessageList.add(item);
        notifyDataSetChanged();
    }

    public void updateItem(Message item) {
        int position = getItemPosition(item.getMessageId());
        if ( position < 0) {
            return;
        }
        mMessageList.set(position, item);
        notifyItemChanged(position);
    }

    public void clearItem() {
        mMessageList.clear();
    }

    private int getItemPosition(String messageId){
        int position = 0;
        for (Message message : mMessageList ) {
            if ( message.getMessageId().equals(messageId)) {
                return position;
            }
            position++;
        }
        return -1;
    }

    public Message getItem(int position) {
        return mMessageList.get(position);
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_message_item, parent, false);
        // view 를 이용한 뷰홀더 리턴
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        // 전달받은 뷰 홀터를 이용한 뷰 구현
        Message item = getItem(position);

        TextMessage textMessage = null;
        PhotoMessage photoMessage = null;

        if ( item instanceof TextMessage) {
            textMessage = (TextMessage) item;
        } else if (item instanceof PhotoMessage) {
            photoMessage = (PhotoMessage) item;
        }

        // 내가 보낸 메세지 인지, 받은 메세지 인지 판별 합니다.

        if ( userId.equals(item.getMessageUser().getUid())) {
            // 내가 보낸 메시지 구현
            // 텍스트 메세지 인지 포토 메세지 인지 구별
            if ( item.getMessageType() == Message.MessageType.TEXT ) {
                holder.sendTxt.setText(textMessage.getMessageText());
                holder.sendTxt.setVisibility(View.VISIBLE);
                holder.sendImage.setVisibility(View.GONE);

            } else if ( item.getMessageType() == Message.MessageType.PHOTO ){
                Glide.with(holder.sendArea)
                        .load(photoMessage.getPhotoUrl())
                        .into(holder.sendImage);

                holder.sendTxt.setVisibility(View.GONE);
                holder.sendImage.setVisibility(View.VISIBLE);
            }

            if ( item.getUnreadCount() > 0 ) {
                holder.sendUnreadCount.setText(String.valueOf(item.getUnreadCount()));
            } else {
                holder.sendUnreadCount.setText("");
            }
            holder.sendDate.setText(messageDateFormat.format(item.getMessageDate()));
            holder.yourArea.setVisibility(View.GONE);
            holder.sendArea.setVisibility(View.VISIBLE);
            holder.exitArea.setVisibility(View.GONE);

        } else {
            // 상대방이 보낸 경우
            if ( item.getMessageType() == Message.MessageType.TEXT ) {

                holder.rcvTextView.setText(textMessage.getMessageText());
                holder.rcvTextView.setVisibility(View.VISIBLE);
                holder.rcvImage.setVisibility(View.GONE);

            } else if ( item.getMessageType() == Message.MessageType.PHOTO ){
                Glide
                    .with(holder.yourArea)
                    .load(photoMessage.getPhotoUrl())
                    .into(holder.rcvImage);

                holder.rcvTextView.setVisibility(View.GONE);
                holder.rcvImage.setVisibility(View.VISIBLE);
            } else if ( item.getMessageType() == Message.MessageType.EXIT ) {
                // #이름 님이 방에서 나가셨습니다.
                holder.exitTextView.setText(String.format("%s님이 방에서 나가셨습니다.", item.getMessageUser().getName()));
            }

            if ( item.getUnreadCount() > 0 ) {
                holder.rcvUnreadCount.setText(String.valueOf(item.getUnreadCount()));
            }  else {
                holder.rcvUnreadCount.setText("");
            }

            if ( item.getMessageUser().getProfileUrl() != null ) {
                Glide
                        .with(holder.yourArea)
                        .load(item.getMessageUser().getProfileUrl())
                        .into(holder.rcvProfileView);
            }

            if ( item.getMessageType() == Message.MessageType.EXIT ) {
                holder.yourArea.setVisibility(View.GONE);
                holder.sendArea.setVisibility(View.GONE);
                holder.exitArea.setVisibility(View.VISIBLE);
            } else {
                holder.rcvDate.setText(messageDateFormat.format(item.getMessageDate()));
                holder.yourArea.setVisibility(View.VISIBLE);
                holder.sendArea.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.yourChatArea)
        LinearLayout yourArea;

        @BindView(R.id.myChatArea)
        LinearLayout sendArea;

        @BindView(R.id.exitArea)
        LinearLayout exitArea;

        @BindView(R.id.rcvProfile)
        RoundedImageView rcvProfileView;

        @BindView(R.id.rcvTxt)
        TextView rcvTextView;

        @BindView(R.id.exitTxt)
        TextView exitTextView;

        @BindView(R.id.rcvImage)
        ImageView rcvImage;

        @BindView(R.id.rcvUnreadCount)
        TextView rcvUnreadCount;

        @BindView(R.id.rcvDate)
        TextView rcvDate;

        @BindView(R.id.sendUnreadCount)
        TextView sendUnreadCount;

        @BindView(R.id.sendDate)
        TextView sendDate;

        @BindView(R.id.sendTxt)
        TextView sendTxt;

        @BindView(R.id.sendImage)
        ImageView sendImage;


        public MessageViewHolder(View v) {
            super(v);
            ButterKnife.bind(this, v);
        }
    }
}
