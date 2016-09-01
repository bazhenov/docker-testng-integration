package me.bazhenov.testng;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Container(name = "nc1", image = "alpine", command = {"nc", "-l", "-p", "1234"}, exposePorts = 1234)
@Container(name = "nc2", image = "alpine", command = {"nc", "-l", "-p", "1234"}, exposePorts = 1234)
public class App1Test {

	@BeforeMethod
	@Parameters({"docker://nc1:1234", "docker://nc2:1234"})
	public void foo(int hostPort1, int hostPort2) {
		System.out.println("Running nc1 on: " + hostPort1);
		System.out.println("Running nc2 on: " + hostPort2);
	}

	@Test
	public void foo() throws InterruptedException {
		Thread.sleep(10000);
	}
}
