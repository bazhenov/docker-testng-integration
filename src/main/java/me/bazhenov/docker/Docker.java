package me.bazhenov.docker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;

import java.io.*;
import java.util.*;

import static java.io.File.createTempFile;
import static java.lang.Integer.parseInt;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static java.nio.file.Files.readAllLines;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.ConcurrentHashMap.newKeySet;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.joining;
import static org.slf4j.LoggerFactory.getLogger;
import static org.testng.util.Strings.isNullOrEmpty;

/**
 * This class provides Docker container facility.
 * <p>
 * Two main methods are:
 * <ul>
 * <li>{@link #executeAndReturnOutput(ContainerDefinition)};</li>
 * <li>{@link #start(ContainerDefinition)}.</li>
 * </ul>
 */
@SuppressWarnings("WeakerAccess")
public final class Docker implements Closeable {

	private static final Logger log = getLogger(Docker.class);
	private static final ObjectMapper jsonReader = new ObjectMapper();

	List<String> tcpFiles = asList("/proc/self/net/tcp", "/proc/self/net/tcp6");

	private final String pathToDocker;
	private final Set<String> containersToRemove = newKeySet();
	private final Set<String> networks = newKeySet();

	public Docker(String pathToDocker) {
		this.pathToDocker = requireNonNull(pathToDocker);
	}

	public Docker() {
		this("docker");
	}

	/**
	 * Runs given container wait for it to finish and then return its stdout.
	 * <p>
	 * Be careful to use this method when large output is generated by a container. This method is fully buffered, so
	 * OOM can possibly be generated.
	 *
	 * @param definition definition of a container
	 * @return stdout of a container
	 * @throws IOException          in case when container finished with non-zero exit code or any other problem when
	 *                              starting container
	 * @throws InterruptedException when thread was interrupted
	 */
	public String executeAndReturnOutput(ContainerDefinition definition) throws IOException, InterruptedException {
		List<String> cmd = prepareDockerCommand(definition);
		return doExecuteAndGetFullOutput(cmd);
	}

	/**
	 * Starts a container in background-mode
	 *
	 * @param definition container definition
	 * @return container id
	 * @throws IOException          if there is error while starting container
	 * @throws InterruptedException when thread was interrupted
	 */
	public String start(ContainerDefinition definition) throws IOException, InterruptedException {
		ensureImageAvailable(definition.getImage());
		createNetwork(definition.getNetwork());
		File cidFile = createTempFile("docker", "cid");
		cidFile.deleteOnExit();
		// Docker requires cid-file to be not present at the moment of starting a container
		if (!cidFile.delete()) {
			throw new IllegalStateException("Docker requires cid-file to be not present at the moment of starting a container");
		}

		List<String> cmd = prepareDockerCommand(definition, "--cidfile", cidFile.getAbsolutePath());

		Process process = runProcess(cmd);
		try {
			String cid = waitForCid(process, cidFile);

			if (definition.isRemoveAfterCompletion()) {
				containersToRemove.add(cid);
			}

			waitForContainerRun(cid, process);

			if (shouldWaitForOpenPorts(definition))
				waitForPorts(cid, definition.getPublishedPorts().keySet());

			return cid;
		} catch (IOException e) {
			throw new UncheckedIOException("Unable to start container\n" +
				"Container stderr: " + readFully(process.getErrorStream()), e);
		}
	}

	private void createNetwork(String network) throws IOException, InterruptedException {
		synchronized (networks) {
			if (!isNullOrEmpty(network) && networks.add(network)) {
				docker("network", "create", network);
			}
		}
	}

	private void waitForContainerRun(String cid, Process process) throws IOException, InterruptedException {
		do {
			String state = getContainerState(cid);
			if ("running".equalsIgnoreCase(state)) {
				return; // container is running, waiting is over
			} else if ("created".equalsIgnoreCase(state)) {
				sleep(100); // container may start not immediately. let's wait some time
			} else if (!process.isAlive() && process.exitValue() != 0) {
				throw new IllegalStateException("Unable to start container " + cid + "\n" +
					"Exit code: " + process.exitValue() + "\n" +
					"Stderr: " + readFully(process.getErrorStream()));
			} else {
				throw new IllegalStateException("Unable to start container " + cid + " current state is " + state);
			}
		} while (true);
	}

