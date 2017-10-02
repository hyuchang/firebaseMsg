package firebase.hucloud.com.firetalk.models;

import lombok.Data;

@Data
public class User {

    private String uid, email, name, profileUrl;
    private boolean selection;

    public User(){

    }

    public User(String uid, String email, String name, String profileUrl) {
        this.uid = uid;
        this.email = email;
        this.name = name;
        this.profileUrl = profileUrl;
    }


}
