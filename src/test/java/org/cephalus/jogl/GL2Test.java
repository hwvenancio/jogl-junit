package org.cephalus.jogl;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLProfile;
import org.cephalus.jogl.junit.JoglRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JoglRunner.class)
@Configuration(profile = GLProfile.GL2)
public class GL2Test {

    @Test
    public void profile(GLAutoDrawable drawable) {
        assertTrue(drawable.getGL() instanceof GL2);
        assertFalse(drawable.getGL() instanceof GL3);
    }

}
