package me.bazhenov.docker;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
public @interface Port {
	int value();
	int atHost() default 0;
}
