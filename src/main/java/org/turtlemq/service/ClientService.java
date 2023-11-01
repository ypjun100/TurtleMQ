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

    public void requestClientStatus() {
        Packet statusPacket = Packet.builder().type(Packet.MessageType.STATUS).build();
        for (WebSocketSession session : clients.values()) {
            if (session.isOpen())
                service.send(session, statusPacket);
            else { // If client is disconnected, remove client.
                onClientTerminated(session.getId());
            }
        }
    }

    public void onClientTerminated(String clientId) {
        // It's okay to use remove() method without try-catch.
        // Because I already check whether the client is in clients hashmap.
        clients.remove(clientId);
    }

    public boolean hasClient(String clientId) { return clients.containsKey(clientId); }
}
