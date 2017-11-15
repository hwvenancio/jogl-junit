package org.cephalus.jogl.junit;

import com.jogamp.nativewindow.WindowClosingProtocol;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.FPSAnimator;
import org.cephalus.jogl.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.internal.runners.statements.RunBefores;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class JoglRunner extends ParentRunner<FrameworkMethod> {

    static {
        GLProfile.initSingleton();
    }

    public JoglRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    public Description getDescription() {
        Description description = Description.createSuiteDescription(getTestClass().getName());
        List<FrameworkMethod> testMethods = getTestClass().getAnnotatedMethods(Test.class);
        for(FrameworkMethod testMethod : testMethods) {
            description.addChild(Description.createTestDescription(getTestClass().getName(), testMethod.getName()));
        }
        return description;
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
    protected void runChild(final FrameworkMethod testMethod, final RunNotifier notifier) {
        CombinedConfiguration configuration = getConfiguration(testMethod);

        Description testDescription = describeChild(testMethod);
        try {
            new Runner(notifier, configuration, getTestClass(), testMethod, testDescription).run();
        } catch (Throwable e) {
            notifier.fireTestFailure(new Failure(testDescription, e));
        }
    }

    private CombinedConfiguration getConfiguration(final FrameworkMethod testMethod) {
        Configuration defaultConfiguration = Runner.class.getAnnotation(Configuration.class);
        return new CombinedConfiguration(defaultConfiguration, getTestClass(), testMethod);
    }

    @Configuration
    private static class Runner implements GLEventListener {

        private final RunNotifier notifier;
        private final CombinedConfiguration config;
        private final TestClass testClass;
        private final FrameworkMethod testMethod;
        private final Description testDescription;
        private final String title;

        private Object testInstance;

        private GLWindow glWindow;
        private GLAnimatorControl animator;

        private List<Throwable> errors = new ArrayList<>();

        private int iterations = 0;

        public Runner(RunNotifier notifier, CombinedConfiguration config, TestClass testClass, FrameworkMethod testMethod, Description testDescription) throws InitializationError {
            this.notifier = notifier;
            this.config = config;
            this.testClass = testClass;
            this.testMethod = testMethod;
            this.testDescription = testDescription;
            this.title = testDescription.getMethodName();
        }

        public void run() throws InterruptedException, IllegalAccessException, InvocationTargetException, InstantiationException {
            testInstance = testClass.getOnlyConstructor().newInstance();

            notifier.fireTestStarted(testDescription);

            createWindow();
            createGLEventListener();
            createAnimator();

            animator.start();
            animator.getThread().join();

            disposeWindow();

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
            glWindow.setAutoSwapBufferMode(false);
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
//            System.out.println("reshape");
        }

        @Override
        public void display(GLAutoDrawable drawable) {
            if (!errors.isEmpty() || ++iterations > config.iterations) {
                animator.stop();
                return;
            }
            invoke(testMethod, drawable);
        }
    }

    private static class CombinedConfiguration {
        private String profile;
        private int width;
        private int height;
        private int fps;
        private int iterations;

        public CombinedConfiguration(Configuration defaultConfiguration, TestClass testClass, Annotatable testMethod) {
            apply(defaultConfiguration);

            applyAll(testClass);
            applyAll(testMethod);
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
        }

        private void apply(Configuration configuration) {
            if(configuration == null)
                return;
            profile = configuration.profile();
            width = configuration.width();
            height = configuration.height();
            fps = configuration.fps();
            iterations = configuration.iterations();
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
    }
}
