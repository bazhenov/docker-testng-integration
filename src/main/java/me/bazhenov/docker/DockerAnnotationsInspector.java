package me.bazhenov.docker;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Performs inspection of test classes using {@link Container} annotations as well as {@link AfterContainerStart} and
 * {@link ContainerPort}.
 * <p>
 * All collected information is accessible through getters of this class.
 */
@SuppressWarnings("WeakerAccess")
public class DockerAnnotationsInspector {

	private Map<Class<?>, ContainerNamespace> namespaces = new HashMap<>();

	/**
	 * Creates and saves container namespace for future use.
	 * <p>
	 * All saved namespaces accessible using {@link #getAllNamespaces()}
	 *
	 * @param clazz test case class
	 * @return set of containers accessible from given test case
	 */
	public ContainerNamespace createNamespace(Class<?> clazz) {
		ContainerNamespace existingNamespace = namespaces.get(clazz);
		if (existingNamespace != null) {
			return existingNamespace;
		}

		Container[] local = clazz.getAnnotationsByType(Container.class);
		ContainersFrom imports = clazz.getAnnotation(ContainersFrom.class);
		if (local.length <= 0 && imports == null) {
			return null;
		}
		Map<String, ContainerDefinition> result = new HashMap<>();
		for (Container a : local) {
			ContainerDefinition def = createContainerDefinitionFromAnnotation(a);
			if (result.containsKey(a.name()))
				throw new IllegalStateException("Duplicating container name: " + a.name() + " for test: " + clazz);
			result.put(a.name(), def);
		}


		List<ContainerNamespace> importNamespaces = new ArrayList<>();
		if (imports != null) {
			for (Class<?> aClass : imports.value()) {
				importNamespaces.add(createNamespace(aClass));
			}
		}
		ContainerNamespace namespace = new ContainerNamespace(result, importNamespaces);
		namespaces.put(clazz, namespace);
		return namespace;
	}

	private static ContainerDefinition createContainerDefinitionFromAnnotation(Container annotation) {
		ContainerDefinition def = new ContainerDefinition(annotation.image(), annotation.command());
		fillPublishedPorts(annotation, def);
		fillEnvironmentVariables(annotation, def);
		fillCustomOptions(annotation, def);
		fillVolumes(annotation, def);
		def.setRemoveAfterCompletion(annotation.removeAfterCompletion());
		def.setWaitForAllExposedPortsToBeOpen(annotation.waitForAllExposedPorts());
		def.setNetwork(annotation.network());
		def.setNetworkAlias(annotation.networkAlias());
		if (!annotation.workingDir().isEmpty()) {
			def.setWorkingDirectory(annotation.workingDir());
		}
		return def;
	}

	private static void fillVolumes(Container annotation, ContainerDefinition def) {
		for (Volume vDef : annotation.volumes()) {
			File atHost = vDef.atHost().isEmpty()
				? null
				: new File(vDef.atHost());
			def.addVolume(new VolumeDef(vDef.value(), atHost, vDef.createDirectoryIfMissing()));
		}
	}

	private static void fillEnvironmentVariables(Container annotation, ContainerDefinition def) {
		for (String value : annotation.environment()) {
			String[] parts = value.split("=", 2);
			def.addEnvironment(parts[0], parts[1]);
		}
	}

	private static void fillPublishedPorts(Container annotation, ContainerDefinition def) {
		for (Port port : annotation.publish())
			def.addPublishedPort(port.value(), port.atHost());
	}

	private static void fillCustomOptions(Container annotation, ContainerDefinition def) {
		for (String option : annotation.options())
			def.addCustomOption(option);
	}

	public Collection<ContainerNamespace> getAllNamespaces() {
		return namespaces.values();
	}

	/**
	 * Retrieves method marked with {@link AfterContainerStart} annotations and build notification method object
	 * used to pass container ports to a test before start
	 *
	 * @param clazz test object type
	 * @return notification method definition
	 */
	public Optional<NotificationMethod> resolveNotificationMethod(Class<?> clazz) {
		ContainerNamespace namespace = namespaces.get(clazz);
		if (namespace == null) {
			return Optional.empty();
		}
		for (Method method : clazz.getMethods()) {
			if (method.getAnnotation(AfterContainerStart.class) == null)
				continue;

			ensureParameterTypes(method);
			List<PortRef> portReferences = new ArrayList<>(method.getParameterCount());
			for (Annotation[] args : method.getParameterAnnotations()) {
				ContainerPort portRef = retrieveAnnotation(args);
				if (portRef == null) {
					throw new IllegalStateException("All parameters should be marked with @ContainerPort: " + method);
				}
				portReferences.add(new PortRef(namespace.getDefinition(portRef.name()), portRef.port()));
			}
			return Optional.of(new NotificationMethod(namespace, method, portReferences));

		}
		return Optional.empty();
	}

	private ContainerPort retrieveAnnotation(Annotation[] args) {
		for (Annotation a : args) {
			if (a instanceof ContainerPort)
				return (ContainerPort) a;
		}
		return null;
	}

	private static void ensureParameterTypes(Method method) {
		for (Class<?> type : method.getParameterTypes()) {
			if (type != int.class)
				throw new IllegalStateException("All method parameters should be of type int: " + method);
		}
	}
}
