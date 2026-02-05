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

import static it.extared.dpp.validator.utils.CommonUtils.debug;
import static it.extrared.dpp.validator.datastore.pgsql.Utils.asLikeParam;
import static it.extrared.dpp.validator.datastore.pgsql.Utils.asPagedResult;

import io.opentelemetry.api.internal.StringUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.SqlConnection;
import io.vertx.mutiny.sqlclient.Tuple;
import it.extared.dpp.validator.dto.*;
import it.extared.dpp.validator.exceptions.NotFoundException;
import it.extared.dpp.validator.jsonld.ShaclTemplateRepository;
import it.extared.dpp.validator.jsonld.dto.InputJsonLdMetadata;
import it.extared.dpp.validator.jsonld.dto.ShaclShapeMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.jboss.logging.Logger;

/** PgSQL implementation of a SHACL {@link ShaclTemplateRepository} */
@ApplicationScoped
public class PgSQLShaclTemplateRepository implements ShaclTemplateRepository {

    private static final String FIND_BY_ID =
            """
            SELECT sht.shacl_content FROM shacl_templates sht WHERE sht.id=$1
            """;

    private static final String FIND_BY_NAME_AND_VERSION =
            """
            SELECT 'NAME_AND_VERSION' as match_type, sht.template_name, sht.template_version, sht.shacl_content FROM shacl_templates sht WHERE sht.template_name=$1 AND sht.template_version=$2
            """;

    private static final String SELECT_SEARCH_TEMPLATES =
            """
            SELECT sht.id, sht.template_name,sht.description, sht.template_version, sht.context_uri FROM shacl_templates sht
            """;
    private static final String SELECT_COUNT_TEMPLATES =
            """
            SELECT COUNT(*) AS count FROM shacl_templates sht
            """;
    private static final String BEST_MATCH =
            """
                        SELECT * FROM (
                            SELECT
                                sht.template_name,
                                sht.template_version,
                                sht.shacl_content,
                                'EXACT_TYPE_MATCH' as match_type,
                                1.0 as score
                            FROM shacl_shapes shp INNER JOIN shacl_templates sht ON shp.template_id=sht.id
                            WHERE shp.target_class = $1

                            UNION ALL

                            SELECT
                                sht.template_name,
                                sht.template_version,
                                sht.shacl_content,
                                'CONTEXT_URI_MATCH' as match_type,
                                0.9 as score
                            FROM shacl_templates sht
                            WHERE context_uri = $2 AND context_uri IS NOT NULL

                            UNION ALL

                            SELECT
                                sht.template_name,
                                sht.template_version,
                                sht.shacl_content,
                                'VOCABULARY_MATCH' as match_type,
                                0.8 as score
                            FROM shacl_shapes shp INNER JOIN shacl_templates sht ON shp.template_id=sht.id
                            WHERE shp.vocabulary_uri = $3 AND shp.vocabulary_uri IS NOT NULL
                        ) all_matches
                        ORDER BY score DESC
                        LIMIT 1
            """;

    private static final String INSERT_SHACL_TEMPLATE =
            """
            INSERT INTO shacl_templates
                            (template_name,description, template_version, shacl_content,context_uri)
                        VALUES ($1, $2, $3, $4,$5)
            RETURNING id
            """;

    private static final String INSERT_SHAPES =
            """
            INSERT INTO shacl_shapes
                            (template_id, shape_id, target_class, vocabulary_uri, ontology_uri)
                        VALUES ($1, $2, $3, $4, $5)
            RETURNING id
            """;

    private static final String DELETE_TEMPLARE =
            """
            DELETE FROM shacl_templates where id=$1
            """;

    private static final Function<Row, MatchResult<String>> AS_MATCH_RESULT =
            r ->
                    new MatchResult<>(
                            r.getString("template_name"),
                            r.getString("template_version"),
                            r.getString("shacl_content"),
                            MatchType.valueOf(r.getString("match_type")));

    private static final Function<Row, ResourceMetadata> AS_RESULT_METADATA =
            r -> {
                TemplateResourceMetadata metadata = new TemplateResourceMetadata();
                metadata.setId(r.getLong("id"));
                metadata.setName(r.getString("template_name"));
                metadata.setDescription(r.getString("description"));
                metadata.setVersion(r.getString("template_version"));
                metadata.setContextUri(r.getString("context_uri"));
                return metadata;
            };

    private static final Logger LOGGER = Logger.getLogger(PgSQLShaclTemplateRepository.class);

    @Override
    public Uni<Void> deleteTemplate(SqlConnection conn, Long id) {
        debug(LOGGER, () -> "retrieving template with id %s".formatted(id));
        return conn.preparedQuery(DELETE_TEMPLARE).execute(Tuple.of(id)).replaceWithVoid();
    }

    @Override
    public Uni<MatchResult<String>> findBestMatch(
            SqlConnection conn, InputJsonLdMetadata jsonLdMetadata) {
        debug(
                LOGGER,
                () -> "finding best match for input json-ld metadata %s".formatted(jsonLdMetadata));
        Uni<RowSet<Row>> rows =
                conn.preparedQuery(BEST_MATCH)
                        .execute(
                                Tuple.of(
                                        jsonLdMetadata.getType(),
                                        jsonLdMetadata.getContextUri(),
                                        jsonLdMetadata.getVocabularyUri()));
        return rows.map(
                r -> r.stream().findFirst().map(AS_MATCH_RESULT).orElse(MatchResult.emptyResult()));
    }

