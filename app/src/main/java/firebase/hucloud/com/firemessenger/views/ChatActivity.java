package firebase.hucloud.com.firemessenger.views;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
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
import firebase.hucloud.com.firemessenger.adapters.MessageListAdapter;
import firebase.hucloud.com.firemessenger.models.*;
import org.w3c.dom.Text;

import java.util.*;

public class ChatActivity extends AppCompatActivity {

    private String mChatId;

    @BindView(R.id.senderBtn)
    ImageView mSenderButton;

    @BindView(R.id.edtContent)
    EditText mMessageText;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.chat_rec_view)
    RecyclerView mChatRecyclerView;

    private MessageListAdapter messageListAdapter;

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
        mToolbar.setTitleTextColor(Color.WHITE);
        if ( mChatId != null ) {
            mChatRef = mFirebaseDb.getReference("users").child(mFirebaseUser.getUid()).child("chats").child(mChatId);
            mChatMessageRef = mFirebaseDb.getReference("chat_messages").child(mChatId);
            mChatMemeberRef = mFirebaseDb.getReference("chat_members").child(mChatId);
            mChatRef.child("title").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String title = dataSnapshot.getValue(String.class);
                    mToolbar.setTitle(title);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
            initTotalunreadCount();
        } else {
            mChatRef = mFirebaseDb.getReference("users").child(mFirebaseUser.getUid()).child("chats");
        }
        messageListAdapter = new MessageListAdapter();
        mChatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mChatRecyclerView.setAdapter(messageListAdapter);

    }

    @Override
    protected void onPause() {
        super.onPause();
        removeMessageListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mChatId != null) {
            addMessageListener();
        }

    }

    private void initTotalunreadCount(){
        mChatRef.child("totalUnreadCount").setValue(0);
    }

    private void addMessageListener(){
        mChatMessageRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                // 신규메세지
                Message item = dataSnapshot.getValue(Message.class);
                // 내가 메세지를 읽었는지 부터 확인 하고
                // 안읽었다면
                // readUserList 내 uid를 추가 하고
                // 읽었다면 메세지를 로딩 해주는 기능을 구현해보도록 하겠습니다.
                List<String> readUserList = item.getReadUserList();
                if ( readUserList != null ) {
                    if ( !readUserList.contains(mFirebaseUser.getUid())) {
                        readUserList.add(mFirebaseUser.getUid());

                        // user>{sss}>chats




                    }

                }





                if ( item.getMessageType() == Message.MessageType.TEXT ) {
                    TextMessage textMessage = dataSnapshot.getValue(TextMessage.class);
                    messageListAdapter.addItem(textMessage);
                } else if ( item.getMessageType() == Message.MessageType.PHOTO ){
                    PhotoMessage photoMessage = dataSnapshot.getValue(PhotoMessage.class);
                    messageListAdapter.addItem(photoMessage);
                }
                // 읽음 처리를.
                // ui
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                // 변경된 메세지 ( unreadCount)
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void removeMessageListener() {

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
        textMessage.setMessageUser(new User(mFirebaseUser.getUid(), mFirebaseUser.getEmail(), mFirebaseUser.getDisplayName(), mFirebaseUser.getPhotoUrl().toString()));
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
