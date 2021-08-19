package me.bazhenov.docker;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static me.bazhenov.docker.Utils.readLineFrom;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Container(name = "nc1", image = "alpine", publish = @Port(1234), network = "test-network",
	command = {"nc", "-lkp", "1234", "-s", "0.0.0.0", "-e", "nc", "nc2-net-alias", "4321"})
@Container(name = "nc2", image = "alpine", network = "test-network", networkAlias = "nc2-net-alias",
	command = {"nc", "-lkp", "4321", "-s", "0.0.0.0", "-e", "echo", "-e", "HTTP/1.1 200 OK\n\nHello"})
@Listeners(DockerTestNgListener.class)
public class ContainersNetworkTest {

	private int port;

	@AfterContainerStart
	public void setUpDocker(@ContainerPort(name = "nc1", port = 1234) int port) {
		this.port = port;
	}

	@Test
	public void foo() {
		assertThat(readLineFrom(port), equalTo("Hello"));
	}

}
