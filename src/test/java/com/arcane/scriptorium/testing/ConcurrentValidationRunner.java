package com.arcane.scriptorium.testing;

import com.arcane.scriptorium.synchronization.ArcaneSynchronizationCoordinatorTest;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Small local runner for environments without Maven or the JUnit Platform Console.
 *
 * <p>The tests themselves are regular JUnit 5 tests. This class only invokes
 * zero-argument methods annotated with {@link Test} or {@link RepeatedTest},
 * which lets the validation suite run inside this workspace when no build tool
 * is installed.</p>
 */
public final class ConcurrentValidationRunner {
    private ConcurrentValidationRunner() {
    }

    public static void main(String[] args) throws Exception {
        List<Failure> failures = new ArrayList<>();
        runClass(ArcaneSynchronizationCoordinatorTest.class, failures);

        if (!failures.isEmpty()) {
            failures.forEach(failure -> {
                System.err.println("[FAIL] " + failure.name());
                failure.cause().printStackTrace(System.err);
            });
            throw new AssertionError(failures.size() + " validation test(s) failed.");
        }

        System.out.println("All concurrency validation tests passed.");
    }

    private static void runClass(Class<?> testClass, List<Failure> failures) throws Exception {
        List<Method> methods = List.of(testClass.getDeclaredMethods()).stream()
                .filter(method -> method.isAnnotationPresent(Test.class)
                        || method.isAnnotationPresent(RepeatedTest.class))
                .sorted(Comparator.comparing(Method::getName))
                .toList();

        for (Method method : methods) {
            int repetitions = method.isAnnotationPresent(RepeatedTest.class)
                    ? method.getAnnotation(RepeatedTest.class).value()
                    : 1;
            for (int repetition = 1; repetition <= repetitions; repetition++) {
                Object instance = testClass.getDeclaredConstructor().newInstance();
                method.setAccessible(true);
                String displayName = testClass.getSimpleName() + "." + method.getName()
                        + "[" + repetition + "/" + repetitions + "]";
                try {
                    method.invoke(instance);
                    System.out.println("[PASS] " + displayName);
                } catch (InvocationTargetException exception) {
                    failures.add(new Failure(displayName, exception.getCause()));
                }
            }
        }
    }

    private record Failure(String name, Throwable cause) {
    }
}
