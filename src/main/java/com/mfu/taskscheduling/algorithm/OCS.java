package com.mfu.taskscheduling.algorithm;

import com.mfu.taskscheduling.algorithm.util.PathCost;
import com.mfu.taskscheduling.algorithm.util.Timeslot;
import com.mfu.taskscheduling.processor.Processor;
import com.mfu.taskscheduling.task.Task;
import com.mfu.taskscheduling.util.Costs;
import com.mfu.taskscheduling.util.Schedulers;
import com.mfu.taskscheduling.util.Workflows;
import org.apache.commons.math3.stat.StatUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents the One-Port Constraint Scheduling algorithm (OCS).
 * The details of the algorithm can be found in the paper with DOI: <a target="blank" href="http://dx.doi.org/10.1016/j.simpat.2016.06.010">10.1016/j.simpat.2016.06.010</a>.
 */
public class OCS extends SchedulingAlgorithm {
    private final List<Set<Task>> allPaths;
    private List<PathCost> pathCosts;
    private final NavigableSet<Double> allocatedTimeSlots;
    private final List<Timeslot> timeslots;
    private final List<Task> schedulingOrder;
    private final Map<Processor, List<Task>> schedules;

    private final Set<Task> memoizedAncestors;

    /**
     * Creates a new instance of the OCS scheduling algorithm.
     */
    public OCS() {
        timeslots = new ArrayList<>();
        allPaths = new ArrayList<>();
        pathCosts = new ArrayList<>();
        allocatedTimeSlots = new TreeSet<>();
        schedules = new LinkedHashMap<>();
        schedulingOrder = new ArrayList<>();
        memoizedAncestors = new HashSet<>();
    }

    @Override
    protected void prepare(List<Task> tasks, List<Processor> processors) {
        processors.forEach(processor -> schedules.put(processor, new ArrayList<>()));
    }

    @Override
    protected void execute(List<Task> tasks, List<Processor> processors) {
        // Prioritization phase
        findAllPaths(tasks);
        pathCosts = calculatePathCost(allPaths, processors);
        Collections.sort(pathCosts);
        calculateSchedulingTime(pathCosts, processors);

        // Selection phase
        Comparator<Task> byEstimatedStartTime = Comparator.comparingDouble(Task::getEstimatedStartTime);
        schedulingOrder.sort(byEstimatedStartTime);
        schedulingOrder.forEach(Task::reset);
        timeslots.clear();
        scheduleTasks(schedulingOrder, processors);
    }

    private void findAllPaths(List<Task> tasks) {
        Map<Task, Set<Task>> sourceToDestinations = tasks.stream()
                .filter(task -> task.getParents().isEmpty())
                .collect(Collectors.toMap(Function.identity(), this::findDestinations));

        sourceToDestinations.forEach((source, destinations) -> destinations.forEach(destination -> getAllPaths(source, destination)));
    }

    private Set<Task> findDestinations(Task task) {
        Set<Task> destinations = new HashSet<>();
        findDestinations(destinations, task);
        return destinations;
    }

    private void findDestinations(Set<Task> destinations, Task task) {
        if (task.getChildren().isEmpty()) {
            destinations.add(task);
        } else {
            task.getChildren().forEach(child -> findDestinations(destinations, child));
        }
    }

    private void getAllPaths(Task source, Task destination) {
        getAllPathsUtil(source, destination, new LinkedHashSet<>(List.of(source)));
    }

    private void getAllPathsUtil(Task source, Task destination, Set<Task> localPath) {
        if (source.equals(destination)) {
            allPaths.add(Set.copyOf(localPath));
            return;
        }

        source.getChildren().stream()
                .filter(localPath::add)
                .forEach(child -> {
                    getAllPathsUtil(child, destination, localPath);
                    localPath.remove(child);
                });
    }

    private List<PathCost> calculatePathCost(List<Set<Task>> paths, List<Processor> processors) {
        return paths.stream().map(path -> new PathCost(path, calculateCostForPath(path, processors))).collect(Collectors.toList());
    }

    private double calculateCostForPath(Set<Task> path, List<Processor> processors) {
        List<Task> tasks = path.stream().toList();
        ;
        double cost = 0;
        for (int i = 0; i < tasks.size() - 1; i++) {
            Task parent = tasks.get(i);
            Task child = tasks.get(i + 1);
            double parentAverageComputationCost = StatUtils.mean(Costs.getComputationCosts(parent, processors));
            double averageBandwidth = StatUtils.mean(Costs.getBandwidths(processors));
            double averageCommunicationCost = Costs.getCommunicationCost(parent, child, averageBandwidth);
            cost += parentAverageComputationCost + averageCommunicationCost;
            if (child.isExit()) {
                double childAverageComputationCost = StatUtils.mean(Costs.getComputationCosts(child, processors));
                cost += childAverageComputationCost;
            }
        }
        return cost;
    }

