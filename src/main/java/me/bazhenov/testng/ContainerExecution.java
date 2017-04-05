package me.bazhenov.testng;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

@SuppressWarnings("WeakerAccess")
public class ContainerExecution {

	private final String image;
	private final List<String> command;
	private Set<Integer> exposePorts = new HashSet<>();
	private Map<String, String> environment = new HashMap<>();
	private boolean removeAfterCompletion;

	public ContainerExecution(String image, String... command) {
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
