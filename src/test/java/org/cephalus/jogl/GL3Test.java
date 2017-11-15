package org.cephalus.jogl;


import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLProfile;
import org.cephalus.jogl.junit.JoglRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JoglRunner.class)
@Configuration(profile = GLProfile.GL3)
public class GL3Test {

    @Test
    public void profile(GLAutoDrawable drawable) {
        assertTrue(drawable.getGL() instanceof GL3);
        assertFalse(drawable.getGL() instanceof GL3);
    }

}
