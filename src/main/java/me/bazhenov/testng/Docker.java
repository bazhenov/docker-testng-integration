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
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static org.slf4j.LoggerFactory.getLogger;

public final class Docker implements Closeable {

	private static final Logger log = getLogger(Docker.class);

	private final Random rnd = new Random();
	private final String pathToDocker = "docker";
	private final List<String> containersToRemove = new ArrayList<>();
	private ObjectMapper jsonReader = new ObjectMapper();
	;

	public String executeAndReturnOutput(ContainerExecution execution) throws IOException, InterruptedException {
		List<String> cmd = prepareDockerCommand(execution, "--rm");
		return doExecuteAndGetOutput(cmd);
	}

	private static String doExecuteAndGetOutput(List<String> cmd) throws IOException, InterruptedException {
		Process process = runProcess(cmd);

		int statusCode = process.waitFor();
		if (statusCode != 0) {
			throw new IOException("Process finished with exit code " + statusCode
				+ ". Output: " + readFully(process.getErrorStream()));
		}

		return readFully(process.getInputStream());
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
		if (execution.isExposeAllPorts()) {
			cmd.add("-P");
		} else if (!execution.getExposePorts().isEmpty()) {
			for (Integer port : execution.getExposePorts()) {
				cmd.add("-p");
				cmd.add(port.toString());
			}
		}

		for (Map.Entry<String, String> i : execution.getEnvironment().entrySet()) {
			cmd.add("-e");
			cmd.add(i.getKey() + "=" + i.getValue());
		}

		cmd.add(execution.getImage());
		cmd.addAll(execution.getCommand());
		return cmd;
	}

	private static String prettyFormatCommand(List<String> cmd) {
		return cmd.stream()
			.map(c -> c.contains(" ") ? "'" + c + "'" : c)
			.collect(joining(" "));
	}

	private String generateUniqName() {
		return "cont-" + rnd.nextInt(Short.MAX_VALUE);
	}

	static String readFully(InputStream stream) {
		Scanner scanner = new Scanner(stream).useDelimiter("\\A");
		return scanner.next();
	}

	public String start(ContainerExecution execution) throws IOException, InterruptedException {
		List<String> command = prepareDockerCommand(execution, "-d", "-l", "testng");

		String id = doExecuteAndGetOutput(command).trim();
		if (execution.isRemoveAfterCompletion())
			containersToRemove.add(id);
		checkContainerState(id, "running");
		return id;
	}

	private void checkContainerState(String id, String expectedState) throws IOException, InterruptedException {
		String json = doExecuteAndGetOutput(asList(pathToDocker, "inspect", id));
		JsonNode root = jsonReader.readTree(json);
		String state = root.at("/0/State/Status").asText();
		if (!expectedState.equalsIgnoreCase(state)) {
			throw new IllegalStateException("Container " + id + " failed to start. Current state: " + state);
		}
	}

	public Map<Integer, Integer> getPublishedTcpPorts(String containerName) throws IOException, InterruptedException {
		String json = doExecuteAndGetOutput(asList(pathToDocker, "inspect", containerName));
		JsonNode root = jsonReader.readTree(json);

		return doGetPublishedPorts(root);
	}

	@Override
	public void close() throws IOException {
		try {
			List<String> cmd = new ArrayList<>(asList(pathToDocker, "rm", "-f"));
			cmd.addAll(containersToRemove);
			doExecuteAndGetOutput(cmd);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (InterruptedException e) {
			Thread.interrupted();
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
}
