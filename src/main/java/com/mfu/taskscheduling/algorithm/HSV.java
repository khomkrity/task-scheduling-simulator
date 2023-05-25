package com.mfu.taskscheduling.algorithm;

import com.mfu.taskscheduling.processor.Processor;
import com.mfu.taskscheduling.task.Priority;
import com.mfu.taskscheduling.task.Task;
import com.mfu.taskscheduling.util.Costs;
import com.mfu.taskscheduling.util.Schedulers;

import java.util.*;

/**
 * Represents the Heterogeneous Selection Value (HSV) scheduling algorithm.
 * The details of the algorithm can be found in the paper with DOI: <a target="blank" href="https://doi.org/10.1016/j.jpdc.2015.04.005">10.1016/j.jpdc.2015.04.005</a>.
 */
public class HSV extends SchedulingAlgorithm {
    private final Priority priority;
    private final Set<Task> readyTasks;
    private final Set<Task> executedTasks;
    private final Map<Processor, List<Task>> schedules;

    /**
     * Creates a new instance of the HSV scheduling algorithm.
     */
    public HSV(Priority priority) {
        this.priority = priority;
        schedules = new HashMap<>();
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
        setPriority(tasks);
        // processor selection phase
        allocateTasks(readyTasks, executedTasks, processors);
    }

    private void setPriority(List<Task> tasks) {
        tasks.forEach(task -> task.setPriority(getHeterogeneousPriorityRankValue(task)));
    }

    private double getHeterogeneousPriorityRankValue(Task task) {
        return task.getChildren().size() * priority.getAverageRankValue(priority.getUpwardRankMatrix(), task);
    }

    private void allocateTasks(Set<Task> readyTasks, Set<Task> executedTasks, List<Processor> processors) {
        while (!readyTasks.isEmpty()) {
            Task maxRankTask = readyTasks.stream()
                    .max(Comparator.comparingDouble(Task::getPriority))
                    .orElse(null);
            allocateTask(maxRankTask, processors);
            updateSchedulingList(readyTasks, executedTasks, maxRankTask);
        }
    }

    private void allocateTask(Task task, List<Processor> processors) {
        Processor chosenProcessor = null;
        double minHeterogeneousSelectionValue = Double.MAX_VALUE;
        double bestReadyTime = 0.0;
        double finishTime;

        for (Processor processor : processors) {
            double earliestStartTime = Schedulers.getEarliestStartTime(task, processor, true);
            finishTime = Schedulers.findEarliestFinishTime(schedules.get(processor), task, processor, earliestStartTime, false);
            double longestDistanceExitTime = getLongestDistanceExitTime(task, processor);
            double currentHeterogeneousSelectionValue;

            currentHeterogeneousSelectionValue = finishTime * longestDistanceExitTime;
            if (currentHeterogeneousSelectionValue < minHeterogeneousSelectionValue) {
                minHeterogeneousSelectionValue = currentHeterogeneousSelectionValue;
                bestReadyTime = earliestStartTime;
                chosenProcessor = processor;
            }
        }
        Schedulers.findEarliestFinishTime(schedules.get(chosenProcessor), task, chosenProcessor, bestReadyTime, true);
    }

    private double getLongestDistanceExitTime(Task task, Processor processor) {
        return priority.getUpwardRankMatrix().get(task).get(processor) - Costs.getComputationCost(task, processor);
    }
}
