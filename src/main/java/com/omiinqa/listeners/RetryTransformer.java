package com.omiinqa.listeners;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Applies {@link RetryAnalyzer} to every {@code @Test} automatically
 * (Decorator over TestNG's annotation metadata).
 *
 * <p>Without this, each test would need {@code retryAnalyzer = ...} repeated by
 * hand — error-prone and easy to forget. Registering the transformer in the
 * suite wires retry uniformly across hundreds of tests (DRY).</p>
 */
public final class RetryTransformer implements IAnnotationTransformer {

    @Override
    public void transform(final ITestAnnotation annotation,
                          final Class testClass,
                          final Constructor testConstructor,
                          final Method testMethod) {
        if (annotation.getRetryAnalyzerClass() == null
                || annotation.getRetryAnalyzerClass() == org.testng.internal.annotations.DisabledRetryAnalyzer.class) {
            annotation.setRetryAnalyzer(RetryAnalyzer.class);
        }
    }
}
