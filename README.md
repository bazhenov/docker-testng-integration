# Docker TestNG Integration

## Introduction

This library allows you to run Docker containers before your test suite.

## Usage

Include dependency in your `pom.xml`

```xml
<dependency>
  <groupId>me.bazhenov</groupId>
  <artifactId>docker-testng-integration</artifactId>
  <version>1.5</version>
  <scope>test</scope>
</dependency>
```

Then in your test use class-level annotation `@Container` (you can provide several annotations), as well as method-level
annotation `@AfterContainerStart`. You also need to register `me.bazhenov.docker.DockerTestNgListener`

```java
@Container(name = "my-container", image = "mysql", publish = @Port(3306), environment =
  {"MYSQL_ROOT_PASSWORD=secret"})
@Listeners(DockerTestNgListener.class)
public class MySqlIT {

  private int mysqlPort;

  @AfterContainerStart
  public void setUp(@ContainerPort(name = "my-container", port = 3306) int port) {
    mysqlPort = port;
  }

  @Test
  public void myTest() {
    String jdbcUrl = "jdbc:mysql://localhost:" + mysqlPort + "/db";
    // Using this URL to connect to MySQL
  }
}
```

If you don't want to register listener on a test-case class you can add the listener to your test suite. If you are
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
            <value>me.bazhenov.docker.DockerTestNgListener</value>
          </property>
        </properties>
      </configuration>
    </plugin>
  </plugins>
</build>
```

You should remember that registering listener both in XML and test classes (using annotations) will result in container being
started twice.

By default all containers are local to the test case. So if you define two test cases with container named `mysql`, 2 mysql 
instances will be started. One for each test case. But you can share containers between test cases, if you need to.

```java
@Container(image = "mysql:5.6", name = "mysql", publish = @Port(3306))
class SharedContainers {}

@ContainersFrom(SharedContainers.class)
@Listeners(DockerTestNgListener.class)
class MyTestCase {

  @AfterContainerStart
  public void dockerSetup(@ContainerPort(name = "mysql", port = 3306) int mysqlPort) {
    // ...
  }
}
```

In this case container `mysql` will be started just once and shared between all test cases importing it using `@ContainersFrom`.

## Features

* library using `docker` command line utility;
* provides an easy way of getting dynamically allocated ports in tests;
* mark all containers with `testng` label, so they could be easily found with `docker ps -af label=testng` command;
* can map host directories as volumes inside a container;
* waits for given ports to be open in a container, so containerized service is up at the moment of test starts;
* library can share containers before several test cases using `@ContainersFrom` annotation. This allows to speed up test
execution if you can reuse single container instead of starting a new container each time.

## Limitations

* only TCP ports are exposed at the moment
