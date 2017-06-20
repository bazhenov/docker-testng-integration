package me.bazhenov.docker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class NotificationMethod {

	private ContainerNamespace namespace;
	private final Method method;
	private List<PortRef> arguments;

	NotificationMethod(ContainerNamespace namespace, Method method, List<PortRef> arguments) {
		this.namespace = requireNonNull(namespace);
		this.method = requireNonNull(method);
		this.arguments = requireNonNull(arguments);
	}

	void call(Object test) {
		Object[] args = new Integer[arguments.size()];
		for (int i = 0; i < args.length; i++) {
			PortRef port = arguments.get(i);
			args[i] = namespace.lookupHostPort(port.getContainerDefinition(), port.getContainerPort());
		}
		try {
			method.invoke(test, args);
		} catch (InvocationTargetException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	Method getMethod() {
		return method;
	}

	List<PortRef> getArguments() {
		return arguments;
	}
}
