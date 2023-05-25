package com.mfu.taskscheduling.task;

/**
 * Represents a file used in a task.
 */
public record FileItem(String name, double size, FileType fileType) {

}
