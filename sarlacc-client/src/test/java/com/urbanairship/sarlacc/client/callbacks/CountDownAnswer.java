package com.urbanairship.sarlacc.client.callbacks;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.CountDownLatch;

final class CountDownAnswer<T> implements Answer<T> {
    private final CountDownLatch latch;
    private final T toReturn;

    CountDownAnswer(CountDownLatch latch, T toReturn) {
        this.latch = latch;
        this.toReturn = toReturn;
    }

    @Override
    public T answer(InvocationOnMock invocation) throws Throwable {
        latch.countDown();
        return toReturn;
    }
}
