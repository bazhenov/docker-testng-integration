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

@Container(name = "nc", image = "alpine", publish = @Port(1234),
	volumes = {@Volume(value = "/opt/response", atHost = "./src/test/resources/response.txt")},
	command = {"nc", "-lkp", "1234", "-s", "0.0.0.0", "-e", "cat", "/opt/response"})
@Listeners(DockerTestNgListener.class)
public class ContainerVolumesTest {

	private int port;

	@AfterContainerStart
	public void afterContainerStart(@ContainerPort(name = "nc", port = 1234) int port) {
		this.port = port;
	}

	@Test
	public void testVolumes() throws IOException {
		URL obj = new URL("http://localhost:" + port + "/");
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
			assertThat(in.readLine(), equalTo("Volume response"));
		}
	}
}
