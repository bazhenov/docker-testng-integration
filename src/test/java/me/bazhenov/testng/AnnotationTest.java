package me.bazhenov.testng;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

@Container(name = "nc1", image = "alpine", exposePorts = 1234,
	command = {"nc", "-lkp", "1234", "-s", "0.0.0.0", "-e", "echo", "-e", "HTTP/1.1 200 OK\n\nHello"})
@Container(name = "nc2", image = "alpine", exposePorts = 1234,
	command = {"nc", "-l", "-p", "1234", "-s", "0.0.0.0"})
public class AnnotationTest {

	@Test
	@Parameters({"nc1:1234", "nc2:1234"})
	public void foo(int hostPort1, int hostPort2) throws IOException {
		assertThat(hostPort1, greaterThan(1024));
		assertThat(hostPort2, greaterThan(1024));

		URL obj = new URL("http://localhost:" + hostPort1 + "/");
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		assertThat(in.readLine(), equalTo("Hello"));
	}
}
