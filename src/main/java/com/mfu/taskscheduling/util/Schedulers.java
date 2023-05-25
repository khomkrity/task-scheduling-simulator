package com.mfu.taskscheduling.util;

import com.mfu.taskscheduling.algorithm.util.Timeslot;
import com.mfu.taskscheduling.processor.Processor;
import com.mfu.taskscheduling.task.Task;

import java.util.*;

/**
 * The SchedulingHelper class provides utility methods for operations related to task scheduling simulation.
 */
public class Schedulers {
    private Schedulers() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Schedules a list of prioritized tasks for execution on assigned processors, taking into account potential port constraints.
     * The start and finish times for each task are determined based on the processor they are assigned to and the estimated
     * earliest start time, which is computed using the {@link Schedulers#getEarliestStartTime(Task, Processor, boolean)} method.
     * The computation cost is derived using the {@link Costs#getComputationCost(Task, Processor)} method.
     * <p>
     * If {@code hasPortConstraint} is set to {@code true}, the method also avoids port collisions by adjusting the start and finish
     * times for the sending and receiving stages of the tasks. The adjustments are done using the {@link Schedulers#avoidPortCollision(List, double, double, double, double)} method.
     * For each task, sending and receiving timeslots are created and added to a list of timeslots which is used to check for potential
     * port collisions in subsequent tasks.
     * <p>
     * This method modifies the start time, finish time, sending times, and receiving times of the tasks, as well as the ready time
     * and running time of the processors.
     *
     * @param prioritizedTasks  A list of tasks sorted in order of priority
     * @param hasPortConstraint A boolean indicating whether port constraints need to be taken into account during scheduling
     */
    public static void schedule(List<Task> prioritizedTasks, boolean hasPortConstraint) {
        List<Timeslot> timeslots = new ArrayList<>();

        for (Task currentTask : prioritizedTasks) {
            Processor currentProcessor = currentTask.getAssignedProcessor();
            double computationCost = Costs.getComputationCost(currentTask, currentProcessor);
            double earliestStartTime = Schedulers.getEarliestStartTime(currentTask, currentProcessor, false);
            double finishTime = earliestStartTime + computationCost;

            if (hasPortConstraint) {
                for (Task parent : currentTask.getParents()) {
                    earliestStartTime = Math.max(earliestStartTime, parent.getFinishReceivingTime());
                }
                double sendingLatency = currentTask.getSendingLatency();
                double receivingLatency = currentTask.getReceivingLatency();
                double startSendingTime = avoidPortCollision(timeslots, earliestStartTime, computationCost, sendingLatency, receivingLatency);
                double finishSendingTime = startSendingTime + sendingLatency;
                double startReceivingTime = finishSendingTime + computationCost;
                double finishReceivingTime = startReceivingTime + receivingLatency;

                currentTask.setReadyTime(earliestStartTime);

                currentTask.setStartSendingTime(startSendingTime);
                currentTask.setFinishSendingTime(finishSendingTime);

                currentTask.setStartTime(finishSendingTime);
                currentTask.setFinishTime(startReceivingTime);

                currentTask.setStartReceivingTime(startReceivingTime);
                currentTask.setFinishReceivingTime(finishReceivingTime);

                if (computationCost != 0.0) {
                    timeslots.add(new Timeslot(startSendingTime, finishSendingTime));
                    timeslots.add(new Timeslot(startReceivingTime, finishReceivingTime));
                }

                currentProcessor.setReadyTime(finishReceivingTime);
                currentProcessor.addRunningTime(computationCost);
            } else {
                currentTask.setStartTime(earliestStartTime);
                currentTask.setFinishTime(finishTime);
                currentProcessor.setReadyTime(finishTime);
                currentProcessor.addRunningTime(computationCost);
            }
        }
    }


    /**
     * Computes the earliest time at which the given task can start executing on the specified processor,
     * taking into account the latest finish times of all of its parent tasks and the ready time of the processor.
     * If the isEstimation flag is set to true, the earliest start time is based on estimated values for the processor
     * and parent task finish times.
     *
     * @param currentTask      The task whose earliest start time is being computed.
     * @param currentProcessor The processor on which the task is scheduled to run.
     * @param isEstimation     A flag indicating whether to use estimated values for processor and parent task finish times.
     * @return The earliest time at which the task can start executing on the specified processor.
     */
    public static double getEarliestStartTime(Task currentTask, Processor currentProcessor, boolean isEstimation) {
        double processorReadyTime = isEstimation ? currentProcessor.getEstimatedReadyTime() : currentProcessor.getReadyTime();
        double maxParentTaskFinishTime = getMaxParentTaskFinishTime(currentTask, currentProcessor, isEstimation);

        return Math.max(processorReadyTime, maxParentTaskFinishTime);
    }

    /**
     * Computes the maximum finish time of the parent tasks of the given task, taking into account communication costs
     * between processors.
     *
     * @param currentTask      the task to compute the maximum parent finish time for
     * @param currentProcessor the processor that will execute the current task
     * @param isEstimation     whether to use estimated finish times instead of actual finish times
     * @return the maximum finish time of the parent tasks
     */
    private static double getMaxParentTaskFinishTime(Task currentTask, Processor currentProcessor, boolean isEstimation) {
        double maxFinishTime = 0.0;

        for (Task parentTask : currentTask.getParents()) {
            Processor previousProcessor = parentTask.getAssignedProcessor();
            double parentTaskFinishTime = isEstimation ? parentTask.getEstimatedFinishTime() : parentTask.getFinishTime();
            if (parentTaskFinishTime < 0)
                throw new IllegalStateException("Violated the precedence constraint: all predecessors must already be completed.");
            double communicationCost = Costs.getCommunicationCost(parentTask, currentTask, previousProcessor, currentProcessor);
            double totalFinishTime = parentTaskFinishTime + communicationCost;
            maxFinishTime = Math.max(maxFinishTime, totalFinishTime);
        }

        return maxFinishTime;
    }

    /**
     * Finds a time slot that avoids port collisions between the given task and other tasks already scheduled,
     * and reserves the available time slot.
     *
     * @param allocatedTimeSlots A set of time slots already allocated to other tasks.
     * @param actualReadyTime    The original estimated start time for the task.
     * @param finishTime         The estimated finish time for the task.
     * @param sendingLatency     The time taken to send data from this task to another task.
     * @param receivingLatency   The time taken to receive data from another task to this task.
     * @param allocateTimeSlot   A boolean indicating whether to allocate the new ready time with no port collision into the timeslot
     * @return The updated estimated start time for the task.
     */
    public static double avoidPortCollisions(NavigableSet<Double> allocatedTimeSlots, double actualReadyTime, double finishTime, double sendingLatency, double receivingLatency, boolean allocateTimeSlot) {
        actualReadyTime = avoidPortCollision(allocatedTimeSlots, actualReadyTime, finishTime, sendingLatency);
        actualReadyTime = avoidPortCollision(allocatedTimeSlots, actualReadyTime, finishTime, receivingLatency);
        if (allocateTimeSlot) {
            allocatedTimeSlots.add(actualReadyTime);
            allocatedTimeSlots.add(finishTime);
        }
        return actualReadyTime;
    }


    /**
     * Finds a time slot that avoids port collisions between the given task and other tasks already scheduled,
     * and returns the updated estimated start time for the task.
     *
     * @param allocatedTimeSlots A set of time slots already allocated to other tasks.
     * @param startTime          The original estimated start time for the task.
     * @param finishTime         The estimated finish time for the task.
     * @param latency            The time taken to send or receive data from another task.
     * @return The updated estimated start time for the task.
     */
    private static double avoidPortCollision(NavigableSet<Double> allocatedTimeSlots, double startTime, double finishTime, double latency) {
        if (allocatedTimeSlots.isEmpty() || latency <= 0) return startTime;

        Double closestStartTime;
        Double closestFinishTime;
        boolean hasTimeOverlap;

        do {
            closestStartTime = allocatedTimeSlots.floor(startTime);
            closestFinishTime = allocatedTimeSlots.ceiling(finishTime);
            hasTimeOverlap = closestStartTime != null && Math.abs(startTime - closestStartTime) < 1.0 || closestFinishTime != null && Math.abs(finishTime - closestFinishTime) < 1.0;

            if (hasTimeOverlap) {
                startTime += latency;
                finishTime += latency;
            }
        } while (hasTimeOverlap);

        return startTime;
    }

    /**
     * This method calculates the earliest feasible start time for a task that avoids collisions
     * with already allocated timeslots in the task schedule. A collision is considered to occur
     * if the task's ready time, sending time or receiving time overlaps with any part of an
     * existing timeslot, considering a buffer zone on each side of the timeslot.
     *
     * @param timeslots        A list of Timeslot objects, each representing a previously allocated
     *                         timeslot in the task schedule. Each Timeslot object should have
     *                         getStartTime() and getFinishTime() methods to return the start and
     *                         finish times of the timeslot.
     * @param readyTime        The initial ready time of the task.
     * @param computationCost  The computation time required for the task.
     * @param sendingLatency   The latency time required for sending the task.
     * @param receivingLatency The latency time required for receiving the task.
     * @return The modified ready time that avoids collisions with the existing timeslots.
     * If no collisions occur with the initial ready time, the initial ready time is
     * returned.
     */
    public static double avoidPortCollision(List<Timeslot> timeslots, double readyTime, double computationCost, double sendingLatency, double receivingLatency) {
        if (timeslots.isEmpty() || computationCost == 0.0) return readyTime;

        double overlapBuffer = 1.0;  // The minimum allowable gap between tasks
        boolean hasOverlap;
        do {
            hasOverlap = false;
            double sendingTime = readyTime + sendingLatency;
            double finishTime = sendingTime + computationCost;
            double receivingTime = finishTime + receivingLatency;

            for (Timeslot timeslot : timeslots) {
                double start = timeslot.startTime() - overlapBuffer;
                double finish = timeslot.finishTime() + overlapBuffer;

                // Check for overlap
                if (Math.max(readyTime, start) < Math.min(receivingTime, finish)) {
                    hasOverlap = true;
                    readyTime = finish;
                    break;
                }
            }
        } while (hasOverlap);

        return readyTime;
    }

    /**
     * Finds the best time slot available to minimize the finish time of the given task on the specified processor,
     * with the constraint of not scheduling the task before the readyTime. If occupySlot is true, it reserves the
     * time slot in the schedule and updates the task's and processor's properties.
     *
     * @param scheduledTasksOnProcessor A list of tasks already scheduled on the processor.
     * @param task                      The task for which to find the finish time.
     * @param processor                 The processor on which the task will be scheduled.
     * @param readyTime                 The earliest time the task can be scheduled.
     * @param occupySlot                If true, reserves the time slot in the schedule and updates the task's and processor's properties.
     * @return The minimal finish time of the task on the specified processor.
     */
    public static double findEarliestFinishTime(List<Task> scheduledTasksOnProcessor, Task task, Processor processor, double readyTime, boolean occupySlot) {
        double computationCost = Costs.getComputationCost(task, processor);
        double startTime;
        double finishTime;
        int index;

        if (scheduledTasksOnProcessor.isEmpty()) {
            if (occupySlot) {
                task.setEstimatedStartTime(readyTime);
                task.setEstimatedFinishTime(readyTime + computationCost);
                task.setAssignedProcessor(processor);
                processor.setEstimatedReadyTime(readyTime + computationCost);
                scheduledTasksOnProcessor.add(task);
            }
            return readyTime + computationCost;
        }

        startTime = Math.max(readyTime, scheduledTasksOnProcessor.get(scheduledTasksOnProcessor.size() - 1).getEstimatedFinishTime());
        finishTime = startTime + computationCost;
        int currentIndex = scheduledTasksOnProcessor.size() - 1;
        int previousIndex = scheduledTasksOnProcessor.size() - 2;
        index = currentIndex + 1;

        while (previousIndex >= 0) {
            Task currentEvent = scheduledTasksOnProcessor.get(currentIndex);
            Task previousEvent = scheduledTasksOnProcessor.get(previousIndex);

            if (readyTime > previousEvent.getEstimatedFinishTime()) {
                if (readyTime + computationCost <= currentEvent.getEstimatedStartTime()) {
                    startTime = readyTime;
                    finishTime = readyTime + computationCost;
                }
                break;
            }

            if (previousEvent.getEstimatedFinishTime() + computationCost <= currentEvent.getEstimatedStartTime()) {
                startTime = previousEvent.getEstimatedFinishTime();
                finishTime = previousEvent.getEstimatedFinishTime() + computationCost;
                index = currentIndex;
            }

            currentIndex--;
            previousIndex--;
        }

        if (readyTime + computationCost <= scheduledTasksOnProcessor.get(0).getEstimatedStartTime()) {
            index = 0;
            startTime = readyTime;
        }

        finishTime = startTime + computationCost;

        if (occupySlot) {
            task.setEstimatedStartTime(startTime);
            task.setEstimatedFinishTime(finishTime);
            task.setAssignedProcessor(processor);
            processor.setEstimatedReadyTime(finishTime);
            scheduledTasksOnProcessor.add(index, task);
        }

        return finishTime;
    }
}
