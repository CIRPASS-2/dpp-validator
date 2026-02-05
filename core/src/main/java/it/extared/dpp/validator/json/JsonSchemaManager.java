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
package it.extared.dpp.validator.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.vertx.mutiny.sqlclient.Pool;
import it.extared.dpp.validator.ValidationResourceManager;
import it.extared.dpp.validator.ValidationType;
import it.extared.dpp.validator.dto.*;
import it.extared.dpp.validator.json.dto.SchemaMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

@ApplicationScoped
public class JsonSchemaManager implements ValidationResourceManager {
    @Inject Pool pool;
    @Inject JsonSchemaRepository repository;
    @Inject ObjectMapper objectMapper;
    @Inject JsonSchemaMetadataExtractor extractor;

    @Override
    public Uni<Long> addValidationResource(ResourceMetadata resourceMetadata, InputStream resource)
            throws IOException {
        JsonNode node = objectMapper.readTree(resource);
        SchemaMetadata meta = extractor.extractMetadata(node);
        return pool.withTransaction(
                conn -> repository.addJsonSchema(conn, resourceMetadata, meta, node));
    }

    @Override
    public Uni<TypedResource> getByNameAndVersion(String name, String version) {
        Uni<MatchResult<JsonNode>> matchResult =
                pool.withConnection(c -> repository.findByNameAndVersion(c, name, version));
        return matchResult.map(
                Unchecked.function(
                        m ->
                                TypedResource.builder()
                                        .withResourceType(ValidationType.PLAIN_JSON)
                                        .withContent(
                                                objectMapper.writeValueAsString(m.getResource()))
                                        .build()));
    }

    @Override
    public Uni<Void> removeValidationResource(Long resourceId) {
        return pool.withTransaction(c -> repository.deleteSchema(c, resourceId));
    }

    @Override
    public Uni<PagedResult<ResourceMetadata>> search(SearchDto searchDto) {
        return pool.withConnection(c -> repository.search(c, searchDto));
    }

    @Override
    public Uni<TypedResource> getResourceById(Long id) {
        return pool.withConnection(conn -> repository.findById(conn, id))
                .map(
                        s ->
                                TypedResource.builder()
                                        .withContent(s)
                                        .withResourceType(ValidationType.PLAIN_JSON)
                                        .build());
    }

    @Override
    public boolean canHandle(ValidationType validationType) {
        return Objects.equals(ValidationType.PLAIN_JSON, validationType);
    }

    @Override
    public Integer priority() {
        return 99;
    }
}
