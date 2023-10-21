package org.turtlemq.data;

import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.socket.WebSocketSession;

@Data
@Builder
public class Worker {
    public enum WorkerStatus { IDLE, RUNNING }

    private @NotNull WebSocketSession session;
    private @NotNull WorkerStatus status;
    private Task task;
}
