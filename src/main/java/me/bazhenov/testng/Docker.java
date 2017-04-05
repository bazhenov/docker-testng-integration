package me.bazhenov.testng;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.*;

import static java.lang.Integer.parseInt;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.joining;
import static org.slf4j.LoggerFactory.getLogger;

@SuppressWarnings("WeakerAccess")
public final class Docker implements Closeable {

	private static final Logger log = getLogger(Docker.class);

	private final String pathToDocker = "docker";
	private final Set<String> containersToRemove = new HashSet<>();
	private ObjectMapper jsonReader = new ObjectMapper();

	public String executeAndReturnOutput(ContainerExecution execution) throws IOException, InterruptedException {
		List<String> cmd = prepareDockerCommand(execution, "--rm", "-l", "testng");
		return doExecuteAndGetFullOutput(cmd);
	}

	public String start(ContainerExecution execution) throws IOException, InterruptedException {
		List<String> cmd = prepareDockerCommand(execution, "-d", "-l", "testng");

		String id = doExecuteAndGetFullOutput(cmd).trim();
		if (execution.isRemoveAfterCompletion())
			containersToRemove.add(id);
		checkContainerState(id, "running");

		Set<Integer> exposePorts = execution.getExposePorts();
		if (!exposePorts.isEmpty())
			waitForPorts(id, exposePorts);

		return id;
	}

	private static String doExecuteAndGetFullOutput(List<String> cmd) throws IOException, InterruptedException {
		return readFully(doExecute(cmd).getInputStream());
	}

	private static Process doExecute(List<String> cmd) throws IOException, InterruptedException {
		Process process = runProcess(cmd);

		int statusCode = process.waitFor();
		if (statusCode != 0) {
			throw new IOException("Process finished with exit code " + statusCode
				+ ". Output: " + readFully(process.getErrorStream()));
		}

		return process;
	}

	private static Process runProcess(List<String> cmd) throws IOException {
		if (log.isDebugEnabled()) {
			log.debug("Executing: {}", prettyFormatCommand(cmd));
		}
		ProcessBuilder builder = new ProcessBuilder(cmd);
		return builder.start();
	}

	private List<String> prepareDockerCommand(ContainerExecution execution, String... additionalOpts) {
		List<String> cmd = new ArrayList<>();
		cmd.add(pathToDocker);
		cmd.add("run");
		if (additionalOpts.length > 0) {
			cmd.addAll(asList(additionalOpts));
		}
		if (!execution.getExposePorts().isEmpty()) {
			for (Integer port : execution.getExposePorts()) {
				cmd.add("-p");
				cmd.add(port.toString());
			}
		}

		for (Map.Entry<String, String> i : execution.getEnvironment().entrySet()) {
			cmd.add("-e");
			cmd.add(i.getKey() + "=" + i.getValue());
		}

		if (execution.isRemoveAfterCompletion())
			cmd.add("--rm");

		cmd.add(execution.getImage());
		cmd.addAll(execution.getCommand());
		return cmd;
	}

	private static String prettyFormatCommand(List<String> cmd) {
		return cmd.stream()
			.map(c -> c.contains(" ") ? "'" + c + "'" : c)
			.collect(joining(" "));
	}

	static String readFully(InputStream stream) {
		Scanner scanner = new Scanner(stream).useDelimiter("\\A");
		return scanner.next();
	}

	private void waitForPorts(String containerId, Set<Integer> ports) throws IOException, InterruptedException {
		Thread self = currentThread();
		long start = currentTimeMillis();
		boolean reported = false;
		while (!self.isInterrupted()) {
			String output = doExecuteAndGetFullOutput(asList(pathToDocker, "exec", containerId, "cat", "/proc/self/net/tcp"));
			Set<Integer> openPorts = readListenPorts(output);
			if (openPorts.containsAll(ports))
				return;

			if (!reported && currentTimeMillis() - start > 5000) {
				reported = true;
				log.warn("Waiting for ports {} to open in container {}", ports, containerId);
			}

			MILLISECONDS.sleep(200);
		}
	}

	private void checkContainerState(String id, String expectedState) throws IOException, InterruptedException {
		String json = doExecuteAndGetFullOutput(asList(pathToDocker, "inspect", id));
		JsonNode root = jsonReader.readTree(json);
		String state = root.at("/0/State/Status").asText();
		if (!expectedState.equalsIgnoreCase(state)) {
			throw new IllegalStateException("Container " + id + " failed to start. Current state: " + state);
		}
	}

	public Map<Integer, Integer> getPublishedTcpPorts(String containerName) throws IOException, InterruptedException {
		String json = doExecuteAndGetFullOutput(asList(pathToDocker, "inspect", containerName));
		JsonNode root = jsonReader.readTree(json);

		return doGetPublishedPorts(root);
	}

	@Override
	public void close() throws IOException {
		if (!containersToRemove.isEmpty()) {
			try {
				List<String> cmd = new ArrayList<>(asList(pathToDocker, "rm", "-f"));
				cmd.addAll(containersToRemove);
				doExecute(cmd);

			} catch (IOException e) {
				throw new UncheckedIOException(e);

			} catch (InterruptedException e) {
				Thread.interrupted();
			}
		}
	}

	static Map<Integer, Integer> doGetPublishedPorts(JsonNode root) {
		JsonNode candidate = root.at("/0/NetworkSettings/Ports");
		if (candidate.isMissingNode() || candidate.isNull())
			return Collections.emptyMap();
		ObjectNode ports = (ObjectNode) candidate;
		Iterator<String> names = ports.fieldNames();
		Map<Integer, Integer> pts = new HashMap<>();
		while (names.hasNext()) {
			String field = names.next();
			if (field.matches("\\d+/tcp")) {
				String[] parts = field.split("/", 2);
				int containerPort = parseInt(parts[0]);
				int localPort = ports.at("/" + field.replace("/", "~1") + "/0/HostPort").asInt();
				pts.put(containerPort, localPort);
			}
		}

		return pts;
	}

	public static Set<Integer> readListenPorts(String output) {
		Scanner scanner = new Scanner(output);
		scanner.useRadix(16).useDelimiter("[\\s:]+");
		Set<Integer> result = new HashSet<>();
		if (scanner.hasNextLine())
			scanner.nextLine();

		while (scanner.hasNextLine()) {
			scanner.nextInt();
			scanner.nextInt();
			int localPort = scanner.nextInt();
			result.add(localPort);
			scanner.nextLine();
		}
		return result;
	}
}
