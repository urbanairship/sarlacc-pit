package com.urbanairship.sarlacc.client.processor.sql;

import java.sql.ResultSet;

public interface ResultSetProcessor<C> {
    void process(ResultSet result);
    C getDataStructure();
}
