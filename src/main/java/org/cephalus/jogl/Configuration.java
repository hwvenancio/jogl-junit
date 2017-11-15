package org.cephalus.jogl;

import com.jogamp.opengl.GLProfile;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Configuration {

    String profile() default GLProfile.GL2;

    int width() default 640;

    int height() default 480;

    int fps() default 60;

    int iterations() default 120;
}
