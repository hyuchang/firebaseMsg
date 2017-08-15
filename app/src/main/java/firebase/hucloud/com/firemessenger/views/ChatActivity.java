package firebase.hucloud.com.firemessenger.views;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import firebase.hucloud.com.firemessenger.R;

public class ChatActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_chat);

        String uid = getIntent().getStringExtra("uid");
        String [] uids = getIntent().getStringArrayExtra("uids");

    }
}