    private void calculateSchedulingTime(List<PathCost> pathCosts, List<Processor> processors) {
        for (PathCost pathCost : pathCosts) {
            Set<Task> path = pathCost.path();
            for (Task task : path) {
                List<Task> parents = task.getParents().stream().filter(parent -> !parent.isEstimated()).toList();
                if (parents.isEmpty() && !task.isEstimated()) {
                    estimateSchedulingTime(task, processors, true);
                    schedulingOrder.add(task);
                }
                findAncestors(task, processors);
            }
        }
    }

    private void findAncestors(Task task, List<Processor> processors) {
        if (task.isEstimated()) return;
        if (memoizedAncestors.contains(task)) return; // If task has already been processed, return
        memoizedAncestors.add(task); // Mark task as processed


        List<Task> parentSiblings = task.getParents()
                .stream()
                .filter(sibling -> !sibling.isEstimated())
                .collect(Collectors.toCollection(ArrayList::new));
        List<Task> siblings = task.getChildren().size() == 1 ? task.getChildren().get(0).getParents()
                .stream()
                .filter(sibling -> !sibling.isEstimated())
                .collect(Collectors.toCollection(ArrayList::new))
                : new ArrayList<>();
        if (isReadyToFindTaskPriority(parentSiblings)) {
            estimateTaskReIndexingSchedulingTime(parentSiblings, task, processors);
        } else {
            for (Task parent : chooseAncestor(task.getParents())) {
                findAncestors(parent, processors);
            }
            if (isReadyToFindTaskPriority(siblings)) {
                estimateTaskReIndexingSchedulingTime(siblings, task.getChildren().get(0), processors);
            } else if (!task.isEstimated()) {
                estimateSchedulingTime(task, processors, true);
                schedulingOrder.add(task);
            }
        }
    }

    private List<Task> chooseAncestor(List<Task> parents) {
        parents = parents.stream().filter(parent -> !parent.isEstimated()).toList();
        if (parents.size() <= 1) return parents;
        Set<Task> orderedParents = new HashSet<>();
        for (PathCost pathCost : pathCosts) {
            Set<Task> path = pathCost.path();
            for (Task parent : parents) {
                if (path.contains(parent)) {
                    orderedParents.add(parent);
                }
            }
        }
        return orderedParents.stream().toList();
    }

    private void estimateTaskReIndexingSchedulingTime(List<Task> siblings, Task child, List<Processor> processors) {
        List<List<Task>> reIndexedTaskArrangements = findReIndexedTaskArrangements(siblings, child);
        List<Task> minMakespanTaskArrangement = new ArrayList<>();
        double currentMakespan;
        double minMakespan = Double.MAX_VALUE;
        for (List<Task> taskArrangement : reIndexedTaskArrangements) {
            for (Task task : taskArrangement) {
                estimateSchedulingTime(task, processors, false);
            }
            currentMakespan = taskArrangement.stream().mapToDouble(Task::getEstimatedFinishTime).max().getAsDouble();
            if (currentMakespan < minMakespan) {
                minMakespan = currentMakespan;
                minMakespanTaskArrangement = taskArrangement;
            }
//            for (Task task : taskArrangement) {
//                allocatedTimeSlots.remove(task.getEstimatedStartTime());
//                allocatedTimeSlots.remove(task.getEstimatedFinishTime());
//            }
        }
        for (Task task : minMakespanTaskArrangement) {
            estimateSchedulingTime(task, processors, true);
            schedulingOrder.add(task);
        }
    }

    private List<List<Task>> findReIndexedTaskArrangements(List<Task> siblings, Task child) {
        Comparator<Task> descendingByTaskLength = (Task task1, Task task2) -> Double.compare(task2.getLength(), task1.getLength());
        siblings.sort(descendingByTaskLength);
        List<List<Task>> taskArrangements = new ArrayList<>();
        for (int i = 0; i < siblings.size(); i++) {
            List<Task> reIndexedArrangement = new ArrayList<>(List.of(siblings.get(i)));
            for (int j = 0; j < siblings.size(); j++) {
                if (i == j) continue;
                reIndexedArrangement.add(siblings.get(j));
            }
            reIndexedArrangement.add(child);
            taskArrangements.add(reIndexedArrangement);
        }
        return taskArrangements;
    }

    private boolean isReadyToFindTaskPriority(List<Task> tasks) {
        if (tasks.size() <= 1) return false;
        for (Task task : tasks) {
            for (Task parent : task.getParents()) {
                if (!parent.isEstimated()) return false;
            }
        }
        return true;
    }

