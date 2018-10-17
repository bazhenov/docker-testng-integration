package me.bazhenov.docker;

import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.TestListenerAdapter;
import org.testng.annotations.Listeners;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.IdentityHashMap;
import java.util.Map;

import static java.lang.Thread.currentThread;

/**
 * Orchestrates container initialisation and tear down for TestNG.
 * <p>
 * Typical usage consist of registering this class as a TestNG listener (see. {@link Listeners}) and then using
 * annotations {@link Container}, {@link AfterContainerStart} and {@link ContainerPort}.
 */
public class DockerTestNgListener extends TestListenerAdapter {

	private final Docker docker = new Docker();

	@Override
	public void onStart(ITestContext testContext) {
		super.onStart(testContext);

		DockerAnnotationsInspector inspector = new DockerAnnotationsInspector();

		try {
			Map<Object, Boolean> testObjects = new IdentityHashMap<>();
			for (ITestNGMethod m : testContext.getAllTestMethods()) {
				testObjects.putIfAbsent(m.getInstance(), true);
			}

			// Retrieving container preferences and creating namespaces
			testObjects.keySet().stream()
				.map(Object::getClass)
				.forEach(inspector::createNamespace);

			// Starting containers
			for (ContainerNamespace namespace : inspector.getAllNamespaces()) {
				for (ContainerDefinition definition : namespace.getAllDefinitions()) {
					String containerId = docker.start(definition);
					Map<Integer, Integer> publishedTcpPorts = docker.getPublishedTcpPorts(containerId);
					namespace.registerPublishedTcpPorts(definition, publishedTcpPorts);
				}
			}

			// Performing port identification
			for (Object test : testObjects.keySet()) {
				inspector.resolveNotificationMethod(test.getClass()).ifPresent(m -> m.call(test));
			}

		} catch (IOException e) {
			throw new UncheckedIOException(e);

		} catch (InterruptedException e) {
			currentThread().interrupt();
		}
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
