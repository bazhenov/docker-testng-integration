package me.bazhenov.testng;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.Collections.singleton;
import static me.bazhenov.testng.Docker.readListenPorts;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class DockerTest {

	private final Pattern unixDatePattern = Pattern.compile("[A-z]{3}\\s+[A-z]{3}\\s+[0-9]{1,2}\\s+" +
		"[0-9]{2}:[0-9]{2}:[0-9]{2}\\s+[A-Z]{3,4}\\s+[0-9]{4}");
	private Docker docker;

	@BeforeMethod
	public void setUp() {
		docker = new Docker();
	}

	@AfterMethod
	public void tearDown() throws IOException {
		docker.close();
	}

	@Test
	public void runSimpleContainer() throws IOException, InterruptedException, ParseException {
		ContainerDefinition execution = new ContainerDefinition("alpine", "date");

		String output = docker.executeAndReturnOutput(execution).trim();
		assertThat(unixDatePattern.matcher(output).matches(), is(true));
	}

	@Test
	public void runWithEnvVariables() throws IOException, InterruptedException, ParseException {
		ContainerDefinition execution = new ContainerDefinition("alpine", "sh", "-c", "echo $VAR");
		execution.addEnvironment("VAR", "value");

		String output = docker.executeAndReturnOutput(execution).trim();
		assertThat(output, equalTo("value"));
	}

	@Test
	public void executeDemonizedContainer() throws IOException, InterruptedException {
		ContainerDefinition execution = new ContainerDefinition("alpine", "nc", "-lp", "1234", "-s", "0.0.0.0");
		execution.setExposePorts(singleton(1234));

		String containerName = docker.start(execution);
		Map<Integer, Integer> ports = docker.getPublishedTcpPorts(containerName);
		assertThat(ports, hasKey(1234));
	}

	@Test
	public void customizingWorkingDirectory() throws IOException, InterruptedException {
		ContainerDefinition def = new ContainerDefinition("alpine", "./echo", "-n", "hello");
		def.setWorkingDirectory("/bin");

		String output = docker.executeAndReturnOutput(def);
		assertThat(output, is("hello"));
	}

	@Test
	public void shouldCorrectlyRemoveUsedVolumes() throws IOException, InterruptedException {
		int beforeCount = docker.getVolumesCount();
		ContainerDefinition def = new ContainerDefinition("alpine", "sh", "-c", "date > /opt/hello; sleep 5");
		def.addVolume("/opt");
		docker.start(def);
		docker.close();
		assertThat(docker.getVolumesCount(), is(beforeCount));
	}

	@Test
	public void parseJson() throws IOException {
		String json = Docker.readFully(getClass().getResourceAsStream("/inspect-example.json"));
		ObjectMapper jsonReader = new ObjectMapper();
		JsonNode root = jsonReader.readTree(json);
		Map<Integer, Integer> ports = Docker.doGetPublishedPorts(root);
		assertThat(ports, hasEntry(8888, 1500));
	}

	@Test
	public void ensureProcNetCouldBeRead() {
		String example = "  sl  local_address rem_address   st tx_queue rx_queue tr tm->when retrnsmt   uid  timeout inode\n" +
			"   0: 00000000:04D2 00000000:0000 0A 00000000:00000000 00:00000000 00000000     0        0 15662 1 ffff8800baf1c780 100 0 0 10 0\n" +
			"   1: 00000000:04D3 00000000:0000 0A 00000000:00000000 00:00000000 00000000     0        0 15662 1 ffff8800baf1c780 100 0 0 10 0";

		Set<Integer> listenPorts = readListenPorts(example);
		assertThat(listenPorts, hasItems(1234, 1235));
	}
}