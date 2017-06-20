package me.bazhenov.docker;

import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Set of containers accessible from given test case
 */
final class ContainerNamespace {

	private Map<String, ContainerDefinition> defs;
	private List<ContainerNamespace> importNamespaces;
	private Map<ContainerDefinition, Map<Integer, Integer>> tcpPorts = new HashMap<>();

	ContainerNamespace(Map<String, ContainerDefinition> defs, List<ContainerNamespace> importNamespaces) {
		ensureUniqueNames(defs, importNamespaces);
		this.defs = requireNonNull(defs);
		this.importNamespaces = importNamespaces;
	}

	private static void ensureUniqueNames(Map<String, ContainerDefinition> defs, List<ContainerNamespace> namespaces) {
		Set<String> visitedNamed = new HashSet<>();
		visitedNamed.addAll(defs.keySet());
		for (ContainerNamespace namespace : namespaces) {
			for (String name : namespace.getDefinedNames()) {
				if (!visitedNamed.add(name)) {
					throw new IllegalArgumentException("Namespace has duplicated container with name: " + name);
				}
			}
		}
	}

	int size() {
		return defs.size() + importNamespaces.stream()
			.mapToInt(ContainerNamespace::size)
			.sum();
	}

	ContainerDefinition getDefinition(String name) {
		if (defs.containsKey(name)) {
			return defs.get(name);
		} else {
			for (ContainerNamespace namespace : importNamespaces) {
				ContainerDefinition definition = namespace.getDefinition(name);
				if (definition != null) {
					return definition;
				}
			}
		}
		return null;
	}

	Collection<ContainerDefinition> getAllDefinitions() {
		return defs.values();
	}

	Set<String> getDefinedNames() {
		return defs.keySet();
	}

	/**
	 * @param publishedTcpPorts map where keys are container ports and values are host ports
	 */
	void registerPublishedTcpPorts(ContainerDefinition definition, Map<Integer, Integer> publishedTcpPorts) {
		if (!defs.containsValue(definition)) {
			throw new IllegalArgumentException("No definition for container found in namespace: " + definition.getImage());
		}
		tcpPorts.put(definition, publishedTcpPorts);
	}

	int lookupHostPort(ContainerDefinition definition, int containerPort) {
		Integer port = doLookupHostPort(definition, containerPort);
		if (port == null) {
			throw new IllegalArgumentException("Missing container in namespace: " + definition.getImage());
		}
		return port;
	}

	private Integer doLookupHostPort(ContainerDefinition definition, int containerPort) {
		Map<Integer, Integer> ports = tcpPorts.get(definition);
		if (ports != null) {
			// Container is registered in out namespace
			Integer hostPort = ports.get(containerPort);
			if (hostPort == null) {
				throw new IllegalArgumentException("Port " + containerPort + " not registered in the container");
			}
			return hostPort;
		} else {
			// Container is probably registered in imported namespace
			for (ContainerNamespace namespace : importNamespaces) {
				Integer port = namespace.doLookupHostPort(definition, containerPort);
				if (port != null)
					return port;
			}
		}
		return null;
	}
}
