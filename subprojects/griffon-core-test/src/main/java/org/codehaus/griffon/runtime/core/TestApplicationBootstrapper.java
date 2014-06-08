/*
 * Copyright 2008-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.griffon.runtime.core;

import griffon.core.GriffonApplication;
import griffon.core.injection.Module;
import griffon.core.test.TestCaseAware;
import griffon.inject.BindTo;
import griffon.util.AnnotationUtils;
import org.codehaus.griffon.runtime.core.injection.AbstractModule;
import org.codehaus.griffon.runtime.core.injection.AnnotatedBindingBuilder;
import org.codehaus.griffon.runtime.core.injection.LinkedBindingBuilder;
import org.codehaus.griffon.runtime.core.injection.SingletonBindingBuilder;

import javax.annotation.Nonnull;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Qualifier;
import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static griffon.util.AnnotationUtils.named;
import static griffon.util.GriffonNameUtils.getPropertyName;
import static griffon.util.GriffonNameUtils.isBlank;

/**
 * @author Andres Almiray
 * @since 2.0.0
 */
public class TestApplicationBootstrapper extends DefaultApplicationBootstrapper implements TestCaseAware {
    private static final String METHOD_MODULES = "modules";
    private static final String METHOD_MODULE_OVERRIDES = "moduleOverrides";
    private Object testCase;

    public TestApplicationBootstrapper(@Nonnull GriffonApplication application) {
        super(application);
    }

    public void setTestCase(@Nonnull Object testCase) {
        this.testCase = testCase;
    }

    @Nonnull
    @Override
    protected List<Module> loadModules() {
        List<Module> modules = doCollectModulesFromMethod();
        if (!modules.isEmpty()) {
            return modules;
        }
        modules = super.loadModules();
        doCollectOverridingModules(modules);
        doCollectModulesFromInnerClasses(modules);
        doCollectModulesFromFields(modules);
        return modules;
    }


