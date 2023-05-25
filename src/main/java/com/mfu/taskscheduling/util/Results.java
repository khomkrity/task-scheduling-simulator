package com.mfu.taskscheduling.util;

import com.mfu.taskscheduling.processor.Processor;
import com.mfu.taskscheduling.task.Priority;
import com.mfu.taskscheduling.task.Task;
import org.apache.commons.math3.stat.StatUtils;

import java.util.List;

public class Results {

    private Results() {
        throw new IllegalStateException("Utility Class");
    }

    /**
     * Returns the makespan of the given task list.
     * Makespan is the maximum finish time among all tasks.
     *
     * @param tasks the list of tasks to calculate the makespan
     * @return the makespan value
     * @throws IllegalArgumentException if the list of tasks is empty
     */
    public static double getMakespan(List<Task> tasks) {
        if (tasks.isEmpty()) {
            throw new IllegalArgumentException("Task list cannot be empty");
        }

        return tasks.stream()
                .mapToDouble(Task::getFinishTime)
                .max()
                .orElse(0.0);
    }

    /**
     * Calculates the speedup value for the given tasks and processors.
     * Speedup is the ratio of sequential execution time to parallel execution time (makespan).
     *
     * @param tasks      the list of tasks to calculate the speedup
     * @param processors the list of available processors
     * @return the speedup value
     * @throws IllegalArgumentException if the list of tasks is empty or the makespan is zero
     */
    public static double getSpeedup(List<Task> tasks, List<Processor> processors, double makespan) {
        if (makespan <= 0.0) {
            throw new IllegalArgumentException("Makespan cannot be zero");
        }

        return getSequentialExecutionTime(tasks, processors) / makespan;
    }

    /**
     * Calculates the sequential execution time for the given tasks and processors.
     * It computes the sum of the median execution times of all tasks across processors.
     *
     * @param tasks      the list of tasks to calculate the sequential execution time
     * @param processors the list of available processors
     * @return the sequential execution time
     */
    private static double getSequentialExecutionTime(List<Task> tasks, List<Processor> processors) {
        double percentile = 50;
        return tasks.stream()
                .mapToDouble(task -> StatUtils.percentile(Costs.getComputationCosts(task, processors), percentile))
                .sum();
    }


    /**
     * Calculates the efficiency for the given tasks and processors.
     * Efficiency is the speedup value divided by the number of processors.
     *
     * @param tasks      the list of tasks to calculate the efficiency
     * @param processors the list of available processors
     * @return the efficiency value
     * @throws IllegalArgumentException if the list of tasks is empty, the makespan is zero, or the number of processors is zero
     */
    public static double getEfficiency(List<Processor> processors, double speedup) {
        if (processors.isEmpty()) {
            throw new IllegalArgumentException("Processor list cannot be empty");
        }

        return speedup / processors.size();
    }

    /**
     * Calculates the Schedule Length Ratio (SLR) for the given tasks and processors.
     * SLR is the ratio of the makespan to the sum of all median task execution times.
     *
     * @param tasks      the list of tasks to calculate the SLR
     * @param processors the list of available processors
     * @return the SLR value
     */
    public static double getScheduleLengthRatio(List<Task> tasks, List<Processor> processors, double makespan) {
        return makespan / getSequentialExecutionTime(tasks, processors);
    }

    /**
     * Calculates the throughput of a schedule in tasks per minute.
     *
     * @param numberOfTask The total number of tasks in the schedule.
     * @param makespan     The total makespan (completion time) of the schedule.
     * @return The throughput of the schedule in tasks per minute.
     */
    public static double getThroughput(double numberOfTask, double makespan) {
        return (numberOfTask / makespan) * 60.0;
    }

    /**
     * Calculates the total running time of all processors in the given list.
     *
     * @param processors The list of processors whose total running time is to be calculated.
     * @return The sum of the running times of all processors in the list.
     */
    public static double getTotalRunningTime(List<Processor> processors) {
        return processors.stream().mapToDouble(Processor::getRunningTime).sum();
    }

    /**
     * Calculates the resource utilization of a processor as a percentage.
     *
     * @param processor        The processor whose resource utilization is to be calculated.
     * @param totalRunningTime The total totalRunningTime of all processors in the schedule.
     * @return The resource utilization of the given processor as a percentage of the totalRunningTime.
     */
    public static double getResourceUtilization(Processor processor, double totalRunningTime) {
        return (processor.getRunningTime() / totalRunningTime) * 100.0;
    }
}
