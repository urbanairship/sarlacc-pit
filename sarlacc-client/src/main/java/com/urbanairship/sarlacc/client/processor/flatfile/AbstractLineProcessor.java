package com.urbanairship.sarlacc.client.processor.flatfile;

import com.google.common.io.LineProcessor;

import java.io.IOException;

/**
 * The various readLines() methods which take a LineProcessor will halt and return
 * if processLine() ever returns false. This seems like a potential source of bugs,
 * so require a thin wrapper.
 */
public abstract class AbstractLineProcessor<T> implements LineProcessor<T> {
    @Override
    public final boolean processLine(String line) throws IOException {
        process(line);
        return true;
    }

    public abstract void process(String line);

    public abstract T getResult();
}
