package me.bazhenov.testng;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(value = RUNTIME)
@Repeatable(Containers.class)
public @interface Container {

	String image();

	String name();

	String[] command() default {};

	int[] exposePorts() default {};

	boolean exposeAllPorts() default false;

	String[] environment() default {};

	boolean removeAfterCompletion() default true;
}
