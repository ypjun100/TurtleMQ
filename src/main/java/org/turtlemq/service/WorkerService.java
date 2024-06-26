package org.turtlemq.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;
import org.turtlemq.data.Task;
import org.turtlemq.data.Worker;
import org.turtlemq.dto.Packet;
import org.turtlemq.dto.WorkerPacket;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class WorkerService {
    private BaseService service;
    private TaskService taskService;

    private final Map<String, Worker> workers = new HashMap<>(); // key - session id
    private final LinkedList<String> idleWorkers = new LinkedList<>(); // save session ids of workers

    @Autowired
    public WorkerService(BaseService baseService, @Lazy TaskService taskService) {
        // TaskService and WorkerService have circular dependency, added lazy keyword to fix this relation.
        this.service = baseService;
        this.taskService = taskService;
    }

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

    // Return true if task assign failed, and false for success.
    public boolean assignTaskFailed(Task task) {
        // if there is no idle worker, return false;
        if (idleWorkers.isEmpty()) {
            log.debug("idle workers : {}", idleWorkers);
            return true;
        }

        // Pop an element from idle worker from idleWorkers.
        Worker worker = workers.get(idleWorkers.removeFirst());
        worker.setStatus(Worker.WorkerStatus.RUNNING);
        worker.setAssignedTask(task);

        // Send request task packet to worker.
        WorkerPacket workerPacket = WorkerPacket.WorkerPacketBuilder()
                .type(Packet.MessageType.REQUEST_TASK)
                .data(task.getData())
                .taskId(task.getId())
                .build();
        service.send(worker.getSession(), workerPacket);

        log.debug("idle workers : {}", idleWorkers);
        return false;
    }

    // Check response from worker is right response that server requested.
    public boolean responseTask(String workerId, String messageId, WorkerPacket responsePacket) {
        if (workers.containsKey(workerId)) {
            // Send task response to client
            Worker worker = workers.get(workerId);

            // If worker sent response that server didn't request, do nothing.
            if (worker.getStatus().equals(Worker.WorkerStatus.IDLE)) { return false; }
            // If worker sent another work response, give a chance one more.
            if (!worker.getAssignedTask().getId().equals(responsePacket.getTaskId())) {
                responsePacket.setData(worker.getAssignedTask().getData());
                responsePacket.setTaskId(worker.getAssignedTask().getId());
                service.send(worker.getSession(), responsePacket);
                return false;
            }

            Packet packet = Packet.builder()
                    .type(Packet.MessageType.RESPONSE_TASK)
                    .messageId(messageId)
                    .data(responsePacket.getData())
                    .build();

            WebSocketSession requestor = worker.getAssignedTask().getRequestor(); // client who requested task
            if (requestor.isOpen()) // Only if client connection is opened, send the response.
                service.send(requestor, packet);

            // Change worker's mode to idle.
            worker.setStatus(Worker.WorkerStatus.IDLE);
            worker.setAssignedTask(null);
            idleWorkers.add(workerId);
            return true;
        }
        return false;
    }

    public void requestWorkerStatus() {
        Packet statusPacket = Packet.builder().type(Packet.MessageType.STATUS).build();
        for (Worker worker : workers.values()) {
            WebSocketSession session = worker.getSession();
            if (session.isOpen() && worker.getPingPongCount() < 1) {
                service.send(session, statusPacket);
                worker.increasePingPongCount();
            } else { // If worker is disconnected, remove client.
                onWorkerTerminated(session.getId());
            }
        }
    }

    public void responseWorkerStatus(String sessionId) {
        if (workers.containsKey(sessionId)) {
            workers.get(sessionId).resetPingPongCount();
        }
    }

    // If a worker is terminated, assign the task originally given to terminated worker to another.
    public void onWorkerTerminated(String sessionId) {
        // Check whether terminated worker is valid.
        if (workers.containsKey(sessionId)) {
            Worker worker = workers.get(sessionId); // terminated worker

            // If status of worker is IDLE, remove worker in idleWorkers.
            if (worker.getStatus().equals(Worker.WorkerStatus.IDLE)) {
                idleWorkers.remove(sessionId);
            } else if (worker.getStatus().equals(Worker.WorkerStatus.RUNNING)) {
                // If terminated worker has a task to complete, assign that task to another worker.
                Task assignedTask = worker.getAssignedTask();
                taskService.requestTask(assignedTask);
            }
            workers.remove(sessionId); // remove worker in workers either.

            // Close session.
            try {
                worker.getSession().close();
            } catch (IOException e) {
                log.debug(e);
            }
        }
    }

    public boolean hasIdleWorker() {
        return !idleWorkers.isEmpty();
    }
    public boolean hasWorker(String workerId) { return workers.containsKey(workerId); }
}
