package com.mfu.taskscheduling.algorithm;

import com.mfu.taskscheduling.processor.Processor;
import com.mfu.taskscheduling.task.Priority;
import com.mfu.taskscheduling.task.Task;
import com.mfu.taskscheduling.util.Schedulers;

import java.util.*;

/**
 * Represents the Predict Earliest Finish Time (PEFT) scheduling algorithm.
 * The details of the algorithm can be found in the paper with DOI: <a target="blank" href="https://doi.org/10.1109/TPDS.2013.57">10.1109/TPDS.2013.57</a>.
 */
public class PEFT extends SchedulingAlgorithm {
    private final Priority priority;
    private final Map<Processor, List<Task>> schedules;
    private final Set<Task> readyTasks;
    private final Set<Task> executedTasks;

    /**
     * Creates a new instance of the PEFT scheduling algorithm.
     */
    public PEFT(Priority priority) {
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
        // allocation phase
        allocateTasks(readyTasks, executedTasks, processors);
    }

    private void setPriority(List<Task> tasks) {
        tasks.forEach(task -> task.setPriority(priority.getAverageRankValue(priority.getOptimisticCostTable(), task)));
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
        double optimisticEarliestFinishTime = Double.MAX_VALUE;
        double bestReadyTime = 0.0;
        double finishTime;

        for (Processor processor : processors) {
            double earliestStartTime = Schedulers.getEarliestStartTime(task, processor, true);
            finishTime = Schedulers.findEarliestFinishTime(schedules.get(processor), task, processor, earliestStartTime, false);
            double optimisticCost = priority.getOptimisticCostTable().get(task).get(processor);
            double currentOptimisticEarliestFinishTime = optimisticCost + finishTime;
            if (currentOptimisticEarliestFinishTime < optimisticEarliestFinishTime) {
                bestReadyTime = earliestStartTime;
                chosenProcessor = processor;
                optimisticEarliestFinishTime = currentOptimisticEarliestFinishTime;
            }
        }
        Schedulers.findEarliestFinishTime(schedules.get(chosenProcessor), task, chosenProcessor, bestReadyTime, true);
    }
}
