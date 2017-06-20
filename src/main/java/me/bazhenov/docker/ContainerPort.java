package me.bazhenov.docker;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This annotation is used in pair with {@link AfterContainerStart}.
 *
 * @see AfterContainerStart <b>@AfterContainerStart</b> for more information
 */
@SuppressWarnings("WeakerAccess")
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface ContainerPort {

	/**
	 * @return name of references container given in {@link Container#name()}
	 */
	String name();

	/**
	 * @return port number given in {@link Container#exposePorts()}
	 */
	int port();
}
