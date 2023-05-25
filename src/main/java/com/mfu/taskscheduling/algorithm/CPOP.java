package com.mfu.taskscheduling.algorithm;

import com.mfu.taskscheduling.processor.Processor;
import com.mfu.taskscheduling.task.Task;
import com.mfu.taskscheduling.util.Schedulers;
import com.mfu.taskscheduling.task.Priority;

import java.util.*;

/**
 * Represents the Critical Path on a Processor (CPOP) scheduling algorithm.
 * The details of the algorithm can be found in the paper with DOI: <a target="blank" href="https://doi.org/10.1109/71.993206">10.1109/71.993206</a>.
 */
public class CPOP extends SchedulingAlgorithm {
    private final Priority priority;
    private final Map<Task, Double> priorities;
    private Task entryTask;
    private final Set<Task> criticalPath;
    private double criticalPathRank;
    private Processor criticalPathProcessor;
    private final Map<Processor, List<Task>> schedules;
    private final Set<Task> readyTasks;
    private final Set<Task> executedTasks;

    /**
     * Creates a new instance of the CPOP scheduling algorithm.
     */
    public CPOP(Priority priority) {
        this.priority = priority;
        priorities = new HashMap<>();
        criticalPath = new HashSet<>();
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
        // Prioritization phase
        setPriority(tasks);
        calculateCriticalPathRank();
        calculateCriticalPath();
        criticalPathProcessor = getCriticalPathProcessor(processors);
        // Selection phase
        allocateTasks(readyTasks, executedTasks, processors);
    }

    private Processor getCriticalPathProcessor(List<Processor> processors) {
        return processors.stream()
                .max(Comparator.comparingDouble(Processor::getMips))
                .orElse(null);
    }

    /**
     * Invokes calculateRank for each task to be scheduled
     */
    private void setPriority(List<Task> tasks) {
        for (Task task : tasks) {
            double taskPriority = priority.getUpwardRanks().get(task) + priority.getDownwardRanks().get(task);
            task.setPriority(taskPriority);
            priorities.put(task, taskPriority);
        }
    }

    private void calculateCriticalPathRank() {
        Map.Entry<Task, Double> taskEntry = priorities.entrySet().stream()
                .filter(entry -> entry.getKey().isEntry())
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .orElse(null);

        if (taskEntry != null) {
            entryTask = taskEntry.getKey();
            criticalPath.add(entryTask);
            criticalPathRank = taskEntry.getValue();
        }
    }

    private void calculateCriticalPath() {
        Task currentTask = entryTask;
        while (currentTask != null && !currentTask.isExit()) {
            Task selectedTask = null;
            for (Task child : currentTask.getChildren()) {
                double childPriority = priorities.get(child);
                if (Priority.isEqual(criticalPathRank, childPriority)) {
                    selectedTask = child;
                    criticalPath.add(selectedTask);
                    break;
                }
            }
            currentTask = selectedTask;
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

            if (criticalPath.contains(task) && chosenProcessor == criticalPathProcessor) {
                bestReadyTime = earliestStartTime;
                chosenProcessor = processor;
                break;
            }

            if (finishTime < earliestFinishTime) {
                bestReadyTime = earliestStartTime;
                earliestFinishTime = finishTime;
                chosenProcessor = processor;
            }
        }

        Schedulers.findEarliestFinishTime(schedules.get(chosenProcessor), task, chosenProcessor, bestReadyTime, true);
    }

}
