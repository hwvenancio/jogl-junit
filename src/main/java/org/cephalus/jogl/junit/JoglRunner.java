package org.cephalus.jogl.junit;

import com.jogamp.nativewindow.WindowClosingProtocol;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.FPSAnimator;
import org.cephalus.jogl.Compare;
import org.cephalus.jogl.Configuration;
import org.cephalus.jogl.Fps;
import org.cephalus.jogl.Iterations;
import org.cephalus.jogl.Profile;
import org.cephalus.jogl.Recorder;
import org.cephalus.jogl.Swap;
import org.cephalus.jogl.Window;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RunRules;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.Annotatable;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.cephalus.jogl.ImageComparator.calculateDivergence;
import static org.cephalus.jogl.ImageComparator.getDifferenceImage;
import static org.cephalus.jogl.Swap.Type.AUTO;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JoglRunner extends ParentRunner<FrameworkMethod> {

    static {
        GLProfile.initSingleton();
    }

    public JoglRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    protected List<FrameworkMethod> getChildren() {
        return getTestClass().getAnnotatedMethods(Test.class);
    }

    @Override
    protected Description describeChild(FrameworkMethod testMethod) {
        return Description.createTestDescription(getTestClass().getName(), testMethod.getName());
    }

    @Override
    protected boolean isIgnored(FrameworkMethod testMethod) {
        return testMethod.getAnnotation(Ignore.class) != null;
    }

    @Override
    protected void runChild(final FrameworkMethod testMethod, final RunNotifier notifier) {
        Description testDescription = describeChild(testMethod);
        if (isIgnored(testMethod)) {
            notifier.fireTestIgnored(testDescription);
        } else {
            try {
                new Runner(notifier, getTestClass(), testMethod, testDescription).run();
            } catch (Throwable e) {
                notifier.fireTestFailure(new Failure(testDescription, e));
            }
        }
    }

    @Configuration
    private static class Runner implements Runnable {

        private final RunNotifier notifier;
        private final TestClass testClass;
        private final FrameworkMethod testMethod;
        private final Description testDescription;

        private Object testInstance;

        public Runner(RunNotifier notifier,  TestClass testClass, FrameworkMethod testMethod, Description testDescription) {
            this.notifier = notifier;
            this.testClass = testClass;
            this.testMethod = testMethod;
            this.testDescription = testDescription;
        }

        @Override
        public void run() {
            try {
                testInstance = testClass.getOnlyConstructor().newInstance();

                Statement test = new LoopRunner(notifier, testClass, testMethod, testDescription, testInstance);
                test = withRules(test);
                test.evaluate();
            } catch (Throwable ex) {
                notifier.fireTestFailure(new Failure(testDescription, ex));
            }
        }

        private Statement withRules(Statement base) {
            List<TestRule> result = testClass.getAnnotatedFieldValues(testInstance, Rule.class, TestRule.class);
            return new RunRules(base, result, testDescription);
        }
    }

    @Configuration
    private static class LoopRunner extends Statement implements GLEventListener {

        private final RunNotifier notifier;
        private final TestClass testClass;
        private final FrameworkMethod testMethod;
        private final Description testDescription;
        private Object testInstance;
        private final String title;
        private final List<Class<? extends Throwable>> exceptions;

        private CombinedConfiguration config;

        private List<Throwable> errors = new ArrayList<>();

        private int iterations = 0;

        private GLWindow glWindow;
        private GLAnimatorControl animator;

        public LoopRunner(RunNotifier notifier, TestClass testClass, FrameworkMethod testMethod, Description testDescription, Object testInstance) {
            this.notifier = notifier;
            this.testClass = testClass;
            this.testMethod = testMethod;
            this.testDescription = testDescription;
            this.testInstance = testInstance;
            this.title = testDescription.getMethodName();
            this.exceptions = extractExpectedExceptions(testMethod);
        }

        @Override
        public void evaluate() throws Throwable {
            notifier.fireTestStarted(testDescription);

            config = getConfiguration(testMethod);

            createWindow();
            createGLEventListener();
            createAnimator();

            animator.start();
            animator.getThread().join();

            disposeWindow();

            if(!exceptions.isEmpty() && errors.isEmpty()){
                notifier.fireTestFailure(new Failure(testDescription, new AssertionError("Expected exception: "
                        + exceptions.get(0).getName())));
            } else {
                for (Throwable error : errors) {
                    if (!expectedException(error))
                        notifier.fireTestFailure(new Failure(testDescription, error));
                }
            }

            notifier.fireTestFinished(testDescription);
        }

        public void createWindow() {
            int width = config.width;
            int height = config.height;
            GLProfile profile = GLProfile.get(config.profile);
            GLCapabilities capabilities = new GLCapabilities(profile);
            glWindow = GLWindow.create(capabilities);
            glWindow.setSize(width, height);
            glWindow.setTitle(title);
            glWindow.setDefaultCloseOperation(WindowClosingProtocol.WindowClosingMode.DO_NOTHING_ON_CLOSE);
            glWindow.setAutoSwapBufferMode(config.swap);
            glWindow.setVisible(true);
        }

        public void disposeWindow() {
            glWindow.setVisible(false);
            glWindow.destroy();
            glWindow = null;
        }

        public void createAnimator() {
            int fps = config.fps;
            if(fps > 0)
                animator = new FPSAnimator(config.fps);
            else
                animator = new Animator();
            animator.add(glWindow);
        }

        public void createGLEventListener() {
            glWindow.addGLEventListener(this);
        }

        private void invoke(FrameworkMethod method, final Object... params) {
            try {
                method.invokeExplosively(testInstance, params);
            } catch (Throwable e) {
                errors.add(e);
            }
        }

        private void invokeAll(List<FrameworkMethod> methods, final Object... params) {
            for (FrameworkMethod each : methods) {
                invoke(each, params);
            }
        }

        @Override
        public void init(GLAutoDrawable drawable) {
            List<FrameworkMethod> befores = testClass.getAnnotatedMethods(Before.class);
            invokeAll(befores, drawable);
        }

        @Override
        public void dispose(GLAutoDrawable drawable) {
            List<FrameworkMethod> afters = testClass.getAnnotatedMethods(After.class);
            invokeAll(afters, drawable);
        }

        @Override
        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        }

        @Override
        public void display(GLAutoDrawable drawable) {
            if (!errors.isEmpty() || ++iterations > config.iterations) {
                animator.stop();
                return;
            }
            invoke(testMethod, drawable);
            compare(drawable);
        }

        private CombinedConfiguration getConfiguration(final FrameworkMethod testMethod) {
            Configuration defaultConfiguration = JoglRunner.Runner.class.getAnnotation(Configuration.class);
            return new CombinedConfiguration(defaultConfiguration, testClass, testMethod);
        }

        private boolean expectedException(Throwable error) {
            for(Class<? extends Throwable> errorClass : exceptions)
                if(errorClass.isInstance(error))
                    return true;
            return false;
        }

        private List<Class<? extends Throwable>> extractExpectedExceptions(FrameworkMethod testMethod) {
            Test test = testMethod.getAnnotation(Test.class);
            if(test == null || test.expected() == Test.None.class)
                return Collections.emptyList();
            return Collections.singletonList(test.expected());
        }

        private void compare(GLAutoDrawable drawable) {
            if(config.compare != null) {
                try {
                    config.compare.compareNext(drawable);
                } catch (Throwable e) {
                    errors.add(e);
                }
            }
        }
    }

    private static class CombinedConfiguration {
        private String profile;
        private int width;
        private int height;
        private int fps;
        private int iterations;
        private boolean swap;
        private CombinedCompare compare;

        public CombinedConfiguration(Configuration defaultConfiguration, TestClass testClass, FrameworkMethod testMethod) {
            apply(defaultConfiguration);

            applyAll(testClass);
            applyAll(testMethod);
            compare = CombinedCompare.create(testClass, testMethod);
        }

        private void applyAll(Annotatable source) {
            Configuration configuration = source.getAnnotation(Configuration.class);
            apply(configuration);
            Profile profile = source.getAnnotation(Profile.class);
            apply(profile);
            Window window = source.getAnnotation(Window.class);
            apply(window);
            Fps fps = source.getAnnotation(Fps.class);
            apply(fps);
            Iterations iterations = source.getAnnotation(Iterations.class);
            apply(iterations);
            Swap swap = source.getAnnotation(Swap.class);
            apply(swap);
        }

        private void apply(Configuration configuration) {
            if(configuration == null)
                return;
            profile = configuration.profile();
            width = configuration.width();
            height = configuration.height();
            fps = configuration.fps();
            iterations = configuration.iterations();
            swap = configuration.swap() == AUTO;
        }

        private void apply(Profile annotation) {
            if(annotation == null)
                return;
            profile = annotation.value();
        }

        private void apply(Window annotation) {
            if(annotation == null)
                return;
            width = annotation.width();
            height = annotation.height();
        }

        private void apply(Fps annotation) {
            if(annotation == null)
                return;
            fps = annotation.value();
        }

        private void apply(Iterations annotation) {
            if(annotation == null)
                return;
            iterations = annotation.value();
        }

        private void apply(Swap annotation) {
            if(annotation == null)
                return;
            swap = annotation.value() == AUTO;
        }
    }

    private static class CombinedCompare {

        private final Class<?> javaClass;
        private final String methodName;

        private String reference;
        private float maxDivergence;

        private ZipInputStream zip;

        public CombinedCompare(Class<?> javaClass, String methodName) {
            this.javaClass = javaClass;
            this.methodName = methodName;
        }

        public static CombinedCompare create(TestClass testClass, FrameworkMethod testMethod) {
            Compare methodCompare = testMethod.getAnnotation(Compare.class);
            Compare classCompare = testClass.getAnnotation(Compare.class);

            if(methodCompare == null && classCompare == null)
                return null;

            CombinedCompare instance = new CombinedCompare(testClass.getJavaClass(), testMethod.getName());
            instance.apply(testMethod);
            instance.apply(classCompare);
            instance.apply(methodCompare);
            instance.start();
            return instance;
        }

        public void compareNext(GLAutoDrawable drawable) {
            try {
                ZipEntry entry = zip.getNextEntry();
                BufferedImage expected = ImageIO.read(zip);
                BufferedImage actual = Recorder.takeSnapshot(drawable);
                BufferedImage diff = getDifferenceImage(expected, actual);
                float divergence = calculateDivergence(diff);
                try {
                    assertTrue(divergence <= maxDivergence);
                } catch (AssertionError ex) {
                    save(methodName, entry.getName(), diff);
                    throw ex;
                }
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

        private void save(String methodName, String frameName, BufferedImage diff) throws IOException {
            File file = new File("target/recorded-frames/diff_" + methodName + "_" + frameName);
            file.getParentFile().mkdirs();
            ImageIO.write(diff, "PNG", file);
        }

        private void start() {
            URL resource = javaClass.getResource(reference + ".zip");
            assertNotNull("Reference not found!", resource);
            zip = new ZipInputStream(javaClass.getResourceAsStream(reference + ".zip"));
        }

        private void apply(FrameworkMethod method) {
            if(reference == null || reference.isEmpty())
                reference = method.getName();
        }

        private void apply(Compare compare) {
            if(compare == null)
                return;

            if(!compare.reference().isEmpty())
                this.reference = compare.reference();
            this.maxDivergence = compare.maxDivergence();
        }
    }
}
