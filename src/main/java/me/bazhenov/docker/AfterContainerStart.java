package me.bazhenov.docker;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Methods annotated with {@link AfterContainerStart} are called by {@link DockerTestNgListener} after
 * all containers are started.
 *
 * All parameters of those methids should be of type {@code int} and annotated with {@link ContainerPort} annotation.
 * <p>
 * Typical usage:
 * <pre>
 *   &#064;Container(name="mysql", publish=@Port(3306), image=...)
 *   &#064;Listeners(DockerTestNgListener.class)
 *   &#064;Listeners(DockerTestNgListener.class)
 *   class SampleTest {
 *
 *     private int mysqlPort;
 *
 *     &#064;AfterContainerStart
 *     public void initDocker(&#064;ContainerPort(name="mysql", port=3306) int mysqlPort) {
 *       this.mysqlPort = mysqlPort;
 *     }
 *
 *     &#064;Test
 *     public void testMysql() {
 *       String url = "jdbc:mysql://localhost:" + mysqlPort;
 *       ...
 *     }
 *   }
 * </pre>
 */
@SuppressWarnings("WeakerAccess")
@Retention(RUNTIME)
@Target(METHOD)
public @interface AfterContainerStart {

}
