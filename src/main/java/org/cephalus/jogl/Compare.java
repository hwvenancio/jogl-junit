package org.cephalus.jogl;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Compare {
    String reference() default "";
    float maxDivergence() default 0.01f;
}
