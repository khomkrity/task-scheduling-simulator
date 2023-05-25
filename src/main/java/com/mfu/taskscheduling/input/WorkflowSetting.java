package com.mfu.taskscheduling.input;

import com.mfu.taskscheduling.task.FileItem;
import com.mfu.taskscheduling.task.FileType;
import com.mfu.taskscheduling.task.Task;
import com.mfu.taskscheduling.util.Workflows;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class for reading and parsing workflow files.
 */
public class WorkflowSetting {
    private final List<String> workflowPaths;

    /**
     * Constructs a new instance of WorkflowSetting given the directory path for workflow files.
     *
     * @param workflowDirectoryPath the directory path for workflow files
     * @throws FileNotFoundException if there is no input files found in the directory
     */
    public WorkflowSetting(String workflowDirectoryPath) throws FileNotFoundException {
        workflowPaths = readWorkflowPaths(workflowDirectoryPath);
    }


    /**
     * Reads the workflow paths in the specified directory.
     *
     * @param workflowDirectoryPath the path to the directory containing the workflow files
     * @return a list of workflow file paths in the directory
     * @throws FileNotFoundException if no workflow files are found in the directory
     */
    private List<String> readWorkflowPaths(String workflowDirectoryPath) throws FileNotFoundException {
        List<String> paths = new ArrayList<>();
        File workflowDirectory = new File(workflowDirectoryPath);
        File[] workflows = workflowDirectory.listFiles();
        if (workflows == null || workflows.length == 0) {
            throw new FileNotFoundException("no input files");
        }
        for (File dagFile : workflows) {
            String path = dagFile.getPath();
            String extension = path.substring(path.lastIndexOf("."));
            if (extension.equals(".xml") || extension.equals(".dax")) {
                paths.add(path);
            }
        }
        if (paths.isEmpty())
            throw new FileNotFoundException("no input files");
        return paths;
    }

    /**
     * Parses a given workflow file and creates a list of tasks.
     *
     * @param path              the path of the workflow file to parse
     * @param hasPortConstraint a boolean indicating whether there is a port constraint in the workflow file
     * @param addPseudoTask     a boolean indicating whether to add pseudo zero-cost entry and exit tasks
     * @return a list of tasks parsed from the workflow file
     * @throws Exception if an error occurs during parsing
     */
    public List<Task> parseWorkflowFile(String path, boolean hasPortConstraint, boolean addPseudoTask, boolean useMockData) throws JDOMException, IOException {
        Map<String, Task> taskByName = new HashMap<>();
        int currentTaskId = 1;
        SAXBuilder saxBuilder = new SAXBuilder();
        Document document = saxBuilder.build(new File(path));
        Element rootElement = document.getRootElement();
        List<Element> workflowElements = rootElement.getChildren();
        for (Element element : workflowElements) {
            switch (element.getName().toLowerCase()) {
                case "job" -> {
                    String taskName = element.getAttributeValue("id");
                    taskByName.put(taskName, createTaskFromJobElement(currentTaskId, element, hasPortConstraint, useMockData));
                    currentTaskId++;
                }
                case "child" -> addTaskDependency(taskByName, element, useMockData);

            }
        }

        List<Task> tasks = new ArrayList<>(taskByName.values());

        if (addPseudoTask) {
            addPseudoZeroCostTask(tasks);
        }

        setDepths(Workflows.getRoots(tasks));

        return tasks;
    }

    /**
     * Sets the depth of each task in the list of roots using a recursive depth-first search approach.
     *
     * @param roots a List of Task objects representing the roots of the workflow to traverse
     */
    private void setDepths(List<Task> roots) {
        int startingDepth = 1;
        for (Task root : roots) {
            setDepth(root, startingDepth);
        }
    }

    /**
     * Adds pseudo zero-cost entry and exit tasks to the task list if there are multiple root or exit tasks.
     * Pseudo entry and exit tasks help establish precedence constraints and enable better parallelism detection
     * and resource allocation decisions.
     *
     * @param tasks The list of tasks in the workflow.
     */

    private void addPseudoZeroCostTask(List<Task> tasks) {
        List<Task> roots = Workflows.getRoots(tasks);
        List<Task> exits = Workflows.getExits(tasks);
        int numberOfRoots = roots.size();
        int numberOfExits = exits.size();
        if (numberOfRoots == 1 && numberOfExits == 1) return;
        double pseudoLength = 0;
        double pseudoLatency = 0;
        Task pseudoEntry = new Task(0, pseudoLength, new ArrayList<>(), pseudoLatency, pseudoLatency);
        Task pseudoExit = new Task(exits.stream().mapToInt(Task::getId).max().orElse(-100) + 1, pseudoLength, new ArrayList<>(), pseudoLatency, pseudoLatency);
        if (numberOfRoots > 1) {
            for (Task root : roots) {
                pseudoEntry.addChild(root);
                root.addParent(pseudoEntry);
            }
            tasks.add(pseudoEntry);
        }
        if (numberOfExits > 1) {
            for (Task exit : exits) {
                pseudoExit.addParent(exit);
                exit.addChild(pseudoExit);
            }
            tasks.add(pseudoExit);
        }
    }

