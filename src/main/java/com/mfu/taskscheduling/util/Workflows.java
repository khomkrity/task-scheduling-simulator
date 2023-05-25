package com.mfu.taskscheduling.util;

import com.mfu.taskscheduling.task.Priority;
import com.mfu.taskscheduling.task.Task;

import java.util.*;

/**
 * The WorkflowHelper class provides utility methods for operations related to workflow characteristics.
 */
public class Workflows {
    private Workflows() {
        throw new IllegalStateException("Utility Class");
    }

    /**
     * Returns a list of root tasks (tasks with no parents) from the given list of tasks.
     *
     * @param tasks the list of tasks to be searched for root tasks
     * @return a list of root tasks
     */
    public static List<Task> getRoots(List<Task> tasks) {
        return tasks.stream().filter(Task::isEntry).toList();
    }

    /**
     * Returns a list of exit tasks (tasks with no children) from the given list of tasks.
     *
     * @param tasks the list of tasks to be searched for exit tasks
     * @return a list of exit tasks
     */
    public static List<Task> getExits(List<Task> tasks) {
        return tasks.stream().filter(Task::isExit).toList();
    }

    /**
     * Calculates the number of edges in a given list of tasks.
     *
     * @param tasks the list of tasks to calculate the number of edges
     * @return the number of edges
     */
    public static int getTotalNumberOfEdge(List<Task> tasks) {
        return tasks.stream()
                .mapToInt(task -> task.getChildren().size())
                .sum();
    }

    /**
     * Computes the maximum width of a list of tasks. Width is the maximum number of tasks at the same depth level.
     *
     * @param tasks the list of tasks to compute the width
     * @return the maximum width of the list of tasks
     */
    public static int getWidth(List<Task> tasks) {
        Map<Integer, Integer> depthCounts = new HashMap<>();
        int maxHeight = 0;

        for (Task task : tasks) {
            int depth = task.getDepth();
            depthCounts.compute(depth, (currentDepth, width) -> width == null ? 1 : width + 1);
            maxHeight = Math.max(maxHeight, depth);
        }

        return depthCounts.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
    }

    /**
     * Computes the maximum height of a list of tasks. Height is the maximum number of levels in the workflow.
     *
     * @param tasks the list of tasks to compute the height
     * @return the maximum height of the list of tasks
     */
    public static int getHeight(List<Task> tasks) {
        return tasks.stream()
                .mapToInt(Task::getDepth)
                .max()
                .orElse(0);
    }

    /**
     * Calculates the density of a given list of tasks.
     *
     * <p>Density represents the ratio of the actual number of edges to the maximum possible number of edges in a DAG.</p>
     *
     * @param tasks the list of tasks to calculate the density
     * @return the density
     */
    public static double getDensity(List<Task> tasks) {
        int numberOfTask = tasks.size();
        return getTotalNumberOfEdge(tasks) / ((double) (numberOfTask * (numberOfTask - 1)) / 2);
    }

    /**
     * Calculates the average task degree of a given list of tasks.
     *
     * @param tasks the list of tasks to calculate the average degree
     * @return the average task degree
     */
    public static double getAverageTaskDegree(List<Task> tasks) {
        return (double) getTotalNumberOfEdge(tasks) / tasks.size();
    }

    /**
     * Calculates the critical path of a given list of tasks on a set of processors.
     *
     * @param tasks    the list of tasks to be scheduled
     * @param priority the task priority data
     * @return a list of tasks representing the critical path
     */
    public static List<Task> getCriticalPath(List<Task> tasks, Priority priority) {
        Map<Task, Double> upwardRanks = priority.getUpwardRanks();
        Map<Task, Double> downwardRanks = priority.getDownwardRanks();
        Map<Task, Double> priorities = new HashMap<>();
        for (Task task : tasks) {
            priorities.put(task, upwardRanks.get(task) + downwardRanks.get(task));
        }
        Task entryTask = tasks.stream()
                .filter(Task::isEntry)
                .max(Comparator.comparingDouble(priorities::get))
                .orElse(null);
        return findCriticalPath(priorities, entryTask);
    }

    private static List<Task> findCriticalPath(Map<Task, Double> priorities, Task entryTask) {
        List<Task> criticalPath = new ArrayList<>();
        criticalPath.add(entryTask);
        double entryTaskPriority = priorities.get(entryTask);
        Task currentTask = entryTask;
        while (currentTask != null && !currentTask.isExit()) {
            Task selectedTask = null;
            for (Task child : currentTask.getChildren()) {
                double childPriority = priorities.get(child);
                if (Priority.isEqual(entryTaskPriority, childPriority)) {
                    selectedTask = child;
                    criticalPath.add(selectedTask);
                    break;
                }
            }
            currentTask = selectedTask;
        }
        return criticalPath;
    }
}
