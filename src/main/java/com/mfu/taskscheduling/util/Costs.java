package com.mfu.taskscheduling.util;

import com.mfu.taskscheduling.processor.Processor;
import com.mfu.taskscheduling.task.FileItem;
import com.mfu.taskscheduling.task.FileType;
import com.mfu.taskscheduling.task.Task;
import org.apache.commons.math3.stat.StatUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Costs {
    private static final Map<Double, Map<Task, Map<Task, Double>>> communicationCosts = new HashMap<>();

    private Costs() {
        throw new IllegalStateException("Utility Class");
    }

    /**
     * Retrieves the bandwidths of the provided list of processors.
     *
     * @param processors the list of processors from which to get the bandwidths
     * @return an array of bandwidths corresponding to the given processors
     */
    public static double[] getBandwidths(List<Processor> processors) {
        double[] bandwidths = new double[processors.size()];
        for (int i = 0; i < processors.size(); i++) {
            bandwidths[i] = processors.get(i).getBandwidth();
        }
        return bandwidths;
    }

    /**
     * Calculates the computation cost of a task on a specific processor.
     *
     * @param task      the task for which the computation cost is to be calculated
     * @param processor the processor on which the task is to be executed
     * @return the computation cost of the task on the given processor
     */
    public static double getComputationCost(Task task, Processor processor) {
        return task.getLength() / processor.getMips();
    }

    /**
     * Retrieves the computation costs of a task on a list of processors.
     *
     * @param task       the task for which the computation costs are to be calculated
     * @param processors the list of processors on which the task is to be executed
     * @return an array of computation costs for the given task on the given processors
     */
    public static double[] getComputationCosts(Task task, List<Processor> processors) {
        double[] computationCosts = new double[processors.size()];
        for (int i = 0; i < processors.size(); i++) {
            computationCosts[i] = getComputationCost(task, processors.get(i));
        }
        return computationCosts;
    }

    /**
     * Calculates the communication cost between two processors for a pair of tasks (parent and current).
     * If the processors are the same, the communication cost is zero.
     *
     * @param parentTask        the parent task in the communication
     * @param currentTask       the current task in the communication
     * @param previousProcessor the processor executing the parent task
     * @param currentProcessor  the processor executing the current task
     * @return the communication cost between the two processors for the given pair of tasks
     */
    public static double getCommunicationCost(Task parentTask, Task currentTask, Processor previousProcessor, Processor currentProcessor) {
        if (previousProcessor.equals(currentProcessor)) return 0.0;
        double bandwidth = Math.min(previousProcessor.getBandwidth(), currentProcessor.getBandwidth());
        return calculateCommunicationCost(parentTask, currentTask, bandwidth);
    }

    /**
     * Calculates the communication cost between two tasks using the specified bandwidth.
     *
     * @param parentTask  the parent task in the communication
     * @param currentTask the current task in the communication
     * @param bandwidth   the bandwidth to be used for calculating the communication cost
     * @return the communication cost between the two tasks with the given bandwidth
     */
    public static double getCommunicationCost(Task parentTask, Task currentTask, double bandwidth) {
        return calculateCommunicationCost(parentTask, currentTask, bandwidth);
    }

    private static double calculateCommunicationCost(Task parentTask, Task currentTask, double bandwidth) {
        Map<Task, Map<Task, Double>> communicationCosts = getOrCreateCommunicationCostMap(bandwidth);

        if (communicationCosts.get(parentTask) != null && communicationCosts.get(parentTask).containsKey(currentTask)) {
            return communicationCosts.get(parentTask).get(currentTask);
        }

        double fileSize = calculateFileSize(parentTask, currentTask);

        // convert from byte to megabyte, then to megabits
        fileSize = (fileSize / 1_000_000.0) * 8.0;
        double communicationCost = fileSize / bandwidth;

        communicationCosts.computeIfAbsent(parentTask, parentTaskKey -> new HashMap<>()).put(currentTask, communicationCost);

        // transfer cost (second)
        return communicationCost;
    }

    private static double calculateFileSize(Task parentTask, Task currentTask) {
        Set<String> parentOutputFileNames = parentTask.getFileItems().stream()
                .filter(fileItem -> fileItem.fileType().equals(FileType.OUTPUT))
                .map(FileItem::name)
                .collect(Collectors.toSet());

        return currentTask.getFileItems().stream()
                .filter(fileItem -> fileItem.fileType().equals(FileType.INPUT) && parentOutputFileNames.contains(fileItem.name()))
                .mapToDouble(FileItem::size)
                .sum();
    }

    private static Map<Task, Map<Task, Double>> getOrCreateCommunicationCostMap(double bandwidth) {
        return communicationCosts.computeIfAbsent(bandwidth, bandwidthKey -> new HashMap<>());
    }

    /**
     * Calculates the communication to computation ratio of a given list of tasks and a list of processors.
     *
     * @param tasks      the list of tasks to calculate the ratio for
     * @param processors the list of processors to use for computation and communication costs
     * @return the communication to computation ratio
     */
    public static double getCommunicationToComputationRatio(List<Task> tasks, List<Processor> processors) {
        double totalAverageCommunicationCost = getSumAverageCommunicationCost(tasks, processors);
        double totalAverageComputationCost =  getSumAverageComputationCost(tasks, processors);
        if(totalAverageComputationCost <= 0){
            throw new ArithmeticException("Invalid computation cost: less than or equal to zero");
        }
        return totalAverageCommunicationCost / totalAverageComputationCost;
    }

    /**
     * Computes the sum of the 50th percentile computation cost of each task in a list of tasks with respect to a list of processors.
     *
     * @param tasks      the list of tasks to compute the computation cost
     * @param processors the list of processors to compute the computation cost with respect to each task
     * @return the sum of the 50th percentile computation cost of each task in the list of tasks
     */
    public static double getSumAverageComputationCost(List<Task> tasks, List<Processor> processors) {
        double sumComputationCost = 0.0;
        for (Task task : tasks) {
            sumComputationCost += StatUtils.percentile(getComputationCosts(task, processors), 50);
        }
        return sumComputationCost;
    }

    /**
     * Computes the sum of the average communication cost of each pair of parent and child tasks in a list of tasks with respect to a list of processors.
     *
     * @param tasks      the list of tasks to compute the communication cost
     * @param processors the list of processors to compute the communication cost
     * @return the sum of the average communication cost of each pair of parent and child tasks in the list of tasks
     */
    public static double getSumAverageCommunicationCost(List<Task> tasks, List<Processor> processors) {
        double sumCommunicationCost = 0.0;
        double averageBandwidth = StatUtils.percentile(getBandwidths(processors), 50);

        for (Task task : tasks) {
            for (Task child : task.getChildren()) {
                sumCommunicationCost += getCommunicationCost(task, child, averageBandwidth);
            }
        }

        return sumCommunicationCost;
    }

    public static void reset() {
        communicationCosts.clear();
    }
}
