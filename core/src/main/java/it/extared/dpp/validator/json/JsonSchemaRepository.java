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
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.SqlConnection;
import it.extared.dpp.validator.dto.MatchResult;
import it.extared.dpp.validator.dto.PagedResult;
import it.extared.dpp.validator.dto.ResourceMetadata;
import it.extared.dpp.validator.dto.SearchDto;
import it.extared.dpp.validator.json.dto.SchemaMetadata;

/** Base interface for a repository of JSON schema validation resources. */
public interface JsonSchemaRepository {

    /**
     * Search the schemas according to the input parameters, and returns the associated metadata as
     * a {@link PagedResult<ResourceMetadata>}
     *
     * @param conn the SQL connection.
     * @param searchDto an object containing search parameters.
     * @return results of the search.
     */
    Uni<PagedResult<ResourceMetadata>> search(SqlConnection conn, SearchDto searchDto);

    /**
     * @param connection the SQL connection.
     * @param resMetadata the resource metadata of the schema to save (name,description,version) as
     *     a {@link ResourceMetadata}.
     * @param metadata the metadata of the schema content to persist as a {@link SchemaMetadata}.
     * @param schema the schema content as a {@link JsonNode}
     * @return the unique numeric identifier of the schema.
     */
    Uni<Long> addJsonSchema(
            SqlConnection connection,
            ResourceMetadata resMetadata,
            SchemaMetadata metadata,
            JsonNode schema);

    /**
     * Find the best matching schema given an array of json properties coming from a JSON to
     * validate. The result is returned as a {@link MatchResult<JsonNode>}.
     *
     * @param connection the SQL connection.
     * @param jsonProperties the json properties to match the schema by.
     * @return the match result containing the schema itself.
     */
    Uni<MatchResult<JsonNode>> findBestMatch(SqlConnection connection, String[] jsonProperties);

    /**
     * Find a schema by name and version. Returns it as a {@link MatchResult<JsonNode>}
     *
     * @param connection the SQL connection.
     * @param name the name associated to the schema.
     * @param version the version associated to the schema.
     * @return the match result containing the schema itself.
     */
    Uni<MatchResult<JsonNode>> findByNameAndVersion(
            SqlConnection connection, String name, String version);

    /**
     * Delete a schema by its unique numeric identifier.
     *
     * @param connection the SQL connection.
     * @param id the unique numeric identifier of the schema.
     * @return nothing.
     */
    Uni<Void> deleteSchema(SqlConnection connection, Long id);

    /**
     * Retrieve a schema by its unique numeric identifier.
     *
     * @param connection SQL connection.
     * @param id the unique numeric identifier of the schema to be retrieved.
     * @return the retrieved schema as a raw JSON string.
     */
    Uni<String> findById(SqlConnection connection, Long id);
}
