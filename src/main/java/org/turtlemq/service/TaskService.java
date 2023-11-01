package org.turtlemq.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;
import org.turtlemq.data.Task;
import org.turtlemq.dto.Packet;
import org.turtlemq.dto.WorkerPacket;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class TaskService {
    private final WorkerService workerService;
    private final ClientService clientService;

    private final LinkedList<Task> taskQueue = new LinkedList<>();

    private final Map<String, String> messageIds = new HashMap<>(); // key - task id, value - message id

    public void requestTask(WebSocketSession clientSession, Packet packet) {
        if (clientService.hasClient(clientSession.getId())) {
            String taskId = UUID.randomUUID().toString();
            messageIds.put(taskId, packet.getMessageId());

            Task task = Task.builder()
                    .id(taskId)
                    .requestor(clientSession)
                    .data(packet.getData())
                    .build();

            // if there is no idle worker, task will be inserted into task queue.
            if (workerService.assignTaskFailed(task)) {
                taskQueue.add(task);
                log.debug("Opps. There is no idle worker");
            }
        }
    }

    // Add a pre-defined task.
    public void requestTask(Task task) {
        // if there is no idle worker, task will be inserted into task queue.
        if (workerService.assignTaskFailed(task)) {
            taskQueue.add(task);
            log.debug("Opps. There is no idle worker");
        }
    }

    public void responseTask(String workerId, WorkerPacket responsePacket) {
        // If there is task that requested from 'requestTask()' method, return it.
        if (messageIds.containsKey(responsePacket.getTaskId())) {
            String messageId = messageIds.get(responsePacket.getTaskId());

            // If the response from worker is right response that server asked, assign task.
            if (workerService.responseTask(workerId, messageId, responsePacket)) {
                // If there is a waiting task in task queue, assign task.
                if (!taskQueue.isEmpty()) {
                    Task task = taskQueue.removeFirst();

                    // Assign a task.
                    if (workerService.assignTaskFailed(task)) {
                        taskQueue.add(task);
                        log.debug("Opps. There is no idle worker");
                    }
                }
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
            log.debug("Opps. There is no idle worker");
        }
    }
}