    @SuppressWarnings("unchecked")
    private List<Module> doCollectModulesFromMethod() {
        Method method = null;
        try {
            method = testCase.getClass().getDeclaredMethod(METHOD_MODULES);
            method.setAccessible(true);
        } catch (NoSuchMethodException e) {
            return Collections.emptyList();
        }

        try {
            return (List<Module>) method.invoke(testCase);
        } catch (Exception e) {
            throw new IllegalArgumentException("An error occurred while initializing modules from " + testCase.getClass().getName() + "." + METHOD_MODULES, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void doCollectOverridingModules(final @Nonnull Collection<Module> modules) {
        Method method = null;
        try {
            method = testCase.getClass().getDeclaredMethod(METHOD_MODULE_OVERRIDES);
            method.setAccessible(true);
        } catch (NoSuchMethodException e) {
            return;
        }

        try {
            List<Module> overrides = (List<Module>) method.invoke(testCase);
            modules.addAll(overrides);
        } catch (Exception e) {
            throw new IllegalArgumentException("An error occurred while initializing modules from " + testCase.getClass().getName() + "." + METHOD_MODULE_OVERRIDES, e);
        }
    }

    private void doCollectModulesFromInnerClasses(final @Nonnull Collection<Module> modules) {
        modules.add(new InnerClassesModule());
    }

    private void doCollectModulesFromFields(final @Nonnull Collection<Module> modules) {
        modules.add(new FieldsModule());
    }

    @Nonnull
    protected List<Annotation> harvestQualifiers(@Nonnull Class<?> clazz) {
        List<Annotation> list = new ArrayList<>();
        Annotation[] annotations = clazz.getAnnotations();
        for (Annotation annotation : annotations) {
            if (AnnotationUtils.isAnnotatedWith(annotation, Qualifier.class)) {
                if (BindTo.class.isAssignableFrom(annotation.getClass())) {
                    continue;
                }

                // special case for @Named
                if (Named.class.isAssignableFrom(annotation.getClass())) {
                    Named named = (Named) annotation;
                    if (isBlank(named.value())) {
                        list.add(named(getPropertyName(clazz)));
                        continue;
                    }
                }
                list.add(annotation);
            }
        }
        return list;
    }

    @Nonnull
    protected List<Annotation> harvestQualifiers(@Nonnull Field field) {
        List<Annotation> list = new ArrayList<>();
        Annotation[] annotations = field.getAnnotations();
        for (Annotation annotation : annotations) {
            if (AnnotationUtils.isAnnotatedWith(annotation, Qualifier.class)) {
                if (BindTo.class.isAssignableFrom(annotation.getClass())) {
                    continue;
                }

                // special case for @Named
                if (Named.class.isAssignableFrom(annotation.getClass())) {
                    Named named = (Named) annotation;
                    if (isBlank(named.value())) {
                        list.add(named(getPropertyName(field.getName())));
                        continue;
                    }
                }
                list.add(annotation);
            }
        }
        return list;
    }

    private class InnerClassesModule extends AbstractModule {
        @Override
        @SuppressWarnings("unchecked")
        protected void doConfigure() {
            for (Class<?> clazz : testCase.getClass().getDeclaredClasses()) {
                BindTo bindTo = clazz.getAnnotation(BindTo.class);
                if (bindTo == null) continue;
                List<Annotation> qualifiers = harvestQualifiers(clazz);
                Annotation classifier = qualifiers.isEmpty() ? null : qualifiers.get(0);
                boolean isSingleton = clazz.getAnnotation(Singleton.class) != null;

                AnnotatedBindingBuilder<?> abuilder = bind(bindTo.value());
                if (classifier != null) {
                    LinkedBindingBuilder<?> lbuilder = abuilder.withClassifier(classifier);
                    if (Provider.class.isAssignableFrom(clazz)) {
                        SingletonBindingBuilder<?> sbuilder = lbuilder.toProvider((Class) clazz);
                        if (isSingleton) sbuilder.asSingleton();
                    } else {
                        SingletonBindingBuilder<?> sbuilder = lbuilder.to((Class) clazz);
                        if (isSingleton) sbuilder.asSingleton();
                    }
                } else {
                    if (Provider.class.isAssignableFrom(clazz)) {
                        SingletonBindingBuilder<?> sbuilder = abuilder.toProvider((Class) clazz);
                        if (isSingleton) sbuilder.asSingleton();
                    } else {
                        SingletonBindingBuilder<?> sbuilder = abuilder.to((Class) clazz);
                        if (isSingleton) sbuilder.asSingleton();
                    }
                }
            }
        }
    }

    private class FieldsModule extends AbstractModule {
        @Override
        @SuppressWarnings("unchecked")
        protected void doConfigure() {
            for (Field field : testCase.getClass().getDeclaredFields()) {
                BindTo bindTo = field.getAnnotation(BindTo.class);
                if (bindTo == null) continue;
                List<Annotation> qualifiers = harvestQualifiers(field);
                Annotation classifier = qualifiers.isEmpty() ? null : qualifiers.get(0);
                boolean isSingleton = field.getAnnotation(Singleton.class) != null;

                field.setAccessible(true);
                Object instance = null;
                try {
                    instance = field.get(testCase);
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException(e);
                }

                if (instance != null) {
                    AnnotatedBindingBuilder<Object> abuilder = (AnnotatedBindingBuilder<Object>) bind(bindTo.value());
                    if (classifier != null) {
                        if (Provider.class.isAssignableFrom(instance.getClass())) {
                            SingletonBindingBuilder<?> sbuilder = abuilder
                                .withClassifier(classifier)
                                .toProvider((Provider<Object>) instance);
                            if (isSingleton) sbuilder.asSingleton();
                        } else {
                            abuilder.withClassifier(classifier).toInstance(instance);
                        }
                    } else if (Provider.class.isAssignableFrom(instance.getClass())) {
                        SingletonBindingBuilder<?> sbuilder = abuilder.toProvider((Provider<Object>) instance);
                        if (isSingleton) sbuilder.asSingleton();
                    } else {
                        abuilder.toInstance(instance);
                    }
                } else {
                    AnnotatedBindingBuilder<?> abuilder = bind(bindTo.value());
                    if (classifier != null) {
                        LinkedBindingBuilder<?> lbuilder = abuilder.withClassifier(classifier);
                        if (Provider.class.isAssignableFrom(field.getType())) {
                            SingletonBindingBuilder<?> sbuilder = lbuilder.toProvider((Class) field.getType());
                            if (isSingleton) sbuilder.asSingleton();
                        } else {
                            SingletonBindingBuilder<?> sbuilder = lbuilder.to((Class) field.getType());
                            if (isSingleton) sbuilder.asSingleton();
                        }
                    } else {
                        if (Provider.class.isAssignableFrom(field.getType())) {
                            SingletonBindingBuilder<?> sbuilder = abuilder.toProvider((Class) field.getType());
                            if (isSingleton) sbuilder.asSingleton();
                        } else {
                            SingletonBindingBuilder<?> sbuilder = abuilder.to((Class) field.getType());
                            if (isSingleton) sbuilder.asSingleton();
                        }
                    }
                }
            }
        }
    }
}
