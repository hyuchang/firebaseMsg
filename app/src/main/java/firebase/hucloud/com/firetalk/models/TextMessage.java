package firebase.hucloud.com.firetalk.models;

import lombok.Data;

@Data
public class TextMessage extends Message{
    private String messageText;
}
