# Docker TestNG Integration

## Introduction

This library allows you to run Docker containers before your test suite.

## Usage

Include dependency in your `pom.xml`

```xml
<dependency>
	<groupId>me.bazhenov</groupId>
	<artifactId>docker-testng-integration</artifactId>
	<version>1.0</version>
	<scope>test</scope>
</dependency>
```

Then in your test use call-level annotation `@Container` (you can provide several annotations)

```java
@Container(name = "my-container", image = "mysql", exposePorts = 3306,
	environment = {"MYSQL_ROOT_PASSWORD=secret"}
public class MySqlIT {

	private int mysqlPort

	@BeforeClass
	@Parameters({"docker://my-container:3306"})
	public void setUp(int port) {
		mysqlPort = port;
	}

	@Test
	public void myTest() {
		String jdbcUrl = "jdbc:mysql://localhost:" + mysqlPort + "/db";
	}
}
```

Then you should add following TestNG listener to your test suite `me.bazhenov.testng.DockerTestNgListener`. If you are
using Maven, just add following config to your `pom.xml`:

```xml
<build>
	<plugins>
		<plugin>
			<groupId>org.apache.maven.plugins</groupId>
			<artifactId>maven-surefire-plugin</artifactId>
			<configuration>
				<properties>
					<property>
						<name>listener</name>
						<value>me.bazhenov.testng.DockerTestNgListener</value>
					</property>
				</properties>
			</configuration>
		</plugin>
	</plugins>
</build>
```

## Features

* using `docker` command line utility;
* provides an easy way of getting dynamically allocated ports in tests;
* mark all containers with `testng` label, so they could be easily found with `docker ps -af label=testng` command;
* waits for given ports to be open in a container, so containerized service is up at the moment of test starts;

## Limitations

* only TCP ports are exposed at the moment
* there is no volume support at the moment