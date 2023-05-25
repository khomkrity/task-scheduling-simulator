package com.mfu.taskscheduling.output;

import com.mfu.taskscheduling.task.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents the result data for a task after the scheduling algorithm execution.
 */
public class TaskResult {
    private final int taskId;
    private final String assignedProcessor;
    private double sendingLatency;
    private double receivingLatency;
    private double startSendingTime;
    private double finishSendingTime;
    private double startReceivingTime;
    private double finishReceivingTime;
    private double readyTime;
    private final double startTime;
    private final double finishTime;
    private final List<Integer> parentIds;
    private final List<Integer> childIds;
    private final int depth;

    /**
     * Constructs a new TaskResult instance based on the provided task.
     *
     * @param task the task object containing the necessary information for this result
     */
    public TaskResult(Task task) {
        this.taskId = task.getId();
        this.assignedProcessor = task.getAssignedProcessor().getName();
        this.startTime = task.getStartTime();
        this.finishTime = task.getFinishTime();
        this.parentIds = new ArrayList<>(task.getParents().size());
        task.getParents().forEach(parent -> parentIds.add(parent.getId()));
        this.childIds = new ArrayList<>(task.getChildren().size());
        task.getChildren().forEach(children -> childIds.add(children.getId()));
        this.depth = task.getDepth();

        this.sendingLatency = task.getSendingLatency();
        this.receivingLatency = task.getReceivingLatency();
        this.readyTime = task.getReadyTime();
        this.startSendingTime = task.getStartSendingTime();
        this.finishSendingTime = task.getFinishSendingTime();
        this.startReceivingTime = task.getStartReceivingTime();
        this.finishReceivingTime = task.getFinishReceivingTime();
    }

    /**
     * Returns the ID of the task.
     *
     * @return the task ID
     */
    public int getTaskId() {
        return taskId;
    }

    /**
     * Returns the name of the assigned processor for the task.
     *
     * @return the name of the assigned processor
     */
    public String getAssignedProcessor() {
        return assignedProcessor;
    }

    /**
     * Returns the start time of the task.
     *
     * @return the start time of the task
     */
    public double getStartTime() {
        return startTime;
    }

    /**
     * Returns the finish time of the task.
     *
     * @return the finish time of the task
     */
    public double getFinishTime() {
        return finishTime;
    }

    /**
     * Returns a list of parent task IDs for the task.
     *
     * @return a list of parent task IDs
     */
    public List<Integer> getParentIds() {
        return parentIds;
    }

    /**
     * Returns a list of child task IDs for the task.
     *
     * @return a list of child task IDs
     */
    public List<Integer> getChildIds() {
        return childIds;
    }

    /**
     * Returns the depth of the task in the workflow.
     *
     * @return the task depth
     */
    public int getDepth() {
        return depth;
    }
}
