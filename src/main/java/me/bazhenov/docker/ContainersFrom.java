package me.bazhenov.docker;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Import container definitions from given class files. This annotation allows to share container definitions between
 * several test cases without duplicating {@link Container} annotations.
 * <p>
 * Typical use:
 * <pre>
 *   &#064;Container(name="mysql", publish=@Port(3306), image="mysql:5.6", environment={"MYSQL_ROOT_PASSWORD=secret"})
 *   class SharedContainers {}
 *
 *   &#064;ContainersFrom(SharedContainers.class)
 *   &#064;Listeners(DockerTestNgListener.class)
 *   class TestCase1 {}
 *
 *   &#064;ContainersFrom(SharedContainers.class)
 *   &#064;Listeners(DockerTestNgListener.class)
 *   class TestCase2 {}
 * </pre>
 */
@SuppressWarnings("WeakerAccess")
@Retention(value = RUNTIME)
@Target({TYPE_USE, TYPE})
public @interface ContainersFrom {

	/**
	 * @return list of classes to import {@link Container} annotations from
	 */
	Class<?>[] value();
}
