package org.turtlemq.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;
import org.turtlemq.data.Task;
import org.turtlemq.data.Worker;
import org.turtlemq.dto.Packet;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class WorkerService {
    private final BaseService service;

    private final Map<String, Worker> workers = new HashMap<>();
    private final LinkedList<String> idleWorkers = new LinkedList<>();

    public void register(WebSocketSession session, Packet packet) {
        if (!workers.containsKey(session.getId())) {
            Worker worker = Worker.builder()
                    .session(session)
                    .status(Worker.WorkerStatus.IDLE)
                    .build();

            workers.put(session.getId(), worker);
            idleWorkers.add(session.getId());
            service.send(session, packet);
        } else {
            log.warn("There is same session id in worker. worker_id : " + session.getId());
        }
    }

    public boolean assignTaskFailed(Task task) {
        // if there is no idle worker, return false;
        if (idleWorkers.isEmpty()) {
            return true;
        }

        // Pop idle worker from idleWorkers.
        Worker worker = workers.get(idleWorkers.removeFirst());
        worker.setStatus(Worker.WorkerStatus.RUNNING);
        worker.setTask(task);

        // Send request task packet to worker.
        Packet packet = Packet.builder()
                .type(Packet.MessageType.REQUEST_TASK)
                .data(task.getData())
                .build();
        service.send(worker.getSession(), packet);
        return false;
    }

    public void responseTask(String workerId, Packet responsePacket) {
        if (workers.containsKey(workerId)) {
            // Send task response to client
            Worker worker = workers.get(workerId);

            // If worker send response that server didn't request, do nothing.
            if (worker.getStatus().equals(Worker.WorkerStatus.IDLE))
                return;

            Packet packet = Packet.builder()
                    .type(Packet.MessageType.RESPONSE_TASK)
                    .data(responsePacket.getData())
                    .build();
            service.send(worker.getTask().getRequestor(), packet);

            // Change worker's mode to idle.
            worker.setStatus(Worker.WorkerStatus.IDLE);
            worker.setTask(null);
            idleWorkers.add(workerId);
        }
    }

    public boolean hasIdleWorker() {
        return !idleWorkers.isEmpty();
    }
}
