package org.cephalus.jogl;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GLAutoDrawable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Recorder {

    private final ZipOutputStream zip;
    private final File zipFile;
    private String testName;
    private int frame;

    public Recorder(String testName) throws IOException {
        this.testName = testName;
        zipFile = new File("target/recorded-frames/" + testName + ".zip");
        zipFile.getParentFile().mkdirs();
        this.zip = new ZipOutputStream(new FileOutputStream(zipFile));
    }

    public void saveSnapshot(GLAutoDrawable drawable) throws IOException {
        BufferedImage image = takeSnapshot(drawable);

        String name = String.format(testName + "_%04d.png", ++frame);
        ZipEntry snapshot = new ZipEntry(name);

        String format = "PNG";

        zip.putNextEntry(snapshot);
        ImageIO.write(image, format, zip);
    }

    public static BufferedImage takeSnapshot(GLAutoDrawable drawable) {
        GL2ES3 gl = drawable.getGL().getGL2ES3();
        gl.glFlush();
        gl.glFinish();
        gl.glReadBuffer(GL.GL_BACK);
        int width = drawable.getSurfaceWidth();
        int height = drawable.getSurfaceHeight();
        int bpp = 4;
        ByteBuffer buffer = Buffers.newDirectByteBuffer(width * height * bpp);
        gl.glReadPixels(0, 0, width, height, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, buffer);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for(int x = 0; x < width; x++)
        {
            for(int y = 0; y < height; y++)
            {
                int i = (x + (width * y)) * bpp;
                int r = buffer.get(i) & 0xFF;
                int g = buffer.get(i + 1) & 0xFF;
                int b = buffer.get(i + 2) & 0xFF;
                image.setRGB(x, height - (y + 1), (0xFF << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return image;
    }

    public void close() throws IOException {
        zip.flush();
        zip.close();
    }

    public void clear() {
        zipFile.delete();
    }
}
