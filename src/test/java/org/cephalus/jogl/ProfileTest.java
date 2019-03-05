package org.cephalus.jogl;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLProfile;
import org.cephalus.jogl.junit.JoglRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JoglRunner.class)
@Iterations(1)
public class ProfileTest {

    @Test
    @Profile(GLProfile.GL2)
    public void gl2(GLAutoDrawable drawable) {
        assertThat(drawable.getGL())
                .isInstanceOf(GL2.class);
    }

    @Test
    @Profile(GLProfile.GL3)
    public void gl3(GLAutoDrawable drawable) {
        assertThat(drawable.getGL())
                .isInstanceOf(GL3.class);
    }

    @Test
    @Profile(GLProfile.GL4)
    public void gl4(GLAutoDrawable drawable) {
        assertThat(drawable.getGL())
                .isInstanceOf(GL4.class);
    }
}
