/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.core.statement;

import java.sql.SQLException;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.v3.core.junit5.PgDatabaseExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class TestBatchExceptionRewrite {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public PgDatabaseExtension pgExtension = PgDatabaseExtension.instance(pg);

    @BeforeEach
    public void createTable() {
        pgExtension.getJdbi().useHandle(h -> h.execute("create table something (id int primary key, name varchar(50), integerValue integer, intValue integer)"));
    }

    @Test
    public void testSimpleBatch() {
        Batch b = pgExtension.openHandle().createBatch();
        b.add("insert into something (id, name) values (0, 'Keith')");
        b.add("insert into something (id, name) values (0, 'Keith')");
        assertThatExceptionOfType(UnableToExecuteStatementException.class)
                .isThrownBy(b::execute)
                .satisfies(e -> assertSuppressions(e.getCause()));
    }

    @Test
    public void testPreparedBatch() {
        PreparedBatch b = pgExtension.openHandle().prepareBatch("insert into something (id, name) values (?,?)");
        b.add(0, "a");
        b.add(0, "a");
        assertThatExceptionOfType(UnableToExecuteStatementException.class)
                .isThrownBy(b::execute)
                .satisfies(e -> assertSuppressions(e.getCause()));
    }

    private void assertSuppressions(Throwable cause) {
        LoggerFactory.getLogger(TestBatchExceptionRewrite.class).info("exception", cause);
        SQLException e = (SQLException) cause;
        SQLException nextException = e.getNextException();
        assertThat((Exception) nextException).isEqualTo(e.getSuppressed()[0]);
        assertThat((Exception) nextException.getNextException()).isNull();
        assertThat(e.getSuppressed()).hasSize(1);
    }
}