    @Override
    public Uni<MatchResult<String>> findByNameAndVersion(
            SqlConnection conn, String name, String version) {
        debug(
                LOGGER,
                () -> "retrieving template by name %s and version %s".formatted(name, version));
        Uni<RowSet<Row>> rows =
                conn.preparedQuery(FIND_BY_NAME_AND_VERSION).execute(Tuple.of(name, version));
        return rows.map(
                r ->
                        r.stream()
                                .findFirst()
                                .map(AS_MATCH_RESULT)
                                .orElseThrow(
                                        () ->
                                                new NotFoundException(
                                                        "No template found with name %s and version %s"
                                                                .formatted(name, version))));
    }

    @Override
    public Uni<Long> addShaclTemplate(
            SqlConnection conn,
            ResourceMetadata resourceMetadata,
            List<ShaclShapeMetadata> metadataList,
            String template) {
        debug(
                LOGGER,
                () ->
                        "adding shapes %s"
                                .formatted(
                                        String.join(
                                                ",",
                                                metadataList.stream()
                                                        .map(ShaclShapeMetadata::toString)
                                                        .toList())));
        Tuple tuple =
                Tuple.of(
                        resourceMetadata.getName(),
                        resourceMetadata.getDescription(),
                        resourceMetadata.getVersion(),
                        template,
                        getContextUriIfPresent(resourceMetadata));
        Uni<Long> uniId =
                conn.preparedQuery(INSERT_SHACL_TEMPLATE)
                        .execute(tuple)
                        .map(
                                rows ->
                                        rows.stream()
                                                .findFirst()
                                                .map(r -> r.getLong("id"))
                                                .orElseThrow(
                                                        () ->
                                                                new RuntimeException(
                                                                        "Something bad happened while inserting template")));
        return uniId.call(id -> insertShapes(conn, id, metadataList));
    }

    private String getContextUriIfPresent(ResourceMetadata metadata) {
        if (metadata instanceof TemplateResourceMetadata tm) {
            return tm.getContextUri();
        }
        return null;
    }

    @Override
    public Uni<String> findById(SqlConnection connection, Long id) {
        debug(LOGGER, () -> "getting template by id %s".formatted(id));
        return connection
                .preparedQuery(FIND_BY_ID)
                .execute(Tuple.of(id))
                .map(
                        rs ->
                                rs.stream()
                                        .findFirst()
                                        .map(r -> r.getString("shacl_content"))
                                        .orElseThrow(
                                                () ->
                                                        new NotFoundException(
                                                                "No template found with id %s"
                                                                        .formatted(id))));
    }

    private Uni<Void> insertShapes(
            SqlConnection conn, Long templateId, List<ShaclShapeMetadata> shapes) {
        List<Tuple> tuples =
                shapes.stream()
                        .map(
                                s ->
                                        Tuple.of(
                                                templateId,
                                                s.getShapeId(),
                                                s.getTargetClass(),
                                                s.getVocabularyUri(),
                                                s.getOntologyUri()))
                        .toList();
        return conn.preparedQuery(INSERT_SHAPES).executeBatch(tuples).replaceWithVoid();
    }

    @Override
    public Uni<PagedResult<ResourceMetadata>> search(SqlConnection conn, SearchDto searchDto) {
        debug(LOGGER, () -> "searching with dto %s".formatted(searchDto));
        StringBuilder query = new StringBuilder(SELECT_SEARCH_TEMPLATES);
        StringBuilder count = new StringBuilder(SELECT_COUNT_TEMPLATES);
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        int paramIdx = 1;
        if (!StringUtils.isNullOrEmpty(searchDto.getName())) {
            debug(LOGGER, () -> "adding template name parameter %s".formatted(searchDto.getName()));
            conditions.add(" UPPER(sht.template_name) LIKE $%s".formatted(paramIdx++));
            params.add(asLikeParam(searchDto.getName()));
        }

        if (!StringUtils.isNullOrEmpty(searchDto.getDescription())) {
            debug(
                    LOGGER,
                    () ->
                            "adding template desc parameter %s"
                                    .formatted(searchDto.getDescription()));
            conditions.add(" UPPER(sht.description) LIKE  $%s".formatted(paramIdx++));
            params.add(asLikeParam(searchDto.getDescription()));
        }

        if (!StringUtils.isNullOrEmpty(searchDto.getVersion())) {
            debug(
                    LOGGER,
                    () -> "adding template version parameter %s".formatted(searchDto.getVersion()));
            conditions.add(" UPPER(sht.template_version) LIKE  $%s".formatted(paramIdx));
            params.add(asLikeParam(searchDto.getVersion()));
        }
        if (!conditions.isEmpty()) {
            StringBuilder where = new StringBuilder(" WHERE ");
            where.append(String.join(" AND ", conditions));
            query.append(where);
            count.append(where);
        }
        query.append(
                " ORDER BY sht.template_name, sht.template_version ASC LIMIT %s OFFSET %s"
                        .formatted(searchDto.getLimit(), searchDto.getOffset()));
        Tuple tuple = Tuple.tuple(params);
        debug(
                LOGGER,
                () ->
                        "executing count query %s and search query %s"
                                .formatted(count.toString(), query.toString()));
        Uni<Long> countUni =
                conn.preparedQuery(count.toString())
                        .execute(tuple)
                        .map(
                                rs ->
                                        rs.stream()
                                                .findFirst()
                                                .map(r -> r.getLong("count"))
                                                .orElseThrow(
                                                        () ->
                                                                new RuntimeException(
                                                                        "Something  bad happened while counting search templates")));
        return countUni.flatMap(
                c ->
                        conn.preparedQuery(query.toString())
                                .execute(Tuple.tuple(params))
                                .map(
                                        rows ->
                                                asPagedResult(
                                                        c,
                                                        searchDto.getLimit(),
                                                        rows,
                                                        AS_RESULT_METADATA)));
    }
}
