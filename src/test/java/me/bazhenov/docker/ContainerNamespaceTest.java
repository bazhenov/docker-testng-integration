package me.bazhenov.docker;

import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ContainerNamespaceTest {

	@Test
	public void shouldBeAbleToRegisterTcpPorts() {
		Map<String, ContainerDefinition> definitions = new HashMap<>();
		ContainerDefinition def = new ContainerDefinition("img", "exec");
		definitions.put("c1", def);
		ContainerNamespace namespace = new ContainerNamespace(definitions, emptyList());

		Map<Integer, Integer> ports = new HashMap<>();
		ports.put(134, 13);
		namespace.registerPublishedTcpPorts(def, ports);
		assertThat(namespace.lookupHostPort(def, 134), is(13));
	}
}