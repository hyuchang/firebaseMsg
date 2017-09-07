package firebase.hucloud.com.firemessenger.views;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import firebase.hucloud.com.firemessenger.R;
import firebase.hucloud.com.firemessenger.adapters.MessageListAdapter;
import firebase.hucloud.com.firemessenger.models.*;

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
    private static final int TAKE_PHOTO_REQUEST_CODE = 201;
    private StorageReference mImageStorageRef;
    private FirebaseAnalytics mFirebaseAnalytics;



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
            ChatFragment.JOINED_ROOM = mChatId;
            initTotalunreadCount();
        } else {
            mChatRef = mFirebaseDb.getReference("users").child(mFirebaseUser.getUid()).child("chats");
        }
        messageListAdapter = new MessageListAdapter();
        mChatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mChatRecyclerView.setAdapter(messageListAdapter);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mChatId != null) {
            removeMessageListener();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mChatId != null) {

            // 총 메세지의 카운터를 가져온다.
            // onchildadded 호출한 변수의 값이 총메세지의 값과 크거나 같다면, 포커스를 맨아래로 내려줍니다.
            mChatMessageRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    long totalMessageCount =  dataSnapshot.getChildrenCount();
                    mMessageEventListener.setTotalMessageCount(totalMessageCount);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
            messageListAdapter.clearItem();
            addChatListener();
            addMessageListener();
        }
    }

    private void initTotalunreadCount(){
        mChatRef.child("totalUnreadCount").setValue(0);
    }

    MessageEventListener mMessageEventListener = new MessageEventListener();

    private void addChatListener(){
        mChatRef.child("title").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String title = dataSnapshot.getValue(String.class);
                if ( title != null ) {
                    mToolbar.setTitle(title);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    private void addMessageListener(){
        mChatMessageRef.addChildEventListener(mMessageEventListener);
    }

    private void removeMessageListener() {
        mChatMessageRef.removeEventListener(mMessageEventListener);
    }

    private class MessageEventListener implements ChildEventListener {

        private long totalMessageCount;

        private long callCount = 1;

        public void setTotalMessageCount(long totalMessageCount) {
            this.totalMessageCount = totalMessageCount;
        }

        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {

            // 신규메세지
            Message item = dataSnapshot.getValue(Message.class);

            // 읽음 처리
            // chat_messages > {chat_id} > {message_id} > readUserList
            // 내가 존재 하는지를 확인
            // 존재한다면
            // 존재 하지 않는다면
            // chat_messages > {chat_id} > {message_id} >  unreadCount -= 1
            // readUserList에 내 uid 추가
            List<String> readUserUIDList = item.getReadUserList();
            if ( readUserUIDList != null ) {
                if ( !readUserUIDList.contains(mFirebaseUser.getUid())) {
                    // chat_messages > {chat_id} > {message_id} >  unreadCount -= 1

                    // messageRef.setValue();
                    dataSnapshot.getRef().runTransaction(new Transaction.Handler() {
                        @Override
                        public Transaction.Result doTransaction(MutableData mutableData) {
                            Message mutableMessage = mutableData.getValue(Message.class);
                            // readUserList에 내 uid 추가
                            // unreadCount -= 1

                            List<String> mutabledReadUserList = mutableMessage.getReadUserList();
                            mutabledReadUserList.add(mFirebaseUser.getUid());
                            int mutableUnreadCount = mutableMessage.getUnreadCount() - 1;

                            if ( mutableMessage.getMessageType() == Message.MessageType.PHOTO) {
                                PhotoMessage mutablePhotoMessage = mutableData.getValue(PhotoMessage .class);
                                mutablePhotoMessage.setReadUserList(mutabledReadUserList);
                                mutablePhotoMessage.setUnreadCount(mutableUnreadCount);
                                mutableData.setValue(mutablePhotoMessage);
                            } else {
                                TextMessage mutableTextMessage = mutableData.getValue(TextMessage.class);
                                mutableTextMessage.setReadUserList(mutabledReadUserList);
                                mutableTextMessage.setUnreadCount(mutableUnreadCount);
                                mutableData.setValue(mutableTextMessage);
                            }
                            return Transaction.success(mutableData);
                        }

                        @Override
                        public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                            initTotalunreadCount();
                        }
                    });
                }
            }

            // ui
            if ( item.getMessageType() == Message.MessageType.TEXT ) {
                TextMessage textMessage = dataSnapshot.getValue(TextMessage.class);
                messageListAdapter.addItem(textMessage);
            } else if ( item.getMessageType() == Message.MessageType.PHOTO ){
                PhotoMessage photoMessage = dataSnapshot.getValue(PhotoMessage.class);
                messageListAdapter.addItem(photoMessage);
            } else if ( item.getMessageType() == Message.MessageType.EXIT ){
                messageListAdapter.addItem(item);
            }

            if ( callCount >= totalMessageCount) {
                // 스크롤을 맨 마지막으로 내린다.
                mChatRecyclerView.scrollToPosition(messageListAdapter.getItemCount() - 1);
            }
            callCount++;
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            // 변경된 메세지 ( unreadCount)
            // 아답터쪽에 변경된 메세지데이터를 전달하고
            // 메시지 아이디 번호로 해당 메세지의 위치를 알아내서
            // 알아낸 위치값을 이용해서 메세지 리스트의 값을 변경할 예정입니다.
            Message item = dataSnapshot.getValue(Message.class);

            if ( item.getMessageType() == Message.MessageType.TEXT ) {
                TextMessage textMessage = dataSnapshot.getValue(TextMessage.class);
                messageListAdapter.updateItem(textMessage);
            } else if ( item.getMessageType() == Message.MessageType.PHOTO ){
                PhotoMessage photoMessage = dataSnapshot.getValue(PhotoMessage.class);
                messageListAdapter.updateItem(photoMessage);
            }
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
    }


    @OnClick(R.id.senderBtn)
    public void onSendEvent(View v){

        if ( mChatId != null ) {
            sendMessage();
        } else {
            createChat();
        }
    }

    @OnClick(R.id.photoSend)
    public void onPhotoSendEvent(View v) {
        // 안드로이드 파일창 오픈 (갤러리 오픈)
        // requestcode = 201
        //TAKE_PHOTO_REQUEST_CODE

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, TAKE_PHOTO_REQUEST_CODE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ( requestCode == TAKE_PHOTO_REQUEST_CODE ) {
            if ( data != null ) {

                // 업로드 이미지를 처리 합니다.
                // 이미지 업로드가 완료된 경우
                // 실제 web 에 업로드 된 주소를 받아서 photoUrl로 저장
                // 그다음 포토메세지 발송
                uploadImage(data.getData());

            }
        }
    }

    private String mPhotoUrl = null;
    private Message.MessageType mMessageType = Message.MessageType.TEXT;

    private void uploadImage(Uri data){
        if ( mImageStorageRef == null ) {
            mImageStorageRef = FirebaseStorage.getInstance().getReference("/chats/").child(mChatId);
        }
        mImageStorageRef.putFile(data).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if ( task.isSuccessful() ) {
                    mPhotoUrl = task.getResult().getDownloadUrl().toString();
                    mMessageType = Message.MessageType.PHOTO;
                    sendMessage();
                }
            }
        });
        //firebase Storage
    }


    private Message message = new Message();
    private void sendMessage(){
        // 메세지 키 생성
        mChatMessageRef = mFirebaseDb.getReference("chat_messages").child(mChatId);
        // chat_message>{chat_id}>{message_id} > messageInfo
        String messageId = mChatMessageRef.push().getKey();
        String messageText = mMessageText.getText().toString();

        final Bundle bundle = new Bundle();
        bundle.putString("me", mFirebaseUser.getEmail());
        bundle.putString("roomId", mChatId);

        if ( mMessageType == Message.MessageType.TEXT ) {
            if ( messageText.isEmpty()) {
                return;
            }
            message = new TextMessage();
            ((TextMessage)message).setMessageText(messageText);
            bundle.putString("messageType", Message.MessageType.TEXT.toString());
        } else if ( mMessageType == Message.MessageType.PHOTO ){
            message = new PhotoMessage();
            ((PhotoMessage)message).setPhotoUrl(mPhotoUrl);
            bundle.putString("messageType", Message.MessageType.PHOTO.toString());
        }

        message.setMessageDate(new Date());
        message.setChatId(mChatId);
        message.setMessageId(messageId);
        message.setMessageType(mMessageType);
        message.setMessageUser(new User(mFirebaseUser.getUid(), mFirebaseUser.getEmail(), mFirebaseUser.getDisplayName(), mFirebaseUser.getPhotoUrl().toString()));
        message.setReadUserList(Arrays.asList(new String[]{mFirebaseUser.getUid()}));

        String [] uids = getIntent().getStringArrayExtra("uids");
        if ( uids != null ) {
            message.setUnreadCount(uids.length-1);
        }
        mMessageText.setText("");
        mMessageType = Message.MessageType.TEXT;
        mChatMemeberRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                //unreadCount 셋팅하기 위한 대화 상대의 수를 가져 옵니다.
                long memberCount = dataSnapshot.getChildrenCount();
                message.setUnreadCount((int)memberCount - 1);
                mChatMessageRef.child(message.getMessageId()).setValue(message, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                        mFirebaseAnalytics.logEvent("sendMessage", bundle);
                        Iterator<DataSnapshot> memberIterator = dataSnapshot.getChildren().iterator();
                        while( memberIterator.hasNext()) {
                            User chatMember = memberIterator.next().getValue(User.class);
                            mUserRef
                                    .child(chatMember.getUid())
                                    .child("chats")
                                    .child(mChatId)
                                    .child("lastMessage")
                                    .setValue(message);

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

        mChatId = mChatRef.push().getKey();
        mChatRef = mChatRef.child(mChatId);
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
                                        addChatListener();
                                        addMessageListener();
                                        isSentMessage = true;

                                        Bundle bundle = new Bundle();
                                        bundle.putString("me", mFirebaseUser.getEmail());
                                        bundle.putString("roomId", mChatId);
                                        mFirebaseAnalytics.logEvent("createChat", bundle);
                                        ChatFragment.JOINED_ROOM = mChatId;

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
