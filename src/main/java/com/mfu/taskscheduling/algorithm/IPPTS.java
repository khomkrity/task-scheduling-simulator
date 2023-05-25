package com.mfu.taskscheduling.algorithm;

import com.mfu.taskscheduling.processor.Processor;
import com.mfu.taskscheduling.task.Priority;
import com.mfu.taskscheduling.task.Task;
import com.mfu.taskscheduling.util.Costs;
import com.mfu.taskscheduling.util.Schedulers;

import java.util.*;

/**
 * Represents the Improved Predict Priority Task Scheduling algorithm (IPPTS).
 * The details of the algorithm can be found in the paper with DOI: <a target="blank" href="https://doi.org/10.1109/TPDS.2020.3041829">10.1109/TPDS.2020.3041829</a>.
 */
public class IPPTS extends SchedulingAlgorithm {
    private final Priority priority;
    private final Set<Task> readyTasks;
    private final Set<Task> executedTasks;
    private final Map<Processor, List<Task>> schedules;

    /**
     * Creates a new instance of the IPPTS scheduling algorithm.
     */
    public IPPTS(Priority priority) {
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
    public void execute(List<Task> tasks, List<Processor> processors) {
        // prioritization phase
        setPriority(tasks);
        // processor selection phase
        allocateTasks(readyTasks, executedTasks, processors);
    }

    private void setPriority(List<Task> tasks) {
        for (Task task : tasks) {
            double averagePredictCost = priority.getAverageRankValue(priority.getPredictCostMatrix(), task);
            double outDegree = task.getChildren().size();
            task.setPriority(averagePredictCost * outDegree);
        }
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
        double lookingAheadEarliestFinishTime = Double.MAX_VALUE;
        double bestReadyTime = 0.0;
        double finishTime;

        for (Processor processor : processors) {
            double earliestStartTime = Schedulers.getEarliestStartTime(task, processor, true);
            finishTime = Schedulers.findEarliestFinishTime(schedules.get(processor), task, processor, earliestStartTime, false);
            double lookingHeadExitTime = getLookingHeadExitTime(task, processor);
            double currentLookingAheadEarliestFinishTime = finishTime + lookingHeadExitTime;
            if (currentLookingAheadEarliestFinishTime < lookingAheadEarliestFinishTime) {
                bestReadyTime = earliestStartTime;
                chosenProcessor = processor;
                lookingAheadEarliestFinishTime = currentLookingAheadEarliestFinishTime;
            }
        }
        Schedulers.findEarliestFinishTime(schedules.get(chosenProcessor), task, chosenProcessor, bestReadyTime, true);
    }

    private double getLookingHeadExitTime(Task task, Processor processor) {
        return priority.getPredictCostMatrix().get(task).get(processor) - Costs.getComputationCost(task, processor);
    }
}
