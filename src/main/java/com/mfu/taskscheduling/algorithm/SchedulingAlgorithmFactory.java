package com.mfu.taskscheduling.algorithm;

import com.mfu.taskscheduling.task.Priority;

import java.util.List;

/**
 * A factory class for creating instances of SchedulingAlgorithm based on the provided SchedulingAlgorithmName enum value.
 */
public class SchedulingAlgorithmFactory {

    private SchedulingAlgorithmFactory() {
        throw new IllegalStateException("Utility Class");
    }

    /**
     * Creates an instance of the specified SchedulingAlgorithm based on the provided algorithm name.
     *
     * @param algorithmName the name of the algorithm as specified in the SchedulingAlgorithmName enum
     * @return an instance of the specified SchedulingAlgorithm
     */
    private static SchedulingAlgorithm createAlgorithm(SchedulingAlgorithmName algorithmName, Priority priority) {
        return switch (algorithmName) {
            case HEFT -> new HEFT(priority);
            case CPOP -> new CPOP(priority);
            case PETS -> new PETS();
            case PPTS -> new PPTS(priority);
            case PEFT -> new PEFT(priority);
            case IPEFT -> new IPEFT(priority);
            case IPPTS -> new IPPTS(priority);
            case HSV -> new HSV(priority);
            case OCS -> new OCS();
            case PROPOSED -> new ProposedAlgorithm(priority);
        };
    }

    /**
     * Creates instances of the specified SchedulingAlgorithm(s) based on the provided list of algorithm names.
     *
     * @param algorithmNames a list of SchedulingAlgorithmName enums representing the desired algorithms
     * @return a list of the specified SchedulingAlgorithm instances
     */
    public static List<SchedulingAlgorithm> createAlgorithms(List<SchedulingAlgorithmName> algorithmNames, Priority priority) {
        return algorithmNames.stream().map(algorithm -> createAlgorithm(algorithm, priority)).toList();
    }
}
