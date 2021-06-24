package me.bazhenov.docker;

import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.TestListenerAdapter;
import org.testng.annotations.Listeners;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
		ExecutorService starter = Executors.newCachedThreadPool();

		try {
			Map<Object, Boolean> testObjects = new IdentityHashMap<>();
			for (ITestNGMethod m : testContext.getAllTestMethods()) {
				testObjects.putIfAbsent(m.getInstance(), true);
			}

			// Retrieving container preferences and creating namespaces
			testObjects.keySet().stream()
				.map(Object::getClass)
				.forEach(inspector::createNamespace);

			List<Future<?>> futures = new ArrayList<>();

			// Starting containers and waiting for ports in parallel threads
			for (ContainerNamespace namespace : inspector.getAllNamespaces()) {
				for (ContainerDefinition definition : namespace.getAllDefinitions()) {
					futures.add(starter.submit(() -> {
						try {
							String containerId = docker.start(definition);
							Map<Integer, Integer> publishedTcpPorts = docker.getPublishedTcpPorts(containerId);
							namespace.registerPublishedTcpPorts(definition, publishedTcpPorts);
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						} catch (InterruptedException e) {
							currentThread().interrupt();
						}
					}));
				}
			}

			// Waiting for all containers ready
			for (Future<?> future : futures) {
				try {
					future.get();
				} catch (ExecutionException e) {
					throw new RuntimeException(e);
				}
			}

			// Performing port identification
			for (Object test : testObjects.keySet()) {
				inspector.resolveNotificationMethod(test.getClass()).ifPresent(m -> m.call(test));
			}
		} catch (InterruptedException e) {
			currentThread().interrupt();
		} finally {
			starter.shutdown();
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
