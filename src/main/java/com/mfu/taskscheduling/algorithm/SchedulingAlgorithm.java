package com.mfu.taskscheduling.algorithm;

import com.mfu.taskscheduling.processor.Processor;
import com.mfu.taskscheduling.task.Task;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * An abstract class representing a scheduling algorithm.
 * <p>
 * It provides a framework for implementing different scheduling algorithms by defining a common interface and utility methods.
 * <p>
 * Subclasses must implement the {@code prepare} and {@code execute} method to perform the actual scheduling logic.
 */
public abstract class SchedulingAlgorithm {
    /**
     * The scheduled list of tasks.
     */
    protected List<Task> scheduledTasks;

    /**
     * Executes the scheduling algorithm on the provided list of tasks and processors.
     * First, prints the progress message showing the current state of the simulation.
     * Then, prepares the algorithm by initializing any required inner variables and setting up the initial state.
     * Next, runs the scheduling algorithm to assign tasks to processors.
     * Finally, sets the scheduled tasks, and returns the scheduled list of tasks.
     * <p>
     * This method serves as the main entry point for running a scheduling algorithm.
     * </p>
     *
     * @param tasks      the list of tasks to be scheduled
     * @param processors the list of processors available for scheduling
     * @return the scheduled list of tasks after running the scheduling algorithm
     */
    public final List<Task> run(List<Task> tasks, List<Processor> processors) {
        print(tasks, processors);
        prepare(tasks, processors);
        execute(tasks, processors);
        setScheduledTasks(tasks);
        return scheduledTasks;
    }

    /**
     * Prints a progress message to the console, showing the current state of the simulation.
     * The message includes the name of the scheduling algorithm, the number of tasks, and the number of processors.
     *
     * @param tasks      the list of tasks being scheduled
     * @param processors the list of processors used in the simulation
     */
    private void print(List<Task> tasks, List<Processor> processors) {
        System.out.println(this + " running with " + tasks.size() + " tasks on " + processors.size() + " processors");
    }

    /**
     * Prepares the scheduling algorithm by initializing any required inner variables
     * and setting up the initial state for the scheduling process.
     * This method must be implemented by each scheduling algorithm subclass.
     *
     * @param tasks      the list of tasks to be scheduled
     * @param processors the list of available processors for scheduling
     */
    protected abstract void prepare(List<Task> tasks, List<Processor> processors);

    /**
     * Schedules the tasks using the specific scheduling algorithm.
     *
     * @param tasks      the list of tasks to be scheduled
     * @param processors the list of processors available for scheduling
     */
    protected abstract void execute(List<Task> tasks, List<Processor> processors);

    /**
     * Sets the prioritized list of tasks based on their estimated start time and finish time.
     * The list is sorted in ascending order of estimated start time, and in case of ties, by estimated finish time.
     *
     * @param scheduledTasks the list of tasks to be prioritized
     */
    private void setScheduledTasks(List<Task> scheduledTasks) {
        Comparator<Task> byStartTime = Comparator
                .comparingDouble(Task::getEstimatedStartTime)
                .thenComparingDouble(Task::getEstimatedFinishTime);
        scheduledTasks.sort(byStartTime);
        this.scheduledTasks = scheduledTasks;
    }

    /**
     * Updates the scheduling lists after a task has been executed.
     * The executed task is removed from the ready tasks set and added to the executed tasks set.
     * Any child tasks that now have all their parents executed are added to the ready tasks set.
     *
     * @param readyTasks    the set of tasks that are ready to be scheduled
     * @param executedTasks the set of tasks that have been executed
     * @param executedTask  the task that has just been executed
     */
    protected void updateSchedulingList(Set<Task> readyTasks, Set<Task> executedTasks, Task executedTask) {
        readyTasks.remove(executedTask);
        executedTasks.add(executedTask);
        executedTask.getChildren().stream()
                .filter(childTask -> executedTasks.containsAll(childTask.getParents()))
                .forEach(readyTasks::add);
    }

    /**
     * Returns a string representation of this algorithm name, which is the simple name of the class.
     *
     * @return the simple name of the class as a string
     */
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
