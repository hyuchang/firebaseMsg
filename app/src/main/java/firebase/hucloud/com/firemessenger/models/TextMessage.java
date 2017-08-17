package firebase.hucloud.com.firemessenger.models;

import lombok.Data;

@Data
public class TextMessage extends Message{
    private String messageText;
}
