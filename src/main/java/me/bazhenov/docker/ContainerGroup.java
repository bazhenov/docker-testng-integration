package me.bazhenov.docker;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@SuppressWarnings("WeakerAccess")
@Target({TYPE_USE, TYPE})
@Retention(value = RUNTIME)
public @interface ContainerGroup {

	Container[] value();
}