    private void estimateSchedulingTime(Task task, List<Processor> processors, boolean allocateTimeSlot) {
        double readyTime = Double.MIN_VALUE;
        double averageComputationCost = StatUtils.mean(Costs.getComputationCosts(task, processors));
        double sendingLatency = task.getSendingLatency();
        double receivingLatency = task.getReceivingLatency();
        double startSendingTime;
        double finishSendingTime;
        double startReceivingTime;
        double finishReceivingTime;
        for (Task parent : task.getParents()) {
            readyTime = Math.max(readyTime, parent.getEstimatedFinishTime());
        }
        if (readyTime == Double.MIN_VALUE) readyTime = 0.0;
        else readyTime += getDelayFromTransferCost(task, processors);
        startSendingTime = Schedulers.avoidPortCollision(timeslots, readyTime, averageComputationCost, sendingLatency, receivingLatency);
        finishSendingTime = startSendingTime + sendingLatency;
        startReceivingTime = finishSendingTime + averageComputationCost;
        finishReceivingTime = startReceivingTime + receivingLatency;
        if (allocateTimeSlot) {
//            allocatedTimeSlots.add(readyTime);
//            allocatedTimeSlots.add(finishTime);
            timeslots.add(new Timeslot(startSendingTime, finishSendingTime));
            timeslots.add(new Timeslot(startReceivingTime, finishReceivingTime));
            task.setEstimated(true);
        }
        task.setEstimatedStartTime(startSendingTime);
        task.setEstimatedFinishTime(finishReceivingTime);
    }

    private double getDelayFromTransferCost(Task child, List<Processor> processors) {
        double latestFinishTime = 0;
        double minReadyTime = 0;
        for (Task parent : child.getParents()) {
            double readyTime = parent.getEstimatedFinishTime();
            latestFinishTime = Math.max(latestFinishTime, readyTime);
            double averageBandwidth = StatUtils.mean(Costs.getBandwidths(processors));
            double averageCommunicationCost = Costs.getCommunicationCost(parent, child, averageBandwidth);
            readyTime += averageCommunicationCost;
            minReadyTime = Math.max(minReadyTime, readyTime);
        }
        return minReadyTime - latestFinishTime;
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
        for (Task task : tasks) {
            allocateTask(task, processors);
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

    private void scheduleTasks(List<Task> schedulingOrder, List<Processor> processors) {
        Map<Processor, Double> latestVmTime = new HashMap<>();
        Map<Task, Double> earliestTaskFinishTime = new HashMap<>();
//        NavigableSet<Double> allocatedTimeSlots = new TreeSet<>();
        processors.forEach(processor -> latestVmTime.put(processor, 0.0));
        for (Task task : schedulingOrder) {
            scheduleTask(timeslots, earliestTaskFinishTime, latestVmTime, task, processors);
        }
    }

    private void scheduleTask(List<Timeslot> timeslots, Map<Task, Double> earliestTaskFinishTime, Map<Processor, Double> latestVmTime, Task task, List<Processor> processors) {
        Processor chosenProcessor = null;
        double earliestFinishTime = Double.MAX_VALUE;
        double bestReadyTime = 0.0;
        double finishTime;
        double sendingLatency = task.getSendingLatency();
        double receivingLatency = task.getReceivingLatency();
        for (Processor processor : processors) {
            double computationCost = Costs.getComputationCost(task, processor);
            double earliestReadyTime = 0.0;
            for (Task parent : task.getParents()) {
                double readyTime = earliestTaskFinishTime.get(parent);
                if (parent.getAssignedProcessor() != processor) {
                    readyTime += Costs.getCommunicationCost(parent, task, parent.getAssignedProcessor(), processor);
                }
                earliestReadyTime = Math.max(earliestReadyTime, readyTime);
            }

            earliestReadyTime = Math.max(earliestReadyTime, latestVmTime.get(processor));
            earliestReadyTime = Schedulers.avoidPortCollision(timeslots, earliestReadyTime, computationCost, sendingLatency, receivingLatency);
            finishTime = earliestReadyTime + sendingLatency + computationCost + receivingLatency;
            if (finishTime < earliestFinishTime) {
                bestReadyTime = earliestReadyTime;
                earliestFinishTime = finishTime;
                chosenProcessor = processor;
            }
        }

        earliestTaskFinishTime.put(task, earliestFinishTime);
        latestVmTime.put(chosenProcessor, Math.max(earliestFinishTime, latestVmTime.get(chosenProcessor)));
//        allocatedTimeSlots.add(bestReadyTime);
//        allocatedTimeSlots.add(earliestFinishTime);
        timeslots.add(new Timeslot(bestReadyTime, bestReadyTime + sendingLatency));
        timeslots.add(new Timeslot(earliestFinishTime - receivingLatency, earliestFinishTime));
        assert chosenProcessor != null;
        task.setAssignedProcessor(chosenProcessor);
        task.setEstimatedStartTime(bestReadyTime);
        task.setEstimatedFinishTime(earliestFinishTime);
    }
}