	private static boolean shouldWaitForOpenPorts(ContainerDefinition definition) {
		return !definition.getPublishedPorts().isEmpty() && definition.isWaitForAllExposedPortsToBeOpen();
	}

	private void ensureImageAvailable(String image) throws IOException, InterruptedException {
		int exitValue = doExecute(asList(pathToDocker, "image", "inspect", image), new HashSet<>(asList(0, 1))).exitCode;
		if (exitValue == 1) {
			log.warn("Image {} is not found locally. It will take some time to download it.", image);
		}
	}

	private String waitForCid(Process process, File cidFile) throws InterruptedException, IOException {
		do {
			if (cidFile.isFile() && cidFile.length() > 0) {
				return readAllLines(cidFile.toPath()).get(0);
			} else if (!process.isAlive() && process.exitValue() != 0) {
				throw new IllegalStateException("Unable to start Docker container.\n" +
					"Exit code: " + process.exitValue() + "\n" +
					"Stderr: " + readFully(process.getErrorStream()));
			}
			sleep(100);
		} while (true);
	}

	private static String doExecuteAndGetFullOutput(List<String> cmd) throws IOException, InterruptedException {
		return doExecute(cmd, singleton(0)).standardOutput;
	}

	private static class ExecutionResult {
		int exitCode;
		String standardOutput;
		String errorOutput;

		public ExecutionResult(int exitCode, String processOutput, String processError) {
			this.exitCode = exitCode;
			this.standardOutput = processOutput;
			this.errorOutput = processError;
		}
	}

	private static ExecutionResult doExecute(List<String> cmd, Set<Integer> expectedExitCodes)
		throws IOException, InterruptedException {

		Process process = runProcess(cmd);

		String processOutput = readFully(process.getInputStream());
		String processError = readFully(process.getErrorStream());
		int exitCode = process.waitFor();
		if (!expectedExitCodes.contains(exitCode)) {
			throw new IOException("Unable to execute: " + String.join(" ", cmd) + "\n" +
				"Exit code: " + exitCode + "\n" +
				"Stderr: " + processError + "\n" +
				"Stdout: " + processOutput);
		}

		return new ExecutionResult(exitCode, processOutput, processError);
	}

	private static Process runProcess(List<String> cmd) throws IOException {
		if (log.isDebugEnabled()) {
			log.debug("Executing: {}", prettyFormatCommand(cmd));
		}
		ProcessBuilder builder = new ProcessBuilder(cmd);
		return builder.start();
	}

	private List<String> prepareDockerCommand(ContainerDefinition def, String... additionalOpts) {
		List<String> cmd = new ArrayList<>();
		cmd.add(pathToDocker);

		cmd.add("run");

		cmd.add("-l");
		cmd.add("docker");

		if (additionalOpts.length > 0) {
			cmd.addAll(asList(additionalOpts));
		}

		def.getPublishedPorts().forEach((key, value) -> {
			cmd.add("-p");
			cmd.add(value > 0 ? value + ":" + key : String.valueOf(key));
		});

		// Mounting volumes
		for (VolumeDef volume : def.getVolumes()) {
			File location = volume.getLocation();
			String mountPoint = volume.getMountPoint();
			if (location == null) {
				cmd.add("-v");
				cmd.add(mountPoint);
			} else {
				ensureVolumeCanBeMounted(volume, location);
				cmd.add("-v");
				cmd.add(location.getAbsolutePath() + ":" + mountPoint);
			}
		}

		for (Map.Entry<String, String> i : def.getEnvironment().entrySet()) {
			cmd.add("-e");
			cmd.add(i.getKey() + "=" + i.getValue());
		}

		if (def.isRemoveAfterCompletion())
			cmd.add("--rm");

		if (def.getWorkingDirectory() != null) {
			cmd.add("-w");
			cmd.add(def.getWorkingDirectory());
		}

		String network = def.getNetwork();
		if (!isNullOrEmpty(network)) {
			cmd.add("--network=" + network);
		}

		String netAlias = def.getNetworkAlias();
		if (!isNullOrEmpty(netAlias)) {
			cmd.add("--network-alias=" + netAlias);
		}

		cmd.addAll(def.getCustomOptions());

		cmd.add(def.getImage());
		cmd.addAll(def.getCommand());
		return cmd;
	}

