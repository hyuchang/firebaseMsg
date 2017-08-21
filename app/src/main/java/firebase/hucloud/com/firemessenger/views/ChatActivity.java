package firebase.hucloud.com.firemessenger.views;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import firebase.hucloud.com.firemessenger.R;
import firebase.hucloud.com.firemessenger.models.Chat;
import firebase.hucloud.com.firemessenger.models.Message;
import firebase.hucloud.com.firemessenger.models.TextMessage;
import firebase.hucloud.com.firemessenger.models.User;

import java.util.*;

public class ChatActivity extends AppCompatActivity {

    private String mChatId;

    @BindView(R.id.senderBtn)
    ImageView mSenderButton;

    @BindView(R.id.edtContent)
    EditText mMessageText;

    private FirebaseDatabase mFirebaseDb;

    private DatabaseReference mChatRef;
    private DatabaseReference mChatMemeberRef;
    private DatabaseReference mChatMessageRef;
    private DatabaseReference mUserRef;

    private FirebaseUser mFirebaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_chat);
        ButterKnife.bind(this);
        mChatId = getIntent().getStringExtra("chat_id");
        mFirebaseDb = FirebaseDatabase.getInstance();
        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        mUserRef = mFirebaseDb.getReference("users");

    }

    @OnClick(R.id.senderBtn)
    public void onSendEvent(View v){

        if ( mChatId != null ) {
            sendMessage();
        } else {
            createChat();
        }
    }

    private void sendMessage(){
        // 메세지 키 생성
        mChatMessageRef = mFirebaseDb.getReference("chat_messages").child(mChatId);
        // chat_message>{chat_id}>{message_id} > messageInfo
        String messageId = mChatMessageRef.push().getKey();
        String messageText = mMessageText.getText().toString();

        if ( messageText.isEmpty()) {
            return;
        }

        final TextMessage textMessage = new TextMessage();
        textMessage.setMessageText(messageText);
        textMessage.setMessageDate(new Date());
        textMessage.setChatId(mChatId);
        textMessage.setMessageId(messageId);
        textMessage.setMessageType(Message.MessageType.TEXT);
        textMessage.setReadUserList(Arrays.asList(new String[]{mFirebaseUser.getUid()}));
        String [] uids = getIntent().getStringArrayExtra("uids");
        if ( uids != null ) {
            textMessage.setUnreadCount(uids.length-1);
        }
        mMessageText.setText("");
        mChatMemeberRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                //unreadCount 셋팅하기 위한 대화 상대의 수를 가져 옵니다.
                long memberCount = dataSnapshot.getChildrenCount();
                textMessage.setUnreadCount((int)memberCount - 1);
                mChatMessageRef.child(textMessage.getMessageId()).setValue(textMessage, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                        Iterator<DataSnapshot> memberIterator = dataSnapshot.getChildren().iterator();
                        while( memberIterator.hasNext()) {
                            User chatMember = memberIterator.next().getValue(User.class);
                            mUserRef
                                    .child(chatMember.getUid())
                                    .child("chats")
                                    .child(mChatId)
                                    .child("lastMessage")
                                    .setValue(textMessage);

                            if ( !chatMember.getUid().equals(mFirebaseUser.getUid())) {
                                mUserRef
                                    .child(chatMember.getUid())
                                    .child("chats")
                                    .child(mChatId)
                                    .child("totalUnreadCount")
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            long totalUnreadCount = dataSnapshot.getValue(long.class);
                                            dataSnapshot.getRef().setValue(totalUnreadCount+1);

                                        }
                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });
                            }



                        }
                    }
                });


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });










    }


    private boolean isSentMessage = false;
    private void createChat() {
        // <방생성>

        // 0. 방 정보 설정 <-- 기존 방이어야 가능함.

        // 1. 대화 상대에 내가 선택한 사람 추가

        // 2. 각 상대별 chats에 방추가

        // 3. 메세지 정보 중 읽은 사람에 내 정보를 추가

        // 4. 4.  첫 메세지 전송

        final Chat chat = new Chat();
        mChatRef = mFirebaseDb.getReference("users").child(mFirebaseUser.getUid()).child("chats");
        mChatId = mChatRef.push().getKey();
        mChatMemeberRef = mFirebaseDb.getReference("chat_members").child(mChatId);
        chat.setChatId(mChatId);
        chat.setCreateDate(new Date());
        String uid = getIntent().getStringExtra("uid");
        String [] uids = getIntent().getStringArrayExtra("uids");
        if ( uid != null ) {
            // 1:1
            uids = new String[]{uid};
        }

        List<String> uidList = new ArrayList<>(Arrays.asList(uids));
        uidList.add(mFirebaseUser.getUid());

        for ( String userId : uidList ) {
            // uid > userInfo
            mUserRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(final DataSnapshot dataSnapshot) {
                    User member = dataSnapshot.getValue(User.class);

                    mChatMemeberRef.child(member.getUid())
                            .setValue(member, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                    // USERS>uid>chats>{chat_id}>chatinfo
                                    dataSnapshot.getRef().child("chats").child(mChatId).setValue(chat);
                                    if ( !isSentMessage ) {
                                        sendMessage();
                                        isSentMessage = true;
                                    }

                                }
                            });

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });


        }











        // users > {uid} > chats > {chat_uid}


    }


}
