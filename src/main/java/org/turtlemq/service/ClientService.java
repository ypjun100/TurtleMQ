package org.turtlemq.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;
import org.turtlemq.dto.Packet;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClientService {
    private final BaseService service;

    private final Map<String, WebSocketSession> clients = new HashMap<>();

    public void register(WebSocketSession session, Packet packet) {
        if (!clients.containsKey(session.getId())) {
            clients.put(session.getId(), session);
            service.send(session, packet);
        }
    }
}
