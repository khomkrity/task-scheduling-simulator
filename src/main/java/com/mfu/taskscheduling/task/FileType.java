package com.mfu.taskscheduling.task;

/**
 * An enumeration representing various file types in each task.
 */
public enum FileType {
    /**
     * Input file type in a task that usually receives from its predecessors
     */
    INPUT,
    /**
     * Output file type in a task that it usually produces
     */
    OUTPUT,
    /**
     * None of the file type
     */
    NONE
}
