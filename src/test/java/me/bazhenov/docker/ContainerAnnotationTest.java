package me.bazhenov.docker;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.IOException;

import static me.bazhenov.docker.Utils.readLineFrom;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

@Container(name = "nc1", image = "alpine", publish = @Port(1234),
	command = {"nc", "-lkp", "1234", "-s", "0.0.0.0", "-e", "echo", "-e", "HTTP/1.1 200 OK\n\nHello"})
@Container(name = "nc2", image = "alpine", publish = {@Port(1234), @Port(4321)}, waitForAllExposedPorts = false,
	command = {"nc", "-l", "-p", "1234", "-s", "0.0.0.0"})
@Container(name = "nc3", image = "alpine", publish = @Port(value = 1234, atHost = 30001),
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
	public void foo() {
		assertThat(hostPort1, greaterThan(1024));
		assertThat(hostPort2, greaterThan(1024));
		assertThat(hostPort3, equalTo(30001));
		assertThat(readLineFrom(hostPort1), equalTo("Hello"));
	}
}
