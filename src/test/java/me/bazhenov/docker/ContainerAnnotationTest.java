package me.bazhenov.docker;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@Container(name = "nc1", image = "alpine", publishPorts = "1234",
	command = {"nc", "-lkp", "1234", "-s", "0.0.0.0", "-e", "echo", "-e", "HTTP/1.1 200 OK\n\nHello"})
@Container(name = "nc2", image = "alpine", exposePorts = 1234, waitForAllExposedPorts = false,
	command = {"nc", "-l", "-p", "1234", "-s", "0.0.0.0"})
@Container(name = "nc3", image = "alpine", publishPorts = "9000-9999:1234",
	options = {"-m50m", "--cpu-period=50000", "--cpu-quota=25000"},
	command = {"nc", "-l", "-p", "1234", "-s", "0.0.0.0"})
@Listeners(DockerTestNgListener.class)
public class ContainerAnnotationTest {

	private int hostPort1;
	private int hostPort2;
	private int hostPort3;

	@AfterContainerStart
	public void setUpDocker(@ContainerPort(name = "nc1", port = 1234) int hostPort1,
													@ContainerPort(name = "nc2", port = 1234) int hostPort2,
													@ContainerPort(name = "nc3", port = 1234) int hostPort3) {
		this.hostPort1 = hostPort1;
		this.hostPort2 = hostPort2;
		this.hostPort3 = hostPort3;
	}

	@Test
	public void foo() throws IOException {
		assertThat(hostPort1, greaterThan(1024));
		assertThat(hostPort2, greaterThan(1024));
		assertThat(hostPort3, is(both(greaterThanOrEqualTo(9000)).and(lessThanOrEqualTo(9999))));

		URL obj = new URL("http://localhost:" + hostPort1 + "/");
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
			assertThat(in.readLine(), equalTo("Hello"));
		}
	}
}
