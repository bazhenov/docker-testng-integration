package me.bazhenov.docker;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.IOException;

import static me.bazhenov.docker.Utils.readLineFrom;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

public class ContainersFromAnnotationInheritedTest {

	public static class FirstTest extends BaseContainersFromAnnotationTest {

		@Test
		public void bar() {
			assertThat(hostPort, greaterThan(1024));
		}
	}

	public static class SecondTest extends BaseContainersFromAnnotationTest {

		@Test
		public void bar() {
			assertThat(readLineFrom(hostPort), equalTo("Hello"));
		}
	}
}

@ContainersFrom(SharedContainers.class)
@Listeners(DockerTestNgListener.class)
abstract class BaseContainersFromAnnotationTest {

	protected int hostPort;

	@AfterContainerStart
	public void setUpDocker(@ContainerPort(name = "nc1", port = 1234) int hostPort1) {
		this.hostPort = hostPort1;
	}

	@Test
	public void foo() throws IOException {
		assertThat(hostPort, greaterThan(1024));
		assertThat(readLineFrom(hostPort), equalTo("Hello"));
	}
}
