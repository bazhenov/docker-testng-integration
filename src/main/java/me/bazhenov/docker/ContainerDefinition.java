package me.bazhenov.docker;

import java.io.File;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

@SuppressWarnings("WeakerAccess")
public final class ContainerDefinition {

	private final String image;
	private final List<String> command;
	private final Map<Integer, Integer> publishedPorts = new HashMap<>();
	private final Map<String, String> environment = new HashMap<>();
	private final List<String> customOptions = new LinkedList<>();
	private boolean removeAfterCompletion = true;
	private boolean waitForAllExposedPortsToBeOpen = true;
	private String workingDirectory;
	private String network, networkAlias;
	private final Collection<VolumeDef> volumes = new ArrayList<>();

	public ContainerDefinition(String image, String... command) {
		this.image = requireNonNull(image);
		this.command = requireNonNull(asList(command));
	}

	public String getImage() {
		return image;
	}

	public Map<Integer, Integer> getPublishedPorts() {
		return publishedPorts;
	}

	/**
	 * @param port container port which should be published to random host port
	 */
	public void addPublishedPort(int port) {
		addPublishedPort(port, 0);
	}

	/**
	 * @param atContainer container port which should be published
	 * @param atHost      host port which should be mapped on container port. if atHost &lt;= 0 port will be random
	 */
	public void addPublishedPort(int atContainer, int atHost) {
		publishedPorts.put(atContainer, atHost);
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
		addVolume(mountPoint, null);
	}

	public void addVolume(String mountPoint, String locationAtHost) {
		File location = locationAtHost == null
			? null
			: new File(locationAtHost);
		addVolume(new VolumeDef(mountPoint, location));
	}

	public void addVolume(VolumeDef volume) {
		requireNonNull(volume);
		volumes.add(volume);
	}

	public Collection<VolumeDef> getVolumes() {
		return volumes;
	}

	public void setNetwork(String network) {
		this.network = network;
	}

	public String getNetwork() {
		return network;
	}

	public void setNetworkAlias(String networkAlias) {
		this.networkAlias = networkAlias;
	}

	public String getNetworkAlias() {
		return networkAlias;
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
			Objects.equals(publishedPorts, that.publishedPorts) &&
			Objects.equals(environment, that.environment) &&
			Objects.equals(workingDirectory, that.workingDirectory) &&
			Objects.equals(volumes, that.volumes) &&
			Objects.equals(customOptions, that.customOptions);
	}

	@Override
	public int hashCode() {
		return Objects.hash(image, command, publishedPorts, environment, removeAfterCompletion, waitForAllExposedPortsToBeOpen,
			workingDirectory, volumes, customOptions);
	}
}
