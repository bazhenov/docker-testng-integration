package me.bazhenov.docker;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

@ContainersFrom(SharedContainers.class)
@Listeners(DockerTestNgListener.class)
public class ContainersFromAnnotationTest {

	private int hostPort;

	@AfterContainerStart
	public void setUpDocker(@ContainerPort(name = "nc1", port = 1234) int hostPort1) {
		this.hostPort = hostPort1;
	}

	@Test
	public void foo() throws IOException {
		assertThat(hostPort, greaterThan(1024));

		URL obj = new URL("http://localhost:" + hostPort + "/");
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
			assertThat(in.readLine(), equalTo("Hello"));
		}
	}
}

@Container(name = "nc1", image = "alpine",
	command = {"nc", "-lkp", "1234", "-s", "0.0.0.0", "-e", "echo", "-e", "HTTP/1.1 200 OK\n\nHello"},
	exposePorts = 1234) class SharedContainers {

}