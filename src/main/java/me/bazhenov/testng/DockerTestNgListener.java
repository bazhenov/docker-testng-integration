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
					String containerId = runContainer(annotation);
					Map<Integer, Integer> publishedTcpPorts = docker.getPublishedTcpPorts(containerId);
					for (Map.Entry<Integer, Integer> port : publishedTcpPorts.entrySet()) {
						int containerPort = port.getKey();
						int hostPort = port.getValue();
						String paramKey = String.format("docker://%s:%d", annotation.name(), containerPort);
						params.put(paramKey, Integer.toString(hostPort));
					}
				}
			}

			testContext.getSuite().getXmlSuite().setParameters(params);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (InterruptedException e) {
			Thread.interrupted();
		}
	}

	private String runContainer(Container annotation) throws IOException, InterruptedException {
		ContainerExecution execution = new ContainerExecution(annotation.image(), annotation.command());
		fillExposePorts(annotation, execution);
		fillEnvironmentVariables(annotation, execution);
		execution.setRemoveAfterCompletion(annotation.removeAfterCompletion());
		return docker.start(execution);
	}

	private static void fillExposePorts(Container annotation, ContainerExecution execution) {
		Set<Integer> ports = new HashSet<>();
		for (int i : annotation.exposePorts()) {
			ports.add(i);
		}
		execution.setExposePorts(ports);
	}

	private static void fillEnvironmentVariables(Container annotation, ContainerExecution execution) {
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
