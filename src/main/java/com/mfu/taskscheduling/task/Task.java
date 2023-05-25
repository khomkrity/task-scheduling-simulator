package com.mfu.taskscheduling.task;

import com.mfu.taskscheduling.processor.Processor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a task in a scheduling problem. A task has an ID, length, parent and child tasks,
 * associated file items, sending and receiving latencies, and various scheduling-related properties,
 * such as start and finish times, estimated start and finish times, priority, and assigned processor.
 */
public class Task {
    private final int id;
    private final double length;
    private final List<Task> parents;
    private final List<Task> children;
    private final List<FileItem> fileItems;
    private final double sendingLatency;
    private final double receivingLatency;
    private int depth;
    private double startTime;
    private double finishTime;
    private double estimatedStartTime;
    private double estimatedFinishTime;
    private double startSendingTime;
    private double finishSendingTime;
    private double startReceivingTime;
    private double finishReceivingTime;
    private double readyTime;
    private double priority;
    private Processor assignedProcessor;
    private boolean isEstimated;

    /**
     * Creates a new Task object with the given parameters.
     *
     * @param id               the ID of the task
     * @param length           the length of the task
     * @param fileItems        the list of file items associated with the task
     * @param sendingLatency   the sending latency of the task
     * @param receivingLatency the receiving latency of the task
     */
    public Task(int id, double length, List<FileItem> fileItems, double sendingLatency, double receivingLatency) {
        this.id = id;
        this.length = length;
        this.sendingLatency = sendingLatency;
        this.receivingLatency = receivingLatency;
        this.parents = new ArrayList<>();
        this.children = new ArrayList<>();
        this.fileItems = fileItems;
        this.finishTime = -1;
    }

    /**
     * Returns the depth of the task in the task graph.
     *
     * @return the task depth
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Sets the depth of the task in the task graph.
     *
     * @param depth the task depth
     */
    public void setDepth(int depth) {
        this.depth = depth;
    }

    /**
     * Returns the start time of the task.
     *
     * @return the task start time
     */
    public double getStartTime() {
        return startTime;
    }

    /**
     * Sets the start time of the task.
     *
     * @param startTime the task start time
     */
    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    /**
     * Returns the finish time of the task.
     *
     * @return the task finish time
     */
    public double getFinishTime() {
        return finishTime;
    }

    /**
     * Sets the finish time of the task.
     *
     * @param finishTime the task finish time
     */
    public void setFinishTime(double finishTime) {
        this.finishTime = finishTime;
    }

    /**
     * Returns the priority of the task.
     *
     * @return the task priority
     */
    public double getPriority() {
        return priority;
    }

    /**
     * Sets the priority of the task.
     *
     * @param priority the task priority
     */
    public void setPriority(double priority) {
        this.priority = priority;
    }

    /**
     * Returns the assigned processor for the task.
     *
     * @return the assigned processor
     */
    public Processor getAssignedProcessor() {
        return assignedProcessor;
    }

    /**
     * assigns a processor to the task.
     *
     * @param assignedProcessor the assigned processor
     */
    public void setAssignedProcessor(Processor assignedProcessor) {
        this.assignedProcessor = assignedProcessor;
    }

    /**
     * Returns the ID of the task.
     *
     * @return the task ID
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the length of the task.
     *
     * @return the task length
     */
    public double getLength() {
        return length;
    }

    /**
     * Returns the list of parent tasks.
     *
     * @return the list of parent tasks
     */
    public List<Task> getParents() {
        return parents;
    }

    /**
     * Adds a parent task to the current task.
     *
     * @param task the parent task to be added
     */
    public void addParent(Task task) {
        parents.add(task);
    }

    /**
     * Checks if the current task is an entry task (i.e., it has no parent tasks).
     *
     * @return true if the task is an entry task, false otherwise
     */
    public boolean isEntry() {
        return parents.isEmpty();
    }

    /**
     * Checks if the current task is an exit task (i.e., it has no child tasks).
     *
     * @return true if the task is an exit task, false otherwise
     */
    public boolean isExit() {
        return children.isEmpty();
    }

