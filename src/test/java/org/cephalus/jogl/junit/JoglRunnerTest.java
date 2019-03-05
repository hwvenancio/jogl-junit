package org.cephalus.jogl.junit;

import com.jogamp.opengl.GLAutoDrawable;
import org.cephalus.jogl.Iterations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.internal.requests.ClassRequest;
import org.junit.internal.requests.FilterRequest;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.notification.Failure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class JoglRunnerTest {

    private static Map<Class<?>, List<String>> methodCalls = new HashMap<>();

    @Test
    public void fullyAnnotated() {
        TestRun run = runTestClass(FullyAnnotatedTest.class);

        assertThat(run.methods)
                .containsExactly("beforeClass", "before", "test1", "after", "before", "test2", "after", "afterClass");
    }

    @Test
    public void filterFullyAnnotated() {
        Filter filter = new Filter() {
            @Override
            public boolean shouldRun(Description description) {
                return "test2".equals(description.getMethodName());
            }

            @Override
            public String describe() {
                return "Custom Filter";
            }
        };

        TestRun run = runTestClass(FullyAnnotatedTest.class, filter);

        assertThat(run.methods)
                .containsExactly("beforeClass", "before", "test2", "after", "afterClass");
    }

    @Test
    public void classIgnored() {
        TestRun run = runTestClass(ClassIgnoredTest.class);

        assertThat(run.methods)
                .isEmpty();
    }

    @Test
    public void singleIgnored() {
        TestRun run = runTestClass(SingleIgnoredTest.class);

        assertThat(run.methods)
                .containsExactly("beforeClass", "before", "test1", "after", "afterClass");
    }

    @Test
    public void exceptions() {
        TestRun run = runTestClass(ExceptionTest.class);

        assertThat(run.result.getFailures())
                .hasSize(2);
        assertThat(run.result.getFailures())
                .extracting(f -> f.getDescription().getMethodName())
                .contains("test1", "test3");
        assertThat(run.result.getFailures())
                .extracting(Failure::getException)
                .hasAtLeastOneElementOfType(RuntimeException.class)
                .hasAtLeastOneElementOfType(AssertionError.class);
    }

    private static TestRun runTestClass(Class<?> testClass) {
        return runTestClass(testClass, null);
    }

    private static synchronized TestRun runTestClass(Class<?> testClass, Filter filter) {
        methodCalls.put(testClass, new ArrayList<>());

        JUnitCore junit = new JUnitCore();

        Request request = new ClassRequest(testClass);
        if(filter != null)
            request = new FilterRequest(request, filter);

        Result result = junit.run(request);

        return new TestRun(result, methodCalls.remove(testClass));
    }

    public static class TestRun {
        public final Result result;
        public final List<String> methods;

        public TestRun(Result result, List<String> methods) {
            this.result = result;
            this.methods = methods;
        }
    }

    @RunWith(JoglRunner.class)
    @Iterations(1)
    public static class FullyAnnotatedTest {

        private static void add(String methodName) {
            methodCalls.get(FullyAnnotatedTest.class).add(methodName);
        }

        @BeforeClass
        public static void beforeClass() {
            add("beforeClass");
        }

        @AfterClass
        public static void afterClass() {
            add("afterClass");
        }

        @Before
        public void before(GLAutoDrawable drawable) {
            add("before");
        }

        @After
        public void after(GLAutoDrawable drawable) {
            add("after");
        }

        @Test
        public void test1(GLAutoDrawable drawable) {
            add("test1");
        }

        @Test
        public void test2(GLAutoDrawable drawable) {
            add("test2");
        }
    }

    @RunWith(JoglRunner.class)
    @Iterations(1)
    @Ignore
    public static class ClassIgnoredTest {

        private static void add(String methodName) {
            methodCalls.get(ClassIgnoredTest.class).add(methodName);
        }

        @BeforeClass
        public static void beforeClass() {
            add("beforeClass");
        }

        @AfterClass
        public static void afterClass() {
            add("afterClass");
        }

        @Before
        public void before(GLAutoDrawable drawable) {
            add("before");
        }

        @After
        public void after(GLAutoDrawable drawable) {
            add("after");
        }

        @Test
        public void test1(GLAutoDrawable drawable) {
            add("test1");
        }

        @Test
        public void test2(GLAutoDrawable drawable) {
            add("test2");
        }
    }

    @RunWith(JoglRunner.class)
    @Iterations(1)
    public static class SingleIgnoredTest {

        private static void add(String methodName) {
            methodCalls.get(SingleIgnoredTest.class).add(methodName);
        }

        @BeforeClass
        public static void beforeClass() {
            add("beforeClass");
        }

        @AfterClass
        public static void afterClass() {
            add("afterClass");
        }

        @Before
        public void before(GLAutoDrawable drawable) {
            add("before");
        }

        @After
        public void after(GLAutoDrawable drawable) {
            add("after");
        }

        @Test
        public void test1(GLAutoDrawable drawable) {
            add("test1");
        }

        @Test
        @Ignore
        public void test2(GLAutoDrawable drawable) {
            add("test2");
        }
    }

    @RunWith(JoglRunner.class)
    @Iterations(1)
    public static class ExceptionTest {

        private static void add(String methodName) {
            methodCalls.get(ExceptionTest.class).add(methodName);
        }

        @BeforeClass
        public static void beforeClass() {
            add("beforeClass");
        }

        @AfterClass
        public static void afterClass() {
            add("afterClass");
        }

        @Before
        public void before(GLAutoDrawable drawable) {
            add("before");
        }

        @After
        public void after(GLAutoDrawable drawable) {
            add("after");
        }

        @Test(expected = RuntimeException.class)
        public void test1(GLAutoDrawable drawable) {
            add("test1");
        }

        @Test(expected = RuntimeException.class)
        public void test2(GLAutoDrawable drawable) {
            add("test2");
            throw new RuntimeException();
        }

        @Test
        public void test3(GLAutoDrawable drawable) {
            add("test3");
            throw new RuntimeException();
        }
    }
}
