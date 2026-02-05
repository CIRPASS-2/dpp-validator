/*
 * Copyright 2024-2027 CIRPASS-2
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
package it.extrared.dpp.validator.mocks;

import io.quarkus.arc.Unremovable;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.SqlConnection;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.function.Function;
import org.mockito.Mockito;

@ApplicationScoped
@Unremovable
public class MockPool extends Pool {

    public MockPool() {
        super(Mockito.mock(io.vertx.sqlclient.Pool.class));
    }

    @Override
    public <T> Uni<T> withTransaction(Function<SqlConnection, Uni<T>> function) {
        return function.apply(Mockito.mock(SqlConnection.class));
    }

    @Override
    public <T> Uni<T> withConnection(Function<SqlConnection, Uni<T>> function) {
        return function.apply(Mockito.mock(SqlConnection.class));
    }
}
