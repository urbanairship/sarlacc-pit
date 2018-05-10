package com.urbanairship.sarlacc.client.functional;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Service;
import com.urbanairship.sarlacc.client.structures.container.UpdatingList;
import com.urbanairship.sarlacc.client.structures.container.UpdatingMap;
import com.urbanairship.sarlacc.client.structures.container.UpdatingSet;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@SuppressWarnings("Duplicates")
public class TestServiceStateChecks {

    @Test
    @SuppressWarnings("unchecked")
    public void testMapUnstarted() throws Exception {
        final Service updateService = Mockito.mock(Service.class);
        final Map<String, String> backingMap = Mockito.spy(
                new HashMap<>(ImmutableMap.of(RandomStringUtils.randomAlphanumeric(10), RandomStringUtils.randomAlphanumeric(10))));
        final UpdatingMap<String, String> updatingMap = new UpdatingMap<>(new AtomicReference<>(backingMap));
        updatingMap.setUpdateService(updateService);

        Mockito.when(updateService.state()).thenReturn(Service.State.NEW);

        for (Method method : UpdatingMap.class.getDeclaredMethods()) {
            if (!shouldCheck(method)) {
                continue;
            }
            
            final Object[] arguments = getArguments(method.getParameters());

            try {
                method.invoke(updatingMap, arguments);
                Assert.fail("Expected call to throw, but it didn't! Method: " + method.getName());
            } catch (Throwable t) {
                Assert.assertTrue(String.format("Unexpected exception for method '%s': %s ", method.getName(), Throwables.getStackTraceAsString(t)),
                        t.getCause() instanceof IllegalStateException || t.getCause() instanceof UnsupportedOperationException);
            }
        }

        Mockito.verify(updateService, Mockito.atLeast(1)).state();
        Mockito.verifyNoMoreInteractions(updateService);
        Mockito.verifyZeroInteractions(backingMap);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMapStarted() throws Exception {
        final Service updateService = Mockito.mock(Service.class);
        final Map<String, String> backingMap = Mockito.spy(
                new HashMap<>(ImmutableMap.of(RandomStringUtils.randomAlphanumeric(10), RandomStringUtils.randomAlphanumeric(10))));
        final UpdatingMap<String, String> updatingMap = new UpdatingMap<>(new AtomicReference<>(backingMap));
        updatingMap.setUpdateService(updateService);

        Mockito.when(updateService.state()).thenReturn(Service.State.RUNNING);

        for (Method method : UpdatingMap.class.getDeclaredMethods()) {
            if (!shouldCheck(method)) {
                continue;
            }
            
            final Object[] arguments = getArguments(method.getParameters());

            try {
                method.invoke(updatingMap, arguments);
            } catch (Throwable t) {
                Assert.assertTrue("Unexpected exception: " + Throwables.getStackTraceAsString(t),
                        t.getCause() instanceof UnsupportedOperationException);
            }
        }

        Mockito.verify(updateService, Mockito.atLeast(1)).state();
        Mockito.verifyNoMoreInteractions(updateService);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testListUnstarted() throws Exception {
        final Service updateService = Mockito.mock(Service.class);
        final List<String> backingList = Mockito.spy(
                new ArrayList<>(ImmutableList.of(RandomStringUtils.randomAlphanumeric(10))));
        final UpdatingList<String> updatingList = new UpdatingList<>(new AtomicReference<>(backingList));
        updatingList.setUpdateService(updateService);

        Mockito.when(updateService.state()).thenReturn(Service.State.NEW);

        for (Method method : UpdatingList.class.getDeclaredMethods()) {
            if (!shouldCheck(method)) {
                continue;
            }
            
            final Object[] arguments = getArguments(method.getParameters());

            try {
                method.invoke(updatingList, arguments);
                Assert.fail("Expected call to throw, but it didn't! Method: " + method.getName());
            } catch (Throwable t) {
                Assert.assertTrue("Unexpected exception: " + Throwables.getStackTraceAsString(t),
                        t.getCause() instanceof IllegalStateException || t.getCause() instanceof UnsupportedOperationException);
            }
        }

        Mockito.verify(updateService, Mockito.atLeast(1)).state();
        Mockito.verifyNoMoreInteractions(updateService);
        Mockito.verifyZeroInteractions(backingList);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testListStarted() throws Exception {
        final Service updateService = Mockito.mock(Service.class);
        final List<String> backingList = Mockito.spy(
                new ArrayList<>(ImmutableList.of(RandomStringUtils.randomAlphanumeric(10))));
        final UpdatingList<String> updatingList = new UpdatingList<>(new AtomicReference<>(backingList));
        updatingList.setUpdateService(updateService);

        Mockito.when(updateService.state()).thenReturn(Service.State.RUNNING);

        for (Method method : UpdatingList.class.getDeclaredMethods()) {
            if (!shouldCheck(method)) {
                continue;
            }
            
            final Object[] arguments = getArguments(method.getParameters());
            
            try {
                method.invoke(updatingList, arguments);
            } catch (Throwable t) {
                Assert.assertTrue("Unexpected exception: " + Throwables.getStackTraceAsString(t),
                        t.getCause() instanceof UnsupportedOperationException);
            }
        }

        Mockito.verify(updateService, Mockito.atLeast(1)).state();
        Mockito.verifyNoMoreInteractions(updateService);
        Mockito.verify(backingList, Mockito.times(1)).contains(Mockito.anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetUnstarted() throws Exception {
        final Service updateService = Mockito.mock(Service.class);
        final Set<String> backingSet = Mockito.spy(
                new HashSet<>(ImmutableSet.of(RandomStringUtils.randomAlphanumeric(10))));
        final UpdatingSet<String> updatingSet = new UpdatingSet<>(new AtomicReference<>(backingSet));
        updatingSet.setUpdateService(updateService);

        Mockito.when(updateService.state()).thenReturn(Service.State.NEW);

        for (Method method : UpdatingSet.class.getDeclaredMethods()) {
            if (!shouldCheck(method)) {
                continue;
            }
            
            final Object[] arguments = getArguments(method.getParameters());
            try {
                method.invoke(updatingSet, arguments);
                Assert.fail("Expected call to throw, but it didn't! Method: " + method.getName());
            } catch (Throwable t) {
                Assert.assertTrue("Unexpected exception: " + Throwables.getStackTraceAsString(t),
                        t.getCause() instanceof IllegalStateException || t.getCause() instanceof UnsupportedOperationException);
            }
        }

        Mockito.verify(updateService, Mockito.atLeast(1)).state();
        Mockito.verifyNoMoreInteractions(updateService);
        Mockito.verifyZeroInteractions(backingSet);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetStarted() throws Exception {
        final Service updateService = Mockito.mock(Service.class);
        final Set<String> backingSet = Mockito.spy(
                new HashSet<>(ImmutableSet.of(RandomStringUtils.randomAlphanumeric(10))));
        final UpdatingSet<String> updatingSet = new UpdatingSet<>(new AtomicReference<>(backingSet));
        updatingSet.setUpdateService(updateService);

        Mockito.when(updateService.state()).thenReturn(Service.State.RUNNING);

        for (Method method : UpdatingSet.class.getDeclaredMethods()) {
            if (!shouldCheck(method)) {
                continue;
            }
            
            final Object[] arguments = getArguments(method.getParameters());

            try {
                method.invoke(updatingSet, arguments);
            } catch (Throwable t) {
                Assert.assertTrue("Unexpected exception: " + Throwables.getStackTraceAsString(t),
                        t.getCause() instanceof UnsupportedOperationException);
            }
        }

        Mockito.verify(updateService, Mockito.atLeast(1)).state();
        Mockito.verifyNoMoreInteractions(updateService);

        Mockito.verify(backingSet, Mockito.times(1)).contains(Mockito.anyString());
    }

    private Object[] getArguments(Parameter[] parameters) {
        return Stream.of(parameters)
                .map(parameter -> {
                    final Class<?> type = parameter.getType();
                    if (String.class.equals(type)) {
                        return "";
                    } else if (Integer.class.equals(type) || Integer.TYPE.equals(type)) {
                        return 0;
                    } else if (Map.class.equals(type)) {
                        return ImmutableMap.of();
                    } else if (List.class.equals(type)) {
                        return ImmutableList.of();
                    } else if (Set.class.equals(type)) {
                        return ImmutableSet.of();
                    } else if (Object.class.equals(type)) {
                        return new Object();
                    } else if (Object[].class.equals(type)) {
                        return new Object[0];
                    } else if (Collection.class.equals(type)) {
                        return ImmutableSet.of();
                    } else {
                        throw new RuntimeException("Unhandled parameter class: " + type.getCanonicalName());
                    }
                }).toArray();
    }

    private boolean shouldCheck(Method method) {
        final int modifiers = method.getModifiers();
        return Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers);
    }
}
