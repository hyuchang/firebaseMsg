package firebase.hucloud.com.firemessenger.views;


import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import firebase.hucloud.com.firemessenger.R;
import firebase.hucloud.com.firemessenger.adapters.FriendListAdapter;
import firebase.hucloud.com.firemessenger.customviews.RecyclerViewItemClickListener;
import firebase.hucloud.com.firemessenger.models.User;

import java.util.Iterator;

/**
 * A simple {@link Fragment} subclass.
 */
public class FriendFragment extends Fragment {

    @BindView(R.id.search_area)
    LinearLayout mSearchArea;

    @BindView(R.id.edtContent)
    EditText edtEmail;

    @BindView(R.id.friendRecyclerView)
    RecyclerView mRecyclerView;

    private FirebaseUser mFirebaseUser;

    private FirebaseAuth mFirebaseAuth;

    private FirebaseDatabase mFirebaseDb;

    private DatabaseReference mFriendsDBRef;
    private DatabaseReference mUserDBRef;

    private FriendListAdapter friendListAdapter;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View friendView = inflater.inflate(R.layout.fragment_friends, container, false);
        ButterKnife.bind(this, friendView);

        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        mFirebaseDb = FirebaseDatabase.getInstance();

        mFriendsDBRef = mFirebaseDb.getReference("users").child(mFirebaseUser.getUid()).child("friends");
        mUserDBRef = mFirebaseDb.getReference("users");

        // 1. 리얼타임데이터베이스에서 나의 친구목록을 리스터를 통하여 데이터를 가져옵니다.
        addFriendListener();
        // 2. 가져온 데이터를 통해서 recyclerview의 아답터에 아이템을 추가 시켜줍니다. (UI)갱신
        friendListAdapter = new FriendListAdapter();
        mRecyclerView.setAdapter(friendListAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        // 3. 아이템별로 (친구) 클릭이벤트를 주어서 선택한 친구와 대화를 할 수 있도록 한다.
        mRecyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(getContext(), new RecyclerViewItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                final User friend = friendListAdapter.getItem(position);

                if (friendListAdapter.getSelectionMode() == FriendListAdapter.UNSELECTION_MODE) {
                    Snackbar.make(view, friend.getName()+"님과 대화를 하시겠습니까?", Snackbar.LENGTH_LONG).setAction("예", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent chatIntent = new Intent(getActivity(), ChatActivity.class);
                            chatIntent.putExtra("uid", friend.getUid());
                            startActivity(chatIntent);
                        }
                    }).show();
                } else {
                    friend.setSelection(friend.isSelection() ? false : true);
                    int selectedUserCount = friendListAdapter.getSelectionUsersCount();
                    Snackbar.make(view, selectedUserCount+"명과 대화를 하시겠습니까?", Snackbar.LENGTH_LONG).setAction("예", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent chatIntent = new Intent(getActivity(), ChatActivity.class);
                            chatIntent.putExtra("uids", friendListAdapter.getSelectedUids());
                            startActivity(chatIntent);
                        }
                    }).show();
                }

            }
        }));

        return friendView;
    }

    public void toggleSearchBar(){
        mSearchArea.setVisibility( mSearchArea.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE );
    }

    public void toggleSelectionMode(){
        friendListAdapter
                .setSelectionMode(friendListAdapter.getSelectionMode() == FriendListAdapter.SELECTION_MODE ? FriendListAdapter.UNSELECTION_MODE :
                        FriendListAdapter.SELECTION_MODE);
    }

    @OnClick(R.id.findBtn)
    public void addFriend(){

        // 1.입력된 이메일을 가져옵니다.
        final String inputEmail = edtEmail.getText().toString();
        // 2. 이메일이 입력되지 않았다면 이메일을 입력해주시라는 메세지를 띄워줍니다.
        if ( inputEmail.isEmpty()) {
            Snackbar.make(mSearchArea, "이메일을 입력해주세요. ", Snackbar.LENGTH_LONG).show();
            return;
        }
        // 3. 자기 자신을 친구로 등록할 수 없기때문에 FirebaseUser의 email이 입력한 이메일과 같다면, 자기자신은 등록 불가 메세지를 띄워줍니다.
        if ( inputEmail.equals(mFirebaseUser.getEmail())) {
            Snackbar.make(mSearchArea, "자기자신은 친구로 등록할 수 없습니다. ", Snackbar.LENGTH_LONG).show();
            return;
        }
        // 3. 이메일이 정상이라면 나의 정보를 조회하여 이미등록된 친구인지를 판단하고
        mFriendsDBRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterable<DataSnapshot> friendsIteratble = dataSnapshot.getChildren();
                Iterator<DataSnapshot> friendsIterator = friendsIteratble.iterator();


                while ( friendsIterator.hasNext()) {
                    User user = friendsIterator.next().getValue(User.class);

                    if ( user.getEmail().equals(inputEmail)) {
                        Snackbar.make(mSearchArea, "이미 등록된 친구입니다.", Snackbar.LENGTH_LONG).show();
                        return;
                    }
                }

                // 4. users db에 존재 하지 않는 이메일이라면, 가입하지 않는 친구라는 메세지를 띄워주고
                mUserDBRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Iterator<DataSnapshot> userIterator = dataSnapshot.getChildren().iterator();
                        int userCount = (int)dataSnapshot.getChildrenCount();
                        int loopCount = 1;


                        while (userIterator.hasNext()) {
                            final User currentUser = userIterator.next().getValue(User.class);
                            if ( inputEmail.equals(currentUser.getEmail())) {
                                // 친구 등록 로직
                                // 5. users/{myuid}/friends/{someone_uid}/firebasePush/상대 정보를 등록하고     //  저의 친구에 상대방을 등록
                                mFriendsDBRef.push().setValue(currentUser, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(final DatabaseError databaseError, DatabaseReference databaseReference) {
                                        // 6. users/{someone_uid}/friends/{myuid}/상대 정보를 등록하고

                                        // 나의 정보를 가져온다
                                        mUserDBRef.child(mFirebaseUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                User user = dataSnapshot.getValue(User.class);
                                                mUserDBRef.child(currentUser.getUid()).child("friends").push().setValue(user);
                                                Snackbar.make(mSearchArea, "친구등록이 완료되었습니다. ", Snackbar.LENGTH_LONG).show();
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {

                                            }
                                        });
                                    }
                                });
                            } else {
                                if ( loopCount++ >= userCount ) {
                                    Snackbar.make(mSearchArea, "가입을 하지 않은 친구입니다.", Snackbar.LENGTH_LONG).show();
                                    return;
                                }
                            }


                            // 총 사용자의 명수 == loopCount
                            // 등록된 사용자가 없다는 메세지를 출력 합니다.
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        // 5. users/{myuid}/friends/{someone_uid}/상대 정보를 등록하고
    }

    private void addFriendListener(){

        mFriendsDBRef.addChildEventListener(new ChildEventListener() {

            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                User friend = dataSnapshot.getValue(User.class);
                // 2. 가져온 데이터를 통해서 recyclerview의 아답터에 아이템을 추가 시켜줍니다. (UI)갱신
                drawUI(friend);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

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

    private void drawUI(User friend){
        friendListAdapter.addItem(friend);

    }

}
