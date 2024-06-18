package org.turtlemq.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;
import org.turtlemq.dto.Packet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
public class ClientService {
    private final BaseService service;

    private final Map<String, WebSocketSession> clients = new HashMap<>();
    private final Map<String, Integer> clientPingPongCounters = new HashMap<>();

    public void register(WebSocketSession session, Packet packet) {
        if (!clients.containsKey(session.getId())) {
            clients.put(session.getId(), session);
            clientPingPongCounters.put(session.getId(), 0);
            service.send(session, packet);
        }
    }

    public void requestClientStatus() {
        Packet statusPacket = Packet.builder().type(Packet.MessageType.STATUS).build();
        for (WebSocketSession session : clients.values()) {
            if (session.isOpen() && clientPingPongCounters.get(session.getId()) < 1) {
                service.send(session, statusPacket);
                clientPingPongCounters.put(session.getId(), clientPingPongCounters.get(session.getId()) + 1);
            } else { // If client is disconnected, remove client.
                onClientTerminated(session.getId());
            }
        }
    }

    public void responseClientStatus(String sessionId) {
        if (clients.containsKey(sessionId)) {
            clientPingPongCounters.put(sessionId, 0);
        }
    }

    public void onClientTerminated(String clientId) {
        WebSocketSession session = clients.get(clientId);

        // It's okay to use remove() method without try-catch.
        // Because I already check whether the client is in clients hashmap.
        clients.remove(clientId);
        clientPingPongCounters.remove(clientId);

        // Close session.
        try {
            session.close();
        } catch (IOException e) {
            log.error(e);
        }
    }

    public boolean hasClient(String clientId) { return clients.containsKey(clientId); }
}
