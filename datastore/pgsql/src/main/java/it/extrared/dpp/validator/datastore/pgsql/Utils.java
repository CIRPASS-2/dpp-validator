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
package it.extrared.dpp.validator.datastore.pgsql;

import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import it.extared.dpp.validator.dto.PagedResult;
import it.extared.dpp.validator.dto.ResourceMetadata;
import java.util.List;
import java.util.function.Function;

public class Utils {

    public static String asLikeParam(String param) {
        return "%" + param.toUpperCase() + "%";
    }

    /**
     * Converts a row set to a Page Result
     *
     * @param count number of elements matched by a count.
     * @param pageSize the max number of element in the page.
     * @param rows the row set.
     * @param mapper a mapper function.
     * @return
     */
    public static PagedResult<ResourceMetadata> asPagedResult(
            Long count,
            Integer pageSize,
            RowSet<Row> rows,
            Function<Row, ResourceMetadata> mapper) {
        List<ResourceMetadata> metas = rows.stream().map(mapper).toList();
        PagedResult.Builder<ResourceMetadata> builder = PagedResult.builder();
        return builder.withElements(metas).withTotalElements(count).withPageSize(pageSize).build();
    }
}
