package com.mfu.taskscheduling.input;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowSettingTest {

    private WorkflowSetting workflowSetting;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        // Create a workflow file inside the temporary directory
        File tempWorkflowFile = tempDir.resolve("workflow.xml").toFile();
        Files.write(tempWorkflowFile.toPath(), "sample content".getBytes());
        // Initialize the workflowSetting instance
        workflowSetting = new WorkflowSetting(tempDir.toString());
        assertNotNull(workflowSetting);
    }

    @Test
    void constructorShouldCreateInstanceWithValidDirectoryPath() {
        assertNotNull(workflowSetting);
    }

    @Test
    void constructorShouldThrowFileNotFoundExceptionWithInvalidDirectoryPath() {
        String invalidDirectoryPath = "path/to/invalid/directory";
        assertThrows(FileNotFoundException.class, () -> new WorkflowSetting(invalidDirectoryPath));
    }

    @Test
    void constructorShouldThrowFileNotFoundExceptionWithNoInputFiles(@TempDir Path tempDir) {
        assertThrows(FileNotFoundException.class, () -> new WorkflowSetting(tempDir.toString()));
    }

    @Test
    void constructorShouldThrowFileNotFoundExceptionWithInvalidInputFiles(@TempDir Path tempDir) throws IOException {
        // Create an invalid format workflow file inside the temporary directory
        File tempWorkflowFile = tempDir.resolve("workflow.txt").toFile();
        Files.write(tempWorkflowFile.toPath(), "sample content".getBytes());
        assertThrows(FileNotFoundException.class, () -> new WorkflowSetting(tempDir.toString()));
    }

    @Test
    void getWorkflowNameShouldExtractWorkflowName() {
        String workflowPath = "C:\\Users\\user\\Documents\\workflows\\example_workflow.xml";
        String expectedWorkflowName = "example_workflow";
        assertEquals(expectedWorkflowName, workflowSetting.getWorkflowName(workflowPath));
    }

    @Test
    void getWorkflowNameShouldHandleSpecialCharacters() {
        String workflowPath = "C:\\Users\\user\\Documents\\workflows\\example_workflow_v2.0.xml";
        String expectedWorkflowName = "example_workflow_v2.0";
        assertEquals(expectedWorkflowName, workflowSetting.getWorkflowName(workflowPath));
    }

    @Test
    void getWorkflowNameShouldHandleForwardSlashes() {
        String workflowPath = "C:/Users/user/Documents/workflows/example_workflow.xml";
        String expectedWorkflowName = "example_workflow";
        assertEquals(expectedWorkflowName, workflowSetting.getWorkflowName(workflowPath));
    }

    @Test
    void getWorkflowNameShouldHandleFileNameOnly() {
        String workflowPath = "example_workflow.xml";
        String expectedWorkflowName = "example_workflow";
        assertEquals(expectedWorkflowName, workflowSetting.getWorkflowName(workflowPath));
    }
}