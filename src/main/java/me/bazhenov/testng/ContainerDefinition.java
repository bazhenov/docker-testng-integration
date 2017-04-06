package me.bazhenov.testng;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

@SuppressWarnings("WeakerAccess")
public class ContainerDefinition {

	private final String image;
	private final List<String> command;
	private Set<Integer> exposePorts = new HashSet<>();
	private Map<String, String> environment = new HashMap<>();
	private boolean removeAfterCompletion = true;
	private boolean waitForAllExpesedPortsToBeOpen = true;

	public ContainerDefinition(String image, String... command) {
		this.image = requireNonNull(image);
		this.command = requireNonNull(asList(command));
	}

	public String getImage() {
		return image;
	}

	public Set<Integer> getExposePorts() {
		return exposePorts;
	}

	public void setExposePorts(Set<Integer> exposePorts) {
		this.exposePorts = exposePorts;
	}

	public boolean isWaitForAllExpesedPortsToBeOpen() {
		return waitForAllExpesedPortsToBeOpen;
	}

	public void setWaitForAllExpesedPortsToBeOpen(boolean waitForAllExpesedPortsToBeOpen) {
		this.waitForAllExpesedPortsToBeOpen = waitForAllExpesedPortsToBeOpen;
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
}
