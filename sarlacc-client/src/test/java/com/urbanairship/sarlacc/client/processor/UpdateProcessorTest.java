package com.urbanairship.sarlacc.client.processor;


import com.urbanairship.sarlacc.client.processor.flatfile.AbstractLineProcessor;
import com.urbanairship.sarlacc.client.processor.flatfile.MultilinePlaintextUpdateProcessor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertEquals;

public class UpdateProcessorTest {
    private MultilinePlaintextUpdateProcessor<String> updateProcessor;
    private AbstractLineProcessor<String> lineProcessor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        lineProcessor = new AbstractLineProcessor<String>() {
            private String latest = null;

            @Override
            public void process(String line) {
                latest = line;
            }

            @Override
            public String getResult() {
                return latest;
            }
        };

        updateProcessor = new MultilinePlaintextUpdateProcessor<>(() -> lineProcessor);
    }

    @Test
    public void testProcess() throws Exception {
        updateProcessor.process(new ByteArrayInputStream("foo\nbar".getBytes("UTF-8")));

        assertEquals("bar", lineProcessor.getResult());
    }
}