    /**
     * Returns the list of child tasks.
     *
     * @return the list of child tasks
     */
    public List<Task> getChildren() {
        return children;
    }

    /**
     * Adds a child task to the current task.
     *
     * @param task the child task to be added
     */
    public void addChild(Task task) {
        children.add(task);
    }

    /**
     * Retrieves the siblings of the current task.
     * <p>
     * Siblings are defined as the set of tasks that share at least one common parent with the current task.
     *
     * @return a List containing the sibling tasks
     */
    public List<Task> getSiblings() {
        return this.getParents()
                .stream()
                .flatMap(parent -> parent.getChildren().stream())
                .toList();
    }

    /**
     * Returns the sending latency of the task.
     *
     * @return the sending latency
     */
    public double getSendingLatency() {
        return sendingLatency;
    }

    /**
     * Returns the receiving latency of the task.
     *
     * @return the receiving latency
     */
    public double getReceivingLatency() {
        return receivingLatency;
    }

    /**
     * Returns the list of file items associated with the task.
     *
     * @return the list of file items
     */
    public List<FileItem> getFileItems() {
        return fileItems;
    }

    /**
     * Returns the estimated start time of the task.
     *
     * @return the estimated task start time
     */
    public double getEstimatedStartTime() {
        return estimatedStartTime;
    }

    /**
     * Sets the estimated start time of the task.
     *
     * @param estimatedStartTime the estimated task start time
     */
    public void setEstimatedStartTime(double estimatedStartTime) {
        this.estimatedStartTime = estimatedStartTime;
    }

    /**
     * Returns the estimated finish time of the task.
     *
     * @return the estimated task finish time
     */
    public double getEstimatedFinishTime() {
        return estimatedFinishTime;
    }

    /**
     * Sets the estimated finish time of the task.
     *
     * @param estimatedFinishTime the estimated task finish time
     */
    public void setEstimatedFinishTime(double estimatedFinishTime) {
        this.estimatedFinishTime = estimatedFinishTime;
    }

    /**
     * Resets the task's start time, estimated start time, finish time, estimated finish time,
     * priority, and assigned processor to their default values.
     */
    public void reset() {
        double defaultValue = 0.0;
        setStartTime(defaultValue);
        setEstimatedStartTime(defaultValue);
        setFinishTime(-1);
        setEstimatedFinishTime(defaultValue);
        setPriority(defaultValue);
        setAssignedProcessor(null);
        setEstimated(false);


        setStartSendingTime(defaultValue);
        setStartReceivingTime(defaultValue);
        setFinishSendingTime(defaultValue);
        setFinishReceivingTime(defaultValue);
        setReadyTime(defaultValue);
    }

    /**
     * Returns the estimation status of the task.
     *
     * @return whether the task is estimated
     */
    public boolean isEstimated() {
        return isEstimated;
    }

    /**
     * Sets the estimation status of the task in the task graph.
     *
     * @param estimated the task estimation status
     */
    public void setEstimated(boolean estimated) {
        isEstimated = estimated;
    }

    public double getStartSendingTime() {
        return startSendingTime;
    }

    public void setStartSendingTime(double startSendingTime) {
        this.startSendingTime = startSendingTime;
    }

    public double getFinishSendingTime() {
        return finishSendingTime;
    }

    public void setFinishSendingTime(double finishSendingTime) {
        this.finishSendingTime = finishSendingTime;
    }

    public double getStartReceivingTime() {
        return startReceivingTime;
    }

    public void setStartReceivingTime(double startReceivingTime) {
        this.startReceivingTime = startReceivingTime;
    }

    public double getFinishReceivingTime() {
        return finishReceivingTime;
    }

    public void setFinishReceivingTime(double finishReceivingTime) {
        this.finishReceivingTime = finishReceivingTime;
    }

    public double getReadyTime() {
        return readyTime;
    }

    public void setReadyTime(double readyTime) {
        this.readyTime = readyTime;
    }
}
