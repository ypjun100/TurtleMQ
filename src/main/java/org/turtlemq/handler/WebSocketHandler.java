package org.turtlemq.handler;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.turtlemq.data.Task;
import org.turtlemq.data.Worker;
import org.turtlemq.dto.Packet;
import org.turtlemq.dto.WorkerPacket;
import org.turtlemq.service.ClientService;
import org.turtlemq.service.ConnectionService;
import org.turtlemq.service.TaskService;
import org.turtlemq.service.WorkerService;

@Component
@RequiredArgsConstructor
@Log4j2
public class WebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper mapper = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS) // ignore escape character
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // allow undeclared properties
            .build();

    private final WorkerService workerService;
    private final ClientService clientService;
    private final TaskService taskService;
    private final ConnectionService connectionService;

    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, TextMessage message) throws Exception {
        Packet packet = mapper.readValue(message.getPayload(), Packet.class);

        log.info(session.getId() + " → " + packet.toString());

        if (packet.getType().equals(Packet.MessageType.REGISTER_WORKER)) {
            workerService.register(session, packet);
            taskService.assignTask();
        } else if (packet.getType().equals(Packet.MessageType.REGISTER_CLIENT)) {
            clientService.register(session, packet);
        } else if (packet.getType().equals(Packet.MessageType.REQUEST_TASK)) {
            taskService.requestTask(session, packet);
        } else if (packet.getType().equals(Packet.MessageType.RESPONSE_TASK)) {
            WorkerPacket workerPacket = mapper.readValue(message.getPayload(), WorkerPacket.class);
            taskService.responseTask(session.getId(), workerPacket);
        } else if (packet.getType().equals(Packet.MessageType.STATUS)) {
            connectionService.responseStatus(session.getId(), packet);
        }
    }

    @Override
    public void afterConnectionClosed(@NotNull WebSocketSession session, @NotNull CloseStatus status) {
        connectionService.onConnectionClosed(session);
    }
}
