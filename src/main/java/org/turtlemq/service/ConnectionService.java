package org.turtlemq.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;
import org.turtlemq.data.Task;
import org.turtlemq.data.Worker;
import org.turtlemq.dto.Packet;

@Service
@RequiredArgsConstructor
@EnableScheduling
@Log4j2
public class ConnectionService {
    private final BaseService service;
    private final WorkerService workerService;
    private final ClientService clientService;
    private final TaskService taskService;

    // Check clients and workers status every fixedRate second.
    @Scheduled(fixedRate = 10000)
    private void ping() {
        clientService.requestClientStatus();
        workerService.requestWorkerStatus();
    }

    public void responseStatus(String sessionId, Packet responsePacket) {
        clientService.responseClientStatus(sessionId);
        workerService.responseWorkerStatus(sessionId);
    }

    public void onConnectionClosed(WebSocketSession session) {
        // If terminated device is worker, remove worker.
        if (workerService.hasWorker(session.getId())) {
            log.warn(session.getId() + "(Worker) is terminated.");
            workerService.onWorkerTerminated(session.getId());
        } else if (clientService.hasClient(session.getId())) { // If client is terminated, remove client.
            log.warn(session.getId() + "(Client) is terminated.");
            clientService.onClientTerminated(session.getId());
        }
    }
}
