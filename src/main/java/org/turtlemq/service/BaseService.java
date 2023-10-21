package org.turtlemq.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.turtlemq.dto.Packet;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Log4j2
public class BaseService {
    protected final ObjectMapper mapper;

    public void send(WebSocketSession session, Packet packet) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(mapper.writeValueAsString(packet)));
                log.info(session.getId() + " ← " + packet.toString());
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
