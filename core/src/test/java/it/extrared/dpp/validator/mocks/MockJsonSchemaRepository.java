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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.Unremovable;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.SqlConnection;
import it.extared.dpp.validator.dto.*;
import it.extared.dpp.validator.json.JsonSchemaRepository;
import it.extared.dpp.validator.json.dto.SchemaMetadata;
import it.extared.dpp.validator.utils.CommonUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@Unremovable
@ApplicationScoped
public class MockJsonSchemaRepository implements JsonSchemaRepository {

    @Inject ObjectMapper objectMapper;

    @Override
    public Uni<PagedResult<ResourceMetadata>> search(SqlConnection conn, SearchDto searchDto) {
        List<ResourceMetadata> resources =
                List.of(
                        new ResourceMetadata(1L, "res-1", "desc-1", "1.0"),
                        new ResourceMetadata(2L, "res-2", "desc-2", "1.0"),
                        new ResourceMetadata(3L, "res-3", "desc-3", "1.0"));
        PagedResult.Builder<ResourceMetadata> builder = PagedResult.builder();
        return Uni.createFrom()
                .item(
                        builder.withElements(resources)
                                .withPageSize(3)
                                .withTotalElements(3L)
                                .build());
    }

    @Override
    public Uni<Long> addJsonSchema(
            SqlConnection connection,
            ResourceMetadata resMetadata,
            SchemaMetadata metadata,
            JsonNode schema) {
        return Uni.createFrom().item(1L);
    }

    @Override
    public Uni<MatchResult<JsonNode>> findBestMatch(
            SqlConnection connection, String[] jsonProperties) {

        return Uni.createFrom()
                .item(CommonUtils.readJsonSchemaNode("test-schema-simple.json"))
                .map(
                        s ->
                                new MatchResult<>(
                                        "battery_passport",
                                        "1.0.0",
                                        s,
                                        MatchType.SIMILARITY_MATCH));
    }

    @Override
    public Uni<MatchResult<JsonNode>> findByNameAndVersion(
            SqlConnection connection, String name, String version) {
        return Uni.createFrom()
                .item(CommonUtils.readJsonSchemaNode("test-schema-simple.json"))
                .map(
                        s ->
                                new MatchResult<>(
                                        "battery_passport",
                                        "1.0.0",
                                        s,
                                        MatchType.NAME_AND_VERSION));
    }

    @Override
    public Uni<Void> deleteSchema(SqlConnection connection, Long id) {
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<String> findById(SqlConnection connection, Long id) {
        return Uni.createFrom()
                .item(new String(CommonUtils.readJsonSchemaBytes("test-schema-simple.json")));
    }
}
