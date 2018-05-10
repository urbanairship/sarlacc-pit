package com.urbanairship.sarlacc.client.processor;

import com.urbanairship.sarlacc.client.processor.flatfile.ImmutableArrayStringSetLineProcessor;
import com.urbanairship.sarlacc.client.structures.impl.ImmutableArrayStringSet;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ImmutableArrayStringSetLineProcessorTest {

    @Test
    public void TestGetResultBuildsOnce() {
        ImmutableArrayStringSetLineProcessor immutableArrayStringSetLineProcessor = new ImmutableArrayStringSetLineProcessor();

        immutableArrayStringSetLineProcessor.process("foo");
        immutableArrayStringSetLineProcessor.process("bar");

        ImmutableArrayStringSet result = immutableArrayStringSetLineProcessor.getResult();
        ImmutableArrayStringSet result2 = immutableArrayStringSetLineProcessor.getResult();

        assertTrue(result == result2);
    }
}
