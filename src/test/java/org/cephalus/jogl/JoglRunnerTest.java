package org.cephalus.jogl;

import com.jogamp.opengl.GLAutoDrawable;
import org.cephalus.jogl.junit.JoglRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JoglRunner.class)
@Iterations(1)
public class JoglRunnerTest {

    @Before
    public void before(GLAutoDrawable drawable) {
        System.out.println("before");
    }

    @After
    public void after(GLAutoDrawable drawable) {
        System.out.println("after");
    }

    @Test
    public void test1(GLAutoDrawable drawable) {
        System.out.println("test1");
    }

    @Test
    public void test2(GLAutoDrawable drawable) {
        System.out.println("test2");
    }
}