    /**
     * Creates a new task from a job element in a workflow file.
     *
     * @param taskId            the ID of the task
     * @param element           the job element from the workflow file
     * @param hasPortConstraint whether there is a port constraint for the task
     * @return a new task object created from the job element
     */
    private Task createTaskFromJobElement(int taskId, Element element, boolean hasPortConstraint, boolean useMockData) {
        double taskLength = getTaskLength(element);
        List<FileItem> fileItems = new ArrayList<>();
        double sendingLatency = 0.0;
        double receivingLatency = 0.0;
        if (hasPortConstraint) {
            sendingLatency = Double.parseDouble(element.getAttributeValue("sending"));
            receivingLatency = Double.parseDouble(element.getAttributeValue("receiving"));
        }
        addFileItems(fileItems, element);
        return new Task(taskId, taskLength, fileItems, sendingLatency, receivingLatency);
    }

    /**
     * Gets the task length of a job element in a workflow file.
     *
     * @param element the job element from the workflow file
     * @return the task length
     */
    private double getTaskLength(Element element) {
        double cloudletLength = 0;
        String nodeRuntime = element.getAttributeValue("runtime");
        if (nodeRuntime != null) {
            cloudletLength = 1000 * Double.parseDouble(nodeRuntime);
        }
        return cloudletLength;
    }

    /**
     * Adds the file items of a job element in a workflow file into the given list.
     *
     * @param fileItems the list of input and output files
     * @param element   the job element from the workflow file
     */
    private void addFileItems(List<FileItem> fileItems, Element element) {
        List<Element> fileElements = element.getChildren();
        for (Element fileElement : fileElements) {
            if (fileElement.getName().equalsIgnoreCase("uses")) {
                FileItem fileItem = createFileItem(fileElement);
                fileItems.add(fileItem);
            }
        }
    }

    private void addMockComputationCosts() {

    }

    /**
     * Parses a file element and creates a file item object from it.
     *
     * @param fileElement the file element to be parsed
     * @return the file item object created from the file element
     */
    private FileItem createFileItem(Element fileElement) {
        String fileName = fileElement.getAttributeValue("name");
        if (fileName == null) {
            fileName = fileElement.getAttributeValue("file");
        }
        String fileLink = fileElement.getAttributeValue("link");
        double size = 0.0;
        String fileSize = fileElement.getAttributeValue("size");
        if (fileSize != null) {
            size = Double.parseDouble(fileSize);
        }
        FileType fileType = switch (fileLink) {
            case "input" -> FileType.INPUT;
            case "output" -> FileType.OUTPUT;
            default -> FileType.NONE;
        };
        return new FileItem(fileName, size, fileType);
    }

    /**
     * Adds task dependencies to a task object based on the parent task elements
     * specified in the element parameter.
     *
     * @param taskByName a map that maps task names to their corresponding task objects
     * @param element    the element that specifies the parent tasks for a child task
     */
    private void addTaskDependency(Map<String, Task> taskByName, Element element, boolean useMockData) {
        List<Element> parentTaskElements = element.getChildren();
        String childTaskName = element.getAttributeValue("ref");
        if (taskByName.containsKey(childTaskName)) {
            Task childTask = taskByName.get(childTaskName);
            for (Element parentTaskElement : parentTaskElements) {
                String parentTaskName = parentTaskElement.getAttributeValue("ref");
                if (taskByName.containsKey(parentTaskName)) {
                    Task parentTask = taskByName.get(parentTaskName);
                    parentTask.addChild(childTask);
                    childTask.addParent(parentTask);
                }
            }
        }
    }

    /**
     * Sets the depth of a task and its children recursively.
     *
     * @param task  the task whose depth is to be set
     * @param depth the depth to be set for the task
     */
    private void setDepth(Task task, int depth) {
        if (depth > task.getDepth()) {
            task.setDepth(depth);
        }
        for (Task childTask : task.getChildren()) {
            setDepth(childTask, task.getDepth() + 1);
        }
    }

    /**
     * Returns the list of workflow file paths.
     *
     * @return the list of workflow file paths
     */
    public List<String> getWorkflowPaths() {
        return workflowPaths;
    }

    /**
     * Extracts and returns the workflow name from a given workflow file path. The workflow name
     * is considered to be the substring between the last slash (either forward or backslash) and the last dot in the file path.
     *
     * @param workflowPath the path of the workflow file
     * @return the extracted workflow name
     */
    public String getWorkflowName(String workflowPath) {
        int lastBackslash = workflowPath.lastIndexOf("\\");
        int lastForwardSlash = workflowPath.lastIndexOf("/");
        int start = Math.max(lastBackslash, lastForwardSlash) + 1; // get index of the first character after the last slash
        int end = workflowPath.lastIndexOf("."); // get index of the last dot
        return workflowPath.substring(start, end); // extract the substring between the two indices
    }
}

