package com.mfu.taskscheduling.input;

import com.mfu.taskscheduling.processor.Processor;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the environment setting for the simulation, which includes the list of processors and whether there is a
 * port constraint on the communication channels between tasks.
 */
public class EnvironmentSetting {
    private boolean hasPortConstraint;
    private boolean addPseudoTask;
    private boolean useMockData;
    private final List<List<Processor>> processors;

    /**
     * Constructs a new instance of the {@code EnvironmentSetting} class using the specified path to the XML file
     * containing the environment setting information.
     *
     * @param environmentSettingPath the path to the XML file containing the environment setting information
     * @throws IOException   if an I/O error occurs while reading the file
     * @throws JDOMException if an error occurs while parsing the XML document
     */
    public EnvironmentSetting(String environmentSettingPath) throws IOException, JDOMException {
        processors = new ArrayList<>();
        readEnvironmentSetting(environmentSettingPath);
    }

    /**
     * Reads the environment settings from the given file path and initializes the configuration properties.
     * The environment settings include the port constraint and processor specifications.
     *
     * @param environmentSettingPath the path to the environment settings file
     * @throws IOException   if there's an error reading the file
     * @throws JDOMException if there's an error parsing the XML file
     */
    private void readEnvironmentSetting(String environmentSettingPath) throws IOException, JDOMException {
        SAXBuilder saxBuilder = new SAXBuilder();
        Document document = saxBuilder.build(new File(environmentSettingPath));
        Element rootElement = document.getRootElement();
        List<Element> configElements = rootElement.getChildren();

        for (Element configElement : configElements) {
            if ("constraint".equals(configElement.getName())) {
                hasPortConstraint = "true".equals(configElement.getAttributeValue("portConstraint"));
                addPseudoTask = "true".equals(configElement.getAttributeValue("pseudoTask"));
                useMockData = "true".equals(configElement.getAttributeValue("mockData"));
            } else if ("scenario".equals(configElement.getName())) {
                List<Processor> scenarioProcessors = new ArrayList<>();
                for (Element deviceElement : configElement.getChildren()) {
                    String processorName = deviceElement.getAttributeValue("name");
                    scenarioProcessors.addAll(readProcessorSpecification(deviceElement.getChildren(), processorName));
                }
                processors.add(scenarioProcessors);
            }
        }
    }

    /**
     * Reads processor specifications from the given XML elements and creates Processor objects.
     * The processor specifications include mips, bandwidth, and cost per mips.
     *
     * @param processorElements a list of XML elements representing processor specifications
     * @param processorName     the name prefix for the processors
     * @return a list of Processor objects created based on the given processor specifications
     */
    private List<Processor> readProcessorSpecification(List<Element> processorElements, String processorName) {
        List<Processor> processors = new ArrayList<>();

        for (int i = 0; i < processorElements.size(); i++) {
            Element currentProcessorElement = processorElements.get(i);
            String name = processorName + "-" + i;
            double mips = Double.parseDouble(currentProcessorElement.getAttributeValue("mips"));
            double bandwidth = Double.parseDouble(currentProcessorElement.getAttributeValue("bandwidth"));
            double costPerMips = Double.parseDouble(currentProcessorElement.getAttributeValue("cost"));

            processors.add(new Processor(i, name, mips, bandwidth, costPerMips));
        }

        return processors;
    }

    /**
     * Returns a boolean value indicating whether there is a port constraint on the communication channels between tasks
     * in the environment setting.
     *
     * @return {@code true} if there is a port constraint, {@code false} otherwise
     */
    public boolean hasPortConstraint() {
        return hasPortConstraint;
    }

    /**
     * Returns a boolean value indicating whether to add pseudo zero-cost entry and exit tasks into the workflow
     *
     * @return {@code true} to add pseudo zero-cost tasks, {@code false} otherwise
     */
    public boolean addPseudoTask() {
        return addPseudoTask;
    }

    /**
     * Returns a boolean value indicating whether to use mock data in the workflow for test purposes
     *
     * @return {@code true} to use mock data, {@code false} otherwise
     */
    public boolean useMockData() {
        return useMockData;
    }

    /**
     * Returns a list of {@code Processor} objects representing the processors in the environment setting.
     *
     * @return a list of {@code Processor} objects representing the processors in the environment setting
     */
    public List<List<Processor>> getProcessors() {
        return processors;
    }

}

