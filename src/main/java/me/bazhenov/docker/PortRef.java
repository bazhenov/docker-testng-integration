package me.bazhenov.docker;

import static java.util.Objects.requireNonNull;

final class PortRef {

	private ContainerDefinition containerDefinition;
	private int port;

	PortRef(ContainerDefinition containerDefinition, int port) {
		this.containerDefinition = requireNonNull(containerDefinition);
		if (port <= 0)
			throw new IllegalArgumentException("Port should be positive");
		this.port = port;
	}

	ContainerDefinition getContainerDefinition() {
		return containerDefinition;
	}

	int getContainerPort() {
		return port;
	}
}
