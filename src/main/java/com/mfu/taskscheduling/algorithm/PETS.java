package com.mfu.taskscheduling.algorithm;

import com.mfu.taskscheduling.processor.Processor;
import com.mfu.taskscheduling.task.Task;
import com.mfu.taskscheduling.util.Costs;
import com.mfu.taskscheduling.util.Schedulers;
import org.apache.commons.math3.stat.StatUtils;

import java.util.*;

/**
 * Represents the Performance Effective Task Scheduling (PETS) algorithm.
 * This scheduling algorithm is designed for scheduling tasks on heterogeneous resources.
 * The details of the algorithm can be found in the paper with DOI: <a target="blank" href="https://doi.org/10.3844/jcssp.2007.94.103">10.3844/jcssp.2007.94.103</a>.
 */

public class PETS extends SchedulingAlgorithm {
    private final Map<Processor, List<Task>> schedules;
    private final Map<Task, Double> dataTransferCosts;
    private final Set<Task> readyTasks;
    private final Set<Task> executedTasks;

    public PETS() {
        schedules = new HashMap<>();
        dataTransferCosts = new HashMap<>();
        readyTasks = new HashSet<>();
        executedTasks = new HashSet<>();
    }

    @Override
    protected void prepare(List<Task> tasks, List<Processor> processors) {
        tasks.stream().filter(Task::isEntry).forEach(readyTasks::add);
        processors.forEach(processor -> schedules.put(processor, new ArrayList<>()));
    }

    @Override
    protected void execute(List<Task> tasks, List<Processor> processors) {
        // prioritization phase
        calculateDataTransferCost(tasks, processors);
        calculateTaskRank(tasks, processors);
        // processor selection phase
        allocateTasks(readyTasks, executedTasks, processors);
    }

    private void calculateDataTransferCost(List<Task> tasks, List<Processor> processors) {
        double averageBandwidth = StatUtils.mean(Costs.getBandwidths(processors));
        for (Task task : tasks) {
            double cumulativeDataTransferCost = 0.0;
            for (Task child : task.getChildren()) {
                cumulativeDataTransferCost += Costs.getCommunicationCost(task, child, averageBandwidth);
            }
            dataTransferCosts.put(task, cumulativeDataTransferCost);
        }
    }

    private double calculatePredecessorTaskRank(Task task) {
        double rank = 0.0;
        for (Task parent : task.getParents()) {
            rank = Math.max(rank, parent.getPriority());
        }
        return rank;
    }

    private void calculateTaskRank(List<Task> tasks, List<Processor> processors) {
        for (Task task : tasks) {
            double rank = Math.round(StatUtils.mean(Costs.getComputationCosts(task, processors))
                    + dataTransferCosts.get(task)
                    + calculatePredecessorTaskRank(task));
            task.setPriority(rank);
        }
    }


    private void allocateTasks(Set<Task> readyTasks, Set<Task> executedTasks, List<Processor> processors) {
        while (!readyTasks.isEmpty()) {
            Task maxRankTask = readyTasks.stream()
                    .max(compareTasksByPriorityAndComputationCost(processors))
                    .orElse(null);
            allocateTask(maxRankTask, processors);
            updateSchedulingList(readyTasks, executedTasks, maxRankTask);
        }
    }

    private Comparator<Task> compareTasksByPriorityAndComputationCost(List<Processor> processors) {
        return (task1, task2) -> {
            int priorityComparison = Double.compare(task2.getPriority(), task1.getPriority());
            if (priorityComparison != 0) {
                return priorityComparison;
            } else {
                double avgComputationCost1 = StatUtils.mean(Costs.getComputationCosts(task1, processors));
                double avgComputationCost2 = StatUtils.mean(Costs.getComputationCosts(task2, processors));
                return Double.compare(avgComputationCost1, avgComputationCost2);
            }
        };
    }

    /**
     * Allocates the given task to one of the available processors, choosing the processor that results in the earliest
     * finish time for the task.
     *
     * @param task       The task to allocate
     * @param processors The list of available processors to allocate the task to
     */
    private void allocateTask(Task task, List<Processor> processors) {
        Processor chosenProcessor = null;
        double earliestFinishTime = Double.MAX_VALUE;
        double bestReadyTime = 0.0;
        double finishTime;

        for (Processor processor : processors) {
            double earliestStartTime = Schedulers.getEarliestStartTime(task, processor, true);
            finishTime = Schedulers.findEarliestFinishTime(schedules.get(processor), task, processor, earliestStartTime, false);

            if (finishTime < earliestFinishTime) {
                bestReadyTime = earliestStartTime;
                earliestFinishTime = finishTime;
                chosenProcessor = processor;
            }
        }

        Schedulers.findEarliestFinishTime(schedules.get(chosenProcessor), task, chosenProcessor, bestReadyTime, true);
    }


}
