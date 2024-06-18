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
    private Task assignedTask;
    private int pingPongCount;

    public void increasePingPongCount() { pingPongCount++; }
    public void resetPingPongCount() { pingPongCount = 0; }
}
