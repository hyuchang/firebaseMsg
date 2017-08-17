package firebase.hucloud.com.firemessenger.models;

import lombok.Data;

import java.util.Date;

/**
 */
@Data
public class Chat {

    private String chatId;
    private String title;
    private Date createDate;
    private TextMessage lastMessage;
    private boolean disabled;
    private int totalUnreadCount;
}
