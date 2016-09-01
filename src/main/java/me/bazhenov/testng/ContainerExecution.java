package me.bazhenov.testng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

public class ContainerExecution {

	private final String image;
	private final List<String> command;
	private boolean exposeAllPorts = false;
	private List<Integer> exposePorts = new ArrayList<>();
	private Map<String, String> environment = new HashMap<>();
	private boolean removeAfterCompletion;

	public ContainerExecution(String image, String... command) {
		this.image = requireNonNull(image);
		this.command = requireNonNull(asList(command));
	}

	public String getImage() {
		return image;
	}

	public boolean isExposeAllPorts() {
		return exposeAllPorts;
	}

	public List<Integer> getExposePorts() {
		return exposePorts;
	}

	public void setExposePorts(List<Integer> exposePorts) {
		this.exposePorts = exposePorts;
	}

	public void setExposeAllPorts(boolean exposeAllPorts) {
		this.exposeAllPorts = exposeAllPorts;
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
