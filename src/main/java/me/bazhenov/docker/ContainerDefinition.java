package me.bazhenov.docker;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

@SuppressWarnings("WeakerAccess")
public final class ContainerDefinition {

	private final String image;
	private final List<String> command;
	private Set<String> publishedPortsDefinition = new HashSet<>();
	private Map<String, String> environment = new HashMap<>();
	private List<String> customOptions = new LinkedList<>();
	private boolean removeAfterCompletion = true;
	private boolean waitForAllExposedPortsToBeOpen = true;
	private String workingDirectory;
	private List<String> mountPoints = new ArrayList<>();

	public ContainerDefinition(String image, String... command) {
		this.image = requireNonNull(image);
		this.command = requireNonNull(asList(command));
	}

	public String getImage() {
		return image;
	}

	public Set<String> getPublishedPortsDefinition() {
		return publishedPortsDefinition;
	}

	public void setExposePorts(Set<Integer> exposePorts) {
		this.publishedPortsDefinition = exposePorts.stream().map(Object::toString).collect(toSet());
	}

	public void setPublishedPortsDefinition(Set<String> publishedPorts) {
		this.publishedPortsDefinition = publishedPorts;
	}

	public List<String> getCustomOptions() {
		return customOptions;
	}

	public void addCustomOption(String option) {
		this.customOptions.add(option);
	}

	public boolean isWaitForAllExposedPortsToBeOpen() {
		return waitForAllExposedPortsToBeOpen;
	}

	public void setWaitForAllExposedPortsToBeOpen(boolean waitForAllExposedPortsToBeOpen) {
		this.waitForAllExposedPortsToBeOpen = waitForAllExposedPortsToBeOpen;
	}

	public List<String> getCommand() {
		return command;
	}

	public void addEnvironment(String var, String value) {
		environment.put(var, value);
	}

	public Map<String, String> getEnvironment() {
		return environment;
	}

	public void setRemoveAfterCompletion(boolean removeAfterCompletion) {
		this.removeAfterCompletion = removeAfterCompletion;
	}

	public boolean isRemoveAfterCompletion() {
		return removeAfterCompletion;
	}

	public void setWorkingDirectory(String workingDirectory) {
		this.workingDirectory = workingDirectory;
	}

	public String getWorkingDirectory() {
		return workingDirectory;
	}

	public void addVolume(String mountPoint) {
		if (mountPoint == null || mountPoint.isEmpty()) {
			throw new IllegalArgumentException();
		}
		mountPoints.add(mountPoint);
	}

	public List<String> getMountPoints() {
		return mountPoints;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ContainerDefinition that = (ContainerDefinition) o;
		return removeAfterCompletion == that.removeAfterCompletion &&
			waitForAllExposedPortsToBeOpen == that.waitForAllExposedPortsToBeOpen &&
			Objects.equals(image, that.image) &&
			Objects.equals(command, that.command) &&
			Objects.equals(publishedPortsDefinition, that.publishedPortsDefinition) &&
			Objects.equals(environment, that.environment) &&
			Objects.equals(workingDirectory, that.workingDirectory) &&
			Objects.equals(mountPoints, that.mountPoints) &&
			Objects.equals(customOptions, that.customOptions);
	}

	@Override
	public int hashCode() {
		return Objects.hash(image, command, publishedPortsDefinition, environment, removeAfterCompletion, waitForAllExposedPortsToBeOpen,
			workingDirectory, mountPoints, customOptions);
	}
}
