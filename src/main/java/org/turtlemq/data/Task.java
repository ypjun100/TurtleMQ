package org.turtlemq.data;

import lombok.Builder;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

@Data
@Builder
public class Task {
    private String id;
    private WebSocketSession requestor;
    private String data;
    private String result;
}
