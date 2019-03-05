package org.cephalus.jogl;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderUtil;
import jogamp.opengl.glu.error.Error;
import org.cephalus.jogl.junit.JoglRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_COLOR_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_FALSE;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL2ES2.GL_COMPILE_STATUS;
import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_LINK_STATUS;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;


@RunWith(JoglRunner.class)
@Iterations(1)
public class CompareTest {

    private static final int[] ints = new int[3];

    private int vs;
    private int fs;
    private int program;

    private int position;
    private int errPosition;
    private int color;
    private int okVao;
    private int errVao;

    @Before
    public void shader(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        vs = loadShader(gl,"simple-color.vs", GL_VERTEX_SHADER);
        fs = loadShader(gl, "simple-color.fs", GL_FRAGMENT_SHADER);
        program = linkProgram(gl, vs, fs);

        gl.glUseProgram(program);
    }

    @Before
    public void points(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        position = loadBuffer(gl,
                -0.5f, -0.5f, 0f
                , 0f, 0.707f, 0f
                , 0.5f, -0.5f, 0f
        );

        errPosition = loadBuffer(gl,
                -0.5f, -0.5f, 0f
                , 0f, 0.6f, 0f
                , 0.5f, -0.5f, 0f
        );

        color = loadBuffer(gl,
                1f, 0f, 0f
                , 0f, 1f, 0f
                , 0f, 0f, 1f
        );

        gl.glGenVertexArrays(1, ints, 0);
        okVao = ints[0];
        gl.glBindVertexArray(okVao);
        gl.glEnableVertexAttribArray(0);
        gl.glEnableVertexAttribArray(1);
        gl.glBindBuffer(GL_ARRAY_BUFFER, position);
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        gl.glBindBuffer(GL_ARRAY_BUFFER, color);
        gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);
        gl.glBindVertexArray(0);

        gl.glGenVertexArrays(1, ints, 0);
        errVao = ints[0];
        gl.glBindVertexArray(errVao);
        gl.glEnableVertexAttribArray(0);
        gl.glEnableVertexAttribArray(1);
        gl.glBindBuffer(GL_ARRAY_BUFFER, errPosition);
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        gl.glBindBuffer(GL_ARRAY_BUFFER, color);
        gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);
        gl.glBindVertexArray(0);
    }

    @After
    public void dispose(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glBindVertexArray(0);
        ints[0] = color;
        ints[1] = errPosition;
        ints[2] = position;
        gl.glDeleteBuffers(3, ints, 0);
        ints[0] = okVao;
        ints[1] = errVao;
        gl.glDeleteVertexArrays(2, ints, 0);

        gl.glUseProgram(0);
        gl.glDetachShader(program, vs);
        gl.glDetachShader(program, fs);
        gl.glDeleteShader(fs);
        gl.glDeleteShader(vs);
        gl.glDeleteProgram(program);
    }

    @Test
    @Compare
    public void triangle(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        gl.glUseProgram(program);
        gl.glBindVertexArray(okVao);
        gl.glDrawArrays(GL_TRIANGLES, 0, 3);
        gl.glBindVertexArray(0);
        gl.glUseProgram(0);
    }

    @Test(expected = AssertionError.class)
    @Compare(reference = "triangle")
    public void differentTriangle(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        gl.glUseProgram(program);
        gl.glBindVertexArray(errVao);
        gl.glDrawArrays(GL_TRIANGLES, 0, 3);
        gl.glBindVertexArray(0);
        gl.glUseProgram(0);
    }

    private static int loadShader(GL2 gl, String resource, int type) {
        int shader = gl.glCreateShader(type);
        InputStream source = CompareTest.class.getResourceAsStream(resource);
        try(Scanner scanner = new Scanner(source, StandardCharsets.UTF_8.name())) {
            String text = scanner.useDelimiter("\\A").next();
            gl.glShaderSource(shader, 1, new String[] {text}, new int[] {text.length()}, 0);
            checkErrors(gl);
        }
        gl.glCompileShader(shader);
        checkErrors(gl);
        gl.glGetShaderiv(shader, GL_COMPILE_STATUS, ints, 0);
        int status = ints[0];
        if (status == GL_FALSE) {
            throw new IllegalStateException(ShaderUtil.getShaderInfoLog(gl, shader));
        }
        return shader;
    }

    private static int linkProgram(GL2 gl, int... shaders) {
        int program = gl.glCreateProgram();
        for(int shader : shaders) {
            gl.glAttachShader(program, shader);
        }
        gl.glLinkProgram(program);
        checkErrors(gl);
        gl.glGetProgramiv(program, GL_LINK_STATUS, ints, 0);
        int status = ints[0];
        if (status == GL_FALSE) {
            throw new IllegalStateException(ShaderUtil.getProgramInfoLog(gl, program));
        }
        return program;
    }

    private static int loadBuffer(GL2 gl, float... values) {
        FloatBuffer data = GLBuffers.newDirectFloatBuffer(values);
        return loadBuffer(gl, values.length, data);
    }

    private static int loadBuffer(GL2 gl, long size, FloatBuffer data) {
        gl.glGenBuffers(1, ints, 0);
        int vbo = ints[0];
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo);
        gl.glBufferData(GL_ARRAY_BUFFER, size * Float.BYTES, data, GL_STATIC_DRAW);
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);
        return vbo;
    }

    public static void checkErrors(GL gl) {
        int errorCheckValue = gl.glGetError();
        if (errorCheckValue != GL.GL_NO_ERROR) {
            throw new RuntimeException("OpenGL error " + errorCheckValue + ": " + Error.gluErrorString(errorCheckValue));
        }
    }
}
