package org.turtlemq.dto;

import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class WorkerPacket extends Packet {
    private String taskId;

    @Builder(builderMethodName = "WorkerPacketBuilder")
    public WorkerPacket(MessageType type, String data, String taskId) {
        super(type, data);
        this.taskId = taskId;
    }
}
