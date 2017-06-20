package me.bazhenov.docker;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Describes which containers should be started before test execution.
 * <p>
 * Use {@link AfterContainerStart} annotation to get exposed ports in your test.
 * <p>
 * Typical usage:
 * <pre>
 *   &#064;Container(name="mysql", exposePorts=3306, image="mysql:5.6", environment={"MYSQL_ROOT_PASSWORD=secret"})
 *   &#064;Listeners(DockerTestNgListener.class)
 *   public class SampleTest {
 *
 *   }
 * </pre>
 *
 * @see AfterContainerStart
 * @see DockerTestNgListener
 */
@SuppressWarnings("WeakerAccess")
@Retention(value = RUNTIME)
@Target({TYPE_USE, TYPE})
@Repeatable(ContainerGroup.class)
public @interface Container {

	/**
	 * @return image name of a container (eg. {@code mongo:3.0})
	 */
	String image();

	/**
	 * @return name of a container to be used with {@link ContainerPort} annotation
	 */
	String name();

	/**
	 * @return entry point of a container
	 */
	String[] command() default {};

	/**
	 * @return TCP ports to be exposed from a container to the host machine
	 */
	int[] exposePorts() default {};

	/**
	 * @return list of environment variables which will be passed to a process running in the container
	 */
	String[] environment() default {};

	/**
	 * @return should be container removed after test execution or not
	 */
	boolean removeAfterCompletion() default true;

	/**
	 * @return working directory of a process inside a container
	 */
	String workingDir() default "";
}
