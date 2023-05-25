package com.mfu.taskscheduling.algorithm.util;


import com.mfu.taskscheduling.task.Task;

import java.util.Set;

/**
 * A record representing the cost of a specific path in a directed acyclic graph.
 * It stores a list of tasks in the path and the corresponding cost.
 * The record implements the Comparable interface to enable sorting based on the path cost.
 */
public record PathCost(Set<Task> path, Double cost) implements Comparable<PathCost> {
    /**
     * Compares this PathCost to another PathCost based on their costs.
     * Used to determine the priority between different paths in the directed acyclic graph.
     *
     * @param pathCost the other PathCost to be compared with this PathCost
     * @return a negative integer, zero, or a positive integer if this PathCost's cost is greater than,
     * equal to, or less than the specified PathCost's cost, respectively
     */
    @Override
    public int compareTo(PathCost pathCost) {
        return pathCost.cost().compareTo(cost);
    }
}
