package com.mfu.taskscheduling.algorithm;

/**
 * An enumeration representing various scheduling algorithms.
 */
public enum SchedulingAlgorithmName {
    /**
     * Heterogeneous Earliest Finish Time scheduling algorithm.
     */
    HEFT,

    /**
     * Critical Path on a Processor scheduling algorithm.
     */
    CPOP,

    /**
     * Predict Priority Task Scheduling algorithm.
     */
    PPTS,

    /**
     * Performance Effective Task Scheduling algorithm.
     */
    PETS,

    /**
     * Predict Earliest Finish Time scheduling algorithm.
     */
    PEFT,

    /**
     * Heterogeneous Selection Value scheduling algorithm.
     */
    HSV,

    /**
     * Improved Predict Priority Task Scheduling algorithm.
     */
    IPPTS,

    /**
     * Improved Predict Earliest Finish Time scheduling algorithm.
     */
    IPEFT,

    /**
     * One-Port Constraint Scheduling algorithm.
     */
    OCS,

    /**
     * The proposed scheduling algorithm.
     */
    PROPOSED
}
