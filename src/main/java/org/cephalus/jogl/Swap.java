package org.cephalus.jogl;

import static org.cephalus.jogl.Swap.Type.MANUAL;

public @interface Swap {

    Type value() default MANUAL;

    enum Type {
        AUTO
        , MANUAL
    }
}
