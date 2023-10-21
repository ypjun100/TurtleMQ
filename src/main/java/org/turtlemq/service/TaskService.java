package org.turtlemq.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;
import org.turtlemq.data.Task;
import org.turtlemq.dto.Packet;

import java.util.LinkedList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class TaskService {
    private final BaseService service;
    private final WorkerService workerService;

    private final LinkedList<Task> taskQueue = new LinkedList<>();

    public void requestTask(WebSocketSession clientSession, Packet packet) {
//        if (clientService.hasClient(clientSession.getId())) {
            Task task = Task.builder()
                    .id(UUID.randomUUID().toString())
                    .requestor(clientSession)
                    .data(packet.getData())
                    .build();

            // if there is no idle worker, task will be inserted into task queue.
            if (workerService.assignTaskFailed(task)) {
                taskQueue.add(task);
                log.info("There is no space");
            }
//        }
    }

    public void responseTask(String workerId, Packet responsePacket) {
        workerService.responseTask(workerId, responsePacket);

        // If there is a waiting task, assign task.
        if (!taskQueue.isEmpty()) {
            Task task = taskQueue.removeFirst();

            if (workerService.assignTaskFailed(task)) {
                taskQueue.add(task);
                log.info("Opps. Idle worker is already taken.");
            }
        }
    }

    // Assign any task if there is a task in taskQueue and idle worker.
    public void assignTask() {
        // If there is no task in taskQueue and no idle worker, do nothing.
        if (taskQueue.isEmpty() || !workerService.hasIdleWorker())
            return;

        Task task = taskQueue.removeFirst();
        if (workerService.assignTaskFailed(task)) {
            taskQueue.add(task);
            log.info("Opps. Idle worker is already taken.");
        }
    }
}
