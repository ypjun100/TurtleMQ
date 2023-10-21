package org.turtlemq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Packet {
    public enum MessageType { REGISTER_WORKER, REGISTER_CLIENT, STATUS, REQUEST_TASK, RESPONSE_TASK }

    private MessageType type;
    private String data;
}
