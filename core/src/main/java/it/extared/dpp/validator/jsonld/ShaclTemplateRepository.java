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
package it.extared.dpp.validator.jsonld;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.SqlConnection;
import it.extared.dpp.validator.dto.MatchResult;
import it.extared.dpp.validator.dto.PagedResult;
import it.extared.dpp.validator.dto.ResourceMetadata;
import it.extared.dpp.validator.dto.SearchDto;
import it.extared.dpp.validator.jsonld.dto.InputJsonLdMetadata;
import it.extared.dpp.validator.jsonld.dto.ShaclShapeMetadata;
import java.util.List;

/** Base interface for a SHACL template repository. */
public interface ShaclTemplateRepository {

    /**
     * Search templates by parameters provided as a {@link SearchDto}. Returns the metadata
     * associated to them as a {@link PagedResult<ResourceMetadata>}.
     *
     * @param conn the SQL connection.
     * @param searchDto the parameters to search by.
     * @return the search result.
     */
    Uni<PagedResult<ResourceMetadata>> search(SqlConnection conn, SearchDto searchDto);

    /**
     * Delete a template by its unique numeric identifier.
     *
     * @param conn the SQL connection.
     * @param id the unique numeric identifier of the template to delete.
     * @return nothing
     */
    Uni<Void> deleteTemplate(SqlConnection conn, Long id);

    /**
     * Find a best match for the metadata extracted from a JSON-LD meant to be validated as a {@link
     * MatchResult<String>}
     *
     * @param conn the SQL connection.
     * @param jsonLdMetadata the metadata extracted from the input JSON-LD
     * @return the match result containing the template itself.
     */
    Uni<MatchResult<String>> findBestMatch(SqlConnection conn, InputJsonLdMetadata jsonLdMetadata);

    /**
     * Find a template by its associated name and version, as a {@link MatchResult<String>}
     *
     * @param conn the SQL connection.
     * @param name the name associated to the template.
     * @param version the version associated to the template.
     * @return the match result containing the template itself.
     */
    Uni<MatchResult<String>> findByNameAndVersion(SqlConnection conn, String name, String version);

    /**
     * Persists a shacl template content together with its resource metadata and content metadata.
     *
     * @param conn the SQL connection.
     * @param resourceMetadata the resource metadata.
     * @param metadataList the metadata of the SHACL shapes.
     * @param template the SHACL template.
     * @return the unique numeric identifier of the template.
     */
    Uni<Long> addShaclTemplate(
            SqlConnection conn,
            ResourceMetadata resourceMetadata,
            List<ShaclShapeMetadata> metadataList,
            String template);

    /**
     * Retrieve a SHACL template as a string.
     *
     * @param connection the SQL connection.
     * @param id the unique numeric id of the SHACL template.
     * @return the SHACL template as a string.
     */
    Uni<String> findById(SqlConnection connection, Long id);
}