	private static void ensureVolumeCanBeMounted(VolumeDef volume, File location) {
		if (!location.exists()) {
			if (volume.isCreateDirectoryIfMissing()) {
				if (!location.mkdirs()) {
					throw new IllegalStateException("Unable to create directory: " + location);
				}
			} else {
				throw new IllegalStateException("No file directory at: " + location);
			}
		}
	}

	private static String prettyFormatCommand(List<String> cmd) {
		return cmd.stream()
			.map(c -> c.contains(" ") ? "'" + c + "'" : c)
			.collect(joining(" "));
	}

	static String readFully(InputStream stream) {
		Scanner scanner = new Scanner(stream).useDelimiter("\\A");
		return scanner.hasNext()
			? scanner.next()
			: "";
	}

	/**
	 * Waits for given ports to be open in a container.
	 * <p>
	 * Only TCP ports are monitored at the moment using /proc/self/net/tcp
	 *
	 * @param cid   container to monitor
	 * @param ports ports to wait for
	 */
	private void waitForPorts(String cid, Set<Integer> ports) throws IOException, InterruptedException {
		Thread self = currentThread();
		long start = currentTimeMillis();
		boolean reported = false;
		while (!self.isInterrupted()) {
			Set<Integer> openPorts = new HashSet<>();
			for (String file : tcpFiles) {
				openPorts.addAll(readListenPorts(docker("exec", cid, "sh", "-c", "[ ! -f " + file + " ] || cat " + file)));
			}
			if (openPorts.containsAll(ports))
				return;

			checkContainerRunning(cid);

			if (!reported && currentTimeMillis() - start > 5000) {
				reported = true;
				log.warn("Waiting for ports {} to open in container {}", ports, cid);
			}

			MILLISECONDS.sleep(200);
		}
	}

	private String docker(String command, String... args) throws IOException, InterruptedException {
		List<String> cmd = new ArrayList<>(args.length + 2);
		cmd.add(pathToDocker);
		cmd.add(command);
		cmd.addAll(asList(args));
		return doExecuteAndGetFullOutput(cmd);
	}

	private void checkContainerRunning(String id) throws IOException, InterruptedException {
		String state = getContainerState(id);
		if (!"running".equalsIgnoreCase(state)) {
			throw new IllegalStateException("Container " + id + " failed to start. Current state: " + state);
		}
	}

	private String getContainerState(String id) throws IOException, InterruptedException {
		String json = docker("inspect", id);
		JsonNode root = jsonReader.readTree(json);
		return root.at("/0/State/Status").asText();
	}

	/**
	 * @param containerName container name or id
	 * @return Map where keys are container ports and values are host ports
	 * @throws IOException          if there is error while docker inspecting
	 * @throws InterruptedException when thread was interrupted
	 */
	public Map<Integer, Integer> getPublishedTcpPorts(String containerName) throws IOException, InterruptedException {
		String json = docker("inspect", containerName);
		JsonNode root = jsonReader.readTree(json);

		return doGetPublishedPorts(root);
	}

	@Override
	public void close() throws IOException {
		if (!containersToRemove.isEmpty()) {
			try {
				List<String> cmd = new ArrayList<>(asList(pathToDocker, "rm", "-f", "-v"));
				cmd.addAll(containersToRemove);
				doExecute(cmd, singleton(0));
				containersToRemove.clear();

			} catch (InterruptedException e) {
				currentThread().interrupt();
			}
		}

		synchronized (networks) {
			if (!networks.isEmpty()) {
				try {
					for (String network : networks) {
						docker("network", "rm", network);
					}
					networks.clear();
				} catch (InterruptedException e) {
					currentThread().interrupt();
				}
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
			scanner.next();
			int localPort = scanner.nextInt();
			result.add(localPort);
			scanner.nextLine();
		}
		return result;
	}


	/**
	 * Used for testing purposes only
	 *
	 * @return the number of volumes registered in docker
	 */
	int getVolumesCount() throws IOException, InterruptedException {
		List<String> cmd = new ArrayList<>();
		cmd.add(pathToDocker);
		cmd.add("volume");
		cmd.add("ls");
		cmd.add("-q");
		String out = doExecuteAndGetFullOutput(cmd);
		String[] parts = out.trim().split("\n");
		return parts.length;
	}
}
