package me.bazhenov.testng;

import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DockerTestNgListener extends TestListenerAdapter {

	private final Docker docker = new Docker();

	@Override
	public void onStart(ITestContext testContext) {
		super.onStart(testContext);

		try {
			Set<Class> testClasses = new HashSet<>();
			for (ITestNGMethod m : testContext.getAllTestMethods()) {
				testClasses.add(m.getRealClass());
			}

			HashMap<String, String> params = new HashMap<>();

			for (Class c : testClasses) {
				Container[] annotations = (Container[]) c.getDeclaredAnnotationsByType(Container.class);
				for (Container annotation : annotations) {
					ContainerDefinition def = createContainerDefinitionFromAnnotation(annotation);
					String containerId = docker.start(def);

					params.putAll(retrievePortMappingParams(annotation, containerId));
				}
			}

			testContext.getSuite().getXmlSuite().setParameters(params);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (InterruptedException e) {
			Thread.interrupted();
		}
	}

	private Map<String, String> retrievePortMappingParams(Container annotation, String containerId)
		throws IOException, InterruptedException {

		Map<String, String> params = new HashMap<>();
		Map<Integer, Integer> publishedTcpPorts = docker.getPublishedTcpPorts(containerId);
		for (Map.Entry<Integer, Integer> port : publishedTcpPorts.entrySet()) {
			int containerPort = port.getKey();
			int hostPort = port.getValue();
			String paramKey = String.format("%s:%d", annotation.name(), containerPort);
			params.put(paramKey, Integer.toString(hostPort));
		}
		return params;
	}

	private static ContainerDefinition createContainerDefinitionFromAnnotation(Container annotation) {
		ContainerDefinition def = new ContainerDefinition(annotation.image(), annotation.command());
		fillExposePorts(annotation, def);
		fillEnvironmentVariables(annotation, def);
		def.setRemoveAfterCompletion(annotation.removeAfterCompletion());
		if (!annotation.workingDir().isEmpty()) {
			def.setWorkingDirectory(annotation.workingDir());
		}
		return def;
	}

	private static void fillExposePorts(Container annotation, ContainerDefinition execution) {
		Set<Integer> ports = new HashSet<>();
		for (int i : annotation.exposePorts()) {
			ports.add(i);
		}
		execution.setExposePorts(ports);
	}

	private static void fillEnvironmentVariables(Container annotation, ContainerDefinition execution) {
		for (String value : annotation.environment()) {
			String[] parts = value.split("=", 2);
			execution.addEnvironment(parts[0], parts[1]);
		}
	}

	@Override
	public void onTestStart(ITestResult result) {
		result.setParameters(new Object[]{33});
		result.setAttribute("docker.port", 34);
	}

	@Override
	public void onFinish(ITestContext testContext) {
		super.onFinish(testContext);
		try {
			docker.close();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
