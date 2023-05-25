package com.mfu.taskscheduling.algorithm;

import com.mfu.taskscheduling.processor.Processor;
import com.mfu.taskscheduling.task.Priority;
import com.mfu.taskscheduling.task.Task;
import com.mfu.taskscheduling.util.Schedulers;

import java.util.*;

/**
 * Represents the Predict Priority Task Scheduling (PPTS) scheduling algorithm.
 * This scheduling algorithm is designed for scheduling tasks on heterogeneous resources.
 * The details of the algorithm can be found in the paper with DOI: <a target="blank" href="https://doi.org/10.1145/3339186.3339206">10.1145/3339186.3339206</a>.
 */
public class PPTS extends SchedulingAlgorithm {
    private final Priority priority;
    private final Set<Task> readyTasks;
    private final Set<Task> executedTasks;
    private final Map<Processor, List<Task>> schedules;

    /**
     * Creates a new instance of the PPTS scheduling algorithm.
     */
    public PPTS(Priority priority) {
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
        tasks.forEach(task -> task.setPriority(priority.getAverageRankValue(priority.getPredictCostMatrix(), task)));
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
        double minLookAhead = Double.MAX_VALUE;
        double bestReadyTime = 0.0;
        double finishTime;

        for (Processor processor : processors) {
            double earliestStartTime = Schedulers.getEarliestStartTime(task, processor, true);
            finishTime = Schedulers.findEarliestFinishTime(schedules.get(processor), task, processor, earliestStartTime, false);
            double currentLookAhead = priority.getPredictCostMatrix().get(task).get(processor) + finishTime;
            if (currentLookAhead < minLookAhead) {
                minLookAhead = currentLookAhead;
                bestReadyTime = earliestStartTime;
                chosenProcessor = processor;
            }
        }
        Schedulers.findEarliestFinishTime(schedules.get(chosenProcessor), task, chosenProcessor, bestReadyTime, true);
    }
}
