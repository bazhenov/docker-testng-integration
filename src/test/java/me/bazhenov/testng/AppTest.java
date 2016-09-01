package me.bazhenov.testng;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Container(image = "mysql", name = "foo", exposeAllPorts = true, environment = {
	"MYSQL_ROOT_PASSWORD=root"
})
public class AppTest {

	@BeforeMethod
	@Parameters("docker://foo:3306")
	public void setUp(int port) {
		System.out.println("Mysql on " + port);
	}

	@Test
	public void foo() throws InterruptedException {
		Thread.sleep(30000);
	}
}
