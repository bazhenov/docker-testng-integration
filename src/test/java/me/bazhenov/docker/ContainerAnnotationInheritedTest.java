package me.bazhenov.docker;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.IOException;

import static me.bazhenov.docker.Utils.readLineFrom;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

public class ContainerAnnotationInheritedTest {

	public static class FirstTest extends BaseContainerAnnotationTest {

		@Test
		public void bar() {
			assertThat(hostPort1, greaterThan(1024));
		}
	}

	public static class SecondTest extends BaseContainerAnnotationTest {

		@Test
		public void bar() {
			assertThat(hostPort2, greaterThan(1024));
		}
	}
}

@Container(name = "nc1", image = "alpine", publish = @Port(1234),
	command = {"nc", "-lkp", "1234", "-s", "0.0.0.0", "-e", "echo", "-e", "HTTP/1.1 200 OK\n\nHello"})
@Container(name = "nc2", image = "alpine", publish = {@Port(1234), @Port(4321)}, waitForAllExposedPorts = false,
	command = {"nc", "-l", "-p", "1234", "-s", "0.0.0.0"})
@Listeners(DockerTestNgListener.class)
abstract class BaseContainerAnnotationTest {

	protected int hostPort1, hostPort2;

	@AfterContainerStart
	public void setUpDocker(@ContainerPort(name = "nc1", port = 1234) int hostPort1,
	                        @ContainerPort(name = "nc2", port = 1234) int hostPort2) {
		this.hostPort1 = hostPort1;
		this.hostPort2 = hostPort2;
	}

	@Test
	public void foo() throws IOException {
		assertThat(hostPort1, greaterThan(1024));
		assertThat(hostPort2, greaterThan(1024));
		assertThat(readLineFrom(hostPort1), equalTo("Hello"));
	}
}
