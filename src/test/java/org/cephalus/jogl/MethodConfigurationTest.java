package org.cephalus.jogl;

import com.jogamp.opengl.*;
import org.cephalus.jogl.junit.JoglRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;

@RunWith(JoglRunner.class)
@Configuration(iterations = 1)
public class MethodConfigurationTest {

    @Test
    public void gl2(GLAutoDrawable drawable) {
        assertTrue(drawable.getGL() instanceof GL2);
    }

    @Test
    @Profile(GLProfile.GL3)
    public void gl3(GLAutoDrawable drawable) {
        assertTrue(drawable.getGL() instanceof GL3);
    }
}
