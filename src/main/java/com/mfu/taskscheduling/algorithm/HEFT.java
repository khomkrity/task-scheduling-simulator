package com.mfu.taskscheduling.algorithm;

import com.mfu.taskscheduling.processor.Processor;
import com.mfu.taskscheduling.task.Priority;
import com.mfu.taskscheduling.task.Task;
import com.mfu.taskscheduling.util.Schedulers;

import java.util.*;

/**
 * Represents the Heterogeneous Earliest Finish Time (HEFT) scheduling algorithm.
 * The details of the algorithm can be found in the paper with DOI: <a target="blank" href="https://doi.org/10.1109/71.993206">10.1109/71.993206</a>.
 */
public class HEFT extends SchedulingAlgorithm {
    private final Priority priority;
    private final Set<Task> readyTasks;
    private final Set<Task> executedTasks;
    private final Map<Processor, List<Task>> schedules;

    /**
     * Creates a new instance of the HEFT scheduling algorithm.
     */
    public HEFT(Priority priority) {
        this.priority = priority;
        schedules = new LinkedHashMap<>();
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
        // Prioritization phase
        setPriority(tasks);
        // Selection phase
        allocateTasks(tasks, processors);
    }

    /**
     * Calculates the rank for each task in the list of tasks using the given list of processors.
     *
     * @param tasks      The list of tasks to calculate ranks
     */
    private void setPriority(List<Task> tasks) {
        tasks.forEach(task -> task.setPriority(priority.getUpwardRanks().get(task)));
    }


    /**
     * Allocates a list of tasks to a list of processors by prioritizing them based on their priority value.
     * The tasks are sorted in descending order of priority, and each task is allocated to the processor that
     * results in the earliest finish time for that task.
     *
     * @param tasks      The list of tasks to allocate
     * @param processors The list of processors to allocate the tasks
     */
    private void allocateTasks(List<Task> tasks, List<Processor> processors) {
        Comparator<Task> byPriority = Comparator.comparingDouble(Task::getPriority).reversed();
        tasks.sort(byPriority);
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

            if (finishTime < earliestFinishTime) {
                bestReadyTime = earliestStartTime;
                earliestFinishTime = finishTime;
                chosenProcessor = processor;
            }
        }

        Schedulers.findEarliestFinishTime(schedules.get(chosenProcessor), task, chosenProcessor, bestReadyTime, true);
    }
}
