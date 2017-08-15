package firebase.hucloud.com.firemessenger.models;

import lombok.Data;

@Data
public class User {
    private String uid, email, name, profileUrl;
    private boolean selection;
}
