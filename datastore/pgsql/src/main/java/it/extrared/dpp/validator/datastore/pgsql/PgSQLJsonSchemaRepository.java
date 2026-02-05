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
import static it.extared.dpp.validator.utils.JsonUtils.toVertxJson;
import static it.extrared.dpp.validator.datastore.pgsql.Utils.asLikeParam;
import static it.extrared.dpp.validator.datastore.pgsql.Utils.asPagedResult;

import com.fasterxml.jackson.databind.JsonNode;
import io.opentelemetry.api.internal.StringUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.SqlConnection;
import io.vertx.mutiny.sqlclient.Tuple;
import it.extared.dpp.validator.dto.*;
import it.extared.dpp.validator.exceptions.NotFoundException;
import it.extared.dpp.validator.json.JsonSchemaRepository;
import it.extared.dpp.validator.json.dto.PatternProperty;
import it.extared.dpp.validator.json.dto.SchemaMetadata;
import it.extared.dpp.validator.json.dto.SchemaVariant;
import it.extared.dpp.validator.utils.JsonUtils;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jboss.logging.Logger;

/** PostgreSQL implementation of a {@link JsonSchemaRepository}. */
@ApplicationScoped
public class PgSQLJsonSchemaRepository implements JsonSchemaRepository {

    private static final String FIND_BY_NAME_AND_VERSION =
            """
            SELECT 'NAME_AND_VERSION' as match_type, js.schema_content, js.schema_version, js.schema_name FROM json_schemas js WHERE js.schema_name=$1 AND js.schema_version=$2
            """;

    private static final String FIND_BY_ID =
            """
SELECT js.schema_content FROM json_schemas js WHERE js.id=$1
""";
    private static final String SELECT_SEARCH_SCHEMAS =
            """
            SELECT js.id, js.schema_name,js.description, js.schema_version FROM json_schemas js
            """;
    private static final String SELECT_COUNT_SCHEMAS =
            """
            SELECT COUNT(*) AS count FROM json_schemas js
            """;

    private static final String INSERT_SCHEMA_WITH_METADATA =
            """
                    INSERT INTO json_schemas
                    (schema_name,description, schema_version, required_paths, required_paths_count, has_variants, schema_content)
                    VALUES ($1, $2, $3, $4, $5, $6,$7)
                    RETURNING id
                    """;

    private static final String INSERT_PATTERN_PROPERTIES =
            """
                    INSERT INTO schema_pattern_properties
                        (schema_metadata_id, pattern_regex, path_prefix, required_sub_paths)
                        VALUES ($1, $2, $3, $4)
                    """;

    private static final String INSERT_VARIANTS =
            """
                    INSERT INTO schema_variants
                        (schema_metadata_id, variant_type, variant_index, required_paths, required_paths_count,
                         discriminator_path, discriminator_value)
                        VALUES ($1, $2, $3, $4, $5, $6, $7)
                    """;

    private static final String DELETE_SCHEMA =
            """
                    DELETE FROM json_schemas WHERE id=$1
                    """;

    private static final String SIMILARITY_BASED_MATCH =
            """
           WITH schema_scores AS (
                SELECT
                    sm.id,
                    sm.schema_name,
                    sm.schema_version,
                    sm.schema_content,
                    sm.required_paths_count,
                    (
                        SELECT COUNT(*)::int
                        FROM unnest(sm.required_paths) AS rp
                        WHERE rp = ANY($1)
                    ) AS matched_count,
                    $2::int AS input_count
                FROM json_schemas sm
            ),
            base_jaccard AS (
                SELECT
                    id,
                    schema_name,
                    schema_version,
                    schema_content,
                    matched_count,
                    required_paths_count,
                    CASE
                        WHEN required_paths_count = 0 THEN 0.0
                        ELSE matched_count::float / (required_paths_count + 0.6 * (input_count - matched_count))::float
                    END AS jaccard_score
                FROM schema_scores
            ),
            variant_scores AS (
                SELECT
                    sv.schema_metadata_id,
                    sv.required_paths_count,
                    (
                        SELECT COUNT(*)::int
                        FROM unnest(sv.required_paths) AS vp
                        WHERE vp = ANY($1)
                    ) AS variant_matched,
                    $2::int AS input_count
                FROM schema_variants sv
            ),
            variant_jaccard AS (
                SELECT
                    schema_metadata_id,
                    MAX(
                        CASE
                            WHEN required_paths_count = 0 THEN 0.0
                            ELSE variant_matched::float / (required_paths_count + input_count - variant_matched)::float
                        END
                    ) AS best_variant_score
                FROM variant_scores
                GROUP BY schema_metadata_id
            ),
            preliminary_results AS (
                SELECT
                    bj.id,
                    bj.schema_name,
                    bj.schema_version,
                    bj.schema_content,
                    bj.matched_count,
                    bj.required_paths_count,
                    CASE
                        WHEN vj.best_variant_score IS NOT NULL
                        THEN GREATEST(bj.jaccard_score, vj.best_variant_score)
                        ELSE bj.jaccard_score
                    END AS preliminary_score
                FROM base_jaccard bj
                LEFT JOIN variant_jaccard vj ON bj.id = vj.schema_metadata_id
                WHERE CASE
                    WHEN vj.best_variant_score IS NOT NULL
                    THEN GREATEST(bj.jaccard_score, vj.best_variant_score)
                    ELSE bj.jaccard_score
                END >= 0.2
                ORDER BY preliminary_score DESC
                LIMIT 5
            )
            SELECT
                pr.id,
                pr.schema_name,
                pr.schema_version,
                pr.preliminary_score,
                pr.matched_count,
                pr.required_paths_count,
                pr.schema_content,
                COALESCE(
                    json_agg(
                        json_build_object(
                            'pattern_regex', pp.pattern_regex,
                            'path_prefix', pp.path_prefix,
                            'required_sub_paths', pp.required_sub_paths
                        )
                    ) FILTER (WHERE pp.id IS NOT NULL),
                    '[]'::json
                ) AS pattern_properties
            FROM preliminary_results pr
            LEFT JOIN schema_pattern_properties pp ON pp.schema_metadata_id = pr.id
            GROUP BY pr.id, pr.schema_name, pr.schema_version, pr.preliminary_score,
                     pr.matched_count, pr.required_paths_count, pr.schema_content
            ORDER BY pr.preliminary_score DESC
           """;

    private static final Function<Row, MatchResult<JsonNode>> AS_MATCH_RESULT =
            r ->
                    new MatchResult<>(
                            r.getString("schema_name"),
                            r.getString("schema_version"),
                            JsonUtils.fromVertxJson(r.getJsonObject("schema_content")),
                            MatchType.valueOf(r.getString("match_type")));

    private static final Function<Row, ResourceMetadata> AS_RESULT_METADATA =
            r -> {
                ResourceMetadata res = new ResourceMetadata();
                res.setId(r.getLong("id"));
                res.setName(r.getString("schema_name"));
                res.setDescription(r.getString("description"));
                res.setVersion(r.getString("schema_version"));
                return res;
            };

    private static final Logger LOGGER = Logger.getLogger(PgSQLJsonSchemaRepository.class);

    @Override
    public Uni<PagedResult<ResourceMetadata>> search(SqlConnection conn, SearchDto searchDto) {
        debug(LOGGER, () -> "search with parameters %s".formatted(searchDto));
        StringBuilder query = new StringBuilder(SELECT_SEARCH_SCHEMAS);
        StringBuilder count = new StringBuilder(SELECT_COUNT_SCHEMAS);
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        int paramIdx = 1;
        if (!StringUtils.isNullOrEmpty(searchDto.getName())) {
            debug(LOGGER, () -> "adding schema name parameter %s".formatted(searchDto.getName()));
            conditions.add(" UPPER(js.schema_name) LIKE $%s".formatted(paramIdx++));
            params.add(asLikeParam(searchDto.getName()));
        }

        if (!StringUtils.isNullOrEmpty(searchDto.getDescription())) {
            debug(
                    LOGGER,
                    () -> "adding schema desc parameter %s".formatted(searchDto.getDescription()));
            conditions.add(" UPPER(js.description) LIKE  $%s".formatted(paramIdx++));
            params.add(asLikeParam(searchDto.getDescription()));
        }

        if (!StringUtils.isNullOrEmpty(searchDto.getVersion())) {
            debug(
                    LOGGER,
                    () -> "adding schema version parameter %s".formatted(searchDto.getVersion()));
            conditions.add(" UPPER(js.schema_version) LIKE  $%s".formatted(paramIdx));
            params.add(asLikeParam(searchDto.getVersion()));
        }
        if (!conditions.isEmpty()) {
            StringBuilder where = new StringBuilder(" WHERE ");
            where.append(String.join(" AND ", conditions));
            query.append(where);
            count.append(where);
        }
        query.append(
                " ORDER BY js.schema_name, js.schema_version ASC LIMIT %s OFFSET %s"
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

    @Override
    public Uni<Long> addJsonSchema(
            SqlConnection connection,
            ResourceMetadata resMetadata,
            SchemaMetadata metadata,
            JsonNode schema) {
        debug(LOGGER, () -> "adding json schema metadata %s".formatted(metadata));
        Uni<RowSet<Row>> rows =
                connection
                        .preparedQuery(INSERT_SCHEMA_WITH_METADATA)
                        .execute(
                                Tuple.tuple(
                                        List.of(
                                                resMetadata.getName(),
                                                resMetadata.getDescription(),
                                                resMetadata.getVersion(),
                                                metadata.getRequiredPaths().toArray(new String[0]),
                                                metadata.getRequiredPaths().size(),
                                                metadata.isHasVariants(),
                                                toVertxJson(schema))));
        Uni<Long> id =
                rows.map(s -> s.stream().findFirst().map(r -> r.getLong("id")))
                        .map(r -> r.orElse(0L));
        if (metadata.isHasVariants())
            id = id.call(mid -> addVariants(connection, mid, metadata.getVariants()));
        if (metadata.getPatternProperties() != null && !metadata.getPatternProperties().isEmpty())
            id =
                    id.call(
                            mid ->
                                    addPatternProperties(
                                            connection, mid, metadata.getPatternProperties()));
        return id;
    }

    private Uni<Void> addPatternProperties(
            SqlConnection conn, Long schemaId, List<PatternProperty> patternProperties) {
        debug(
                LOGGER,
                () ->
                        "schema with id %s has pattern properties. Adding them to the database"
                                .formatted(schemaId));
        List<Tuple> tuples =
                patternProperties.stream()
                        .map(
                                pp ->
                                        Tuple.of(
                                                schemaId,
                                                pp.getPatternRegex(),
                                                pp.getPathPrefix(),
                                                pp.getRequiredSubPaths().toArray(new String[0])))
                        .toList();
        return conn.preparedQuery(INSERT_PATTERN_PROPERTIES).executeBatch(tuples).replaceWithVoid();
    }

    private Uni<Void> addVariants(SqlConnection conn, Long schemaId, List<SchemaVariant> variants) {
        debug(
                LOGGER,
                () ->
                        "schema with id %s has variants. Adding them to the database"
                                .formatted(schemaId));
        List<Tuple> tuples =
                variants.stream()
                        .map(
                                v ->
                                        Tuple.tuple(
                                                List.of(
                                                        schemaId,
                                                        v.getVariantType(),
                                                        v.getVariantIndex(),
                                                        v.getRequiredPaths().toArray(new String[0]),
                                                        v.getRequiredPaths().size(),
                                                        v.getDiscriminatorPath(),
                                                        v.getDiscriminatorValue())))
                        .toList();
        return conn.preparedQuery(INSERT_VARIANTS).executeBatch(tuples).replaceWithVoid();
    }

    @Override
    public Uni<MatchResult<JsonNode>> findBestMatch(
            SqlConnection connection, String[] jsonProperties) {
        debug(
                LOGGER,
                () ->
                        "searching for best match with properties %s"
                                .formatted(String.join(",", jsonProperties)));
        return connection
                .preparedQuery(SIMILARITY_BASED_MATCH)
                .execute(Tuple.of(jsonProperties, jsonProperties.length))
                .map(rows -> this.performPatternPropertiesRefinement(jsonProperties, rows));
    }

    @Override
    public Uni<MatchResult<JsonNode>> findByNameAndVersion(
            SqlConnection connection, String name, String version) {
        debug(LOGGER, () -> "finding schema by name %s and version".formatted(name, version));
        Uni<RowSet<Row>> rows =
                connection.preparedQuery(FIND_BY_NAME_AND_VERSION).execute(Tuple.of(name, version));
        return rows.map(
                r ->
                        r.stream()
                                .findFirst()
                                .map(AS_MATCH_RESULT)
                                .orElseThrow(
                                        () ->
                                                new NotFoundException(
                                                        "No schema found with name %s and version %s"
                                                                .formatted(name, version))));
    }

    private MatchResult<JsonNode> performPatternPropertiesRefinement(
            String[] jsonProperties, RowSet<Row> rows) {
        if (!rows.iterator().hasNext()) {
            return MatchResult.emptyResult();
        }

        Set<String> inputProps = Set.of(jsonProperties);
        Set<ParsedProperty> parsedInput =
                inputProps.stream().map(this::parseProperty).collect(Collectors.toSet());

        Stream<SchemaCandidate> candidates =
                rows.stream().map(r -> toSchemaCandidate(r, parsedInput, inputProps));

        return candidates
                .filter(c -> c.finalScore >= 0.3)
                .max(Comparator.comparingDouble(a -> a.finalScore))
                .map(this::asMatchResult)
                .orElse(MatchResult.emptyResult());
    }

    private MatchResult<JsonNode> asMatchResult(SchemaCandidate s) {
        MatchResult<JsonNode> result =
                new MatchResult<>(
                        s.schemaName,
                        s.schemaVersion,
                        JsonUtils.fromVertxJson(s.schemaContent),
                        MatchType.SIMILARITY_MATCH);
        debug(
                LOGGER,
                () ->
                        "retrieved schema %s-%s : \n %s"
                                .formatted(s.schemaName, s.schemaVersion, s.schemaContent));
        return result;
    }

    private SchemaCandidate toSchemaCandidate(
            Row row, Set<ParsedProperty> parsedInput, Set<String> inputProps) {
        JsonArray patternsJson = row.getJsonArray("pattern_properties");
        List<PatternPropertyData> patterns = parsePatterns(patternsJson);

        SchemaCandidate candidate =
                new SchemaCandidate(
                        row.getLong("id"),
                        row.getString("schema_name"),
                        row.getString("schema_version"),
                        row.getDouble("preliminary_score"),
                        row.getJsonObject("schema_content"));

        if (!patterns.isEmpty()) {
            int matchedPatterns = countMatchedPatterns(patterns, parsedInput);
            int matchedBasePaths = row.getInteger("matched_count");
            int requiredBasePaths = row.getInteger("required_paths_count");
            int inputCount = inputProps.size();

            int totalMatched = matchedBasePaths + matchedPatterns;
            int totalRequired = requiredBasePaths + patterns.size();

            candidate.finalScore =
                    totalMatched / (totalRequired + 0.6 * (inputCount - totalMatched));
        } else {
            candidate.finalScore = candidate.preliminaryScore;
        }
        debug(
                LOGGER,
                () ->
                        "Final score is %s, preliminary score is %s"
                                .formatted(candidate.finalScore, candidate.preliminaryScore));
        return candidate;
    }

    private List<PatternPropertyData> parsePatterns(JsonArray patternsJson) {
        if (patternsJson == null || patternsJson.isEmpty()) {
            return List.of();
        }

        List<PatternPropertyData> result = new ArrayList<>();
        for (int i = 0; i < patternsJson.size(); i++) {
            JsonObject obj = patternsJson.getJsonObject(i);
            result.add(
                    new PatternPropertyData(
                            obj.getString("pattern_regex"),
                            obj.getString("path_prefix"),
                            getRequiredSubPaths(obj)));
        }
        return result;
    }

    private List<String> getRequiredSubPaths(JsonObject object) {
        JsonArray array = object.getJsonArray("required_sub_paths");
        if (array == null) {
            return List.of();
        }

        return array.stream().filter(Objects::nonNull).map(Object::toString).toList();
    }

    private int countMatchedPatterns(
            List<PatternPropertyData> patterns, Set<ParsedProperty> input) {
        int matched = 0;

        for (PatternPropertyData pattern : patterns) {
            Pattern regex = Pattern.compile(pattern.patternRegex);

            // Find all properties input that matches the pattern regex from the schema
            Set<String> matchedPrefixes =
                    input.stream()
                            .filter(p -> p.prefix != null && regex.matcher(p.prefix).matches())
                            .map(p -> p.prefix)
                            .collect(Collectors.toSet());

            // For each matched prefix verify it has subpaths declared in schema
            for (String prefix : matchedPrefixes) {
                Set<String> availableSubpaths =
                        input.stream()
                                .filter(p -> prefix.equals(p.prefix) && p.subpath != null)
                                .map(p -> p.subpath)
                                .collect(Collectors.toSet());

                if (availableSubpaths.containsAll(pattern.requiredSubPaths)) {
                    matched++;
                }
            }
        }

        return matched;
    }

    private ParsedProperty parseProperty(String propertyPath) {
        int dotIndex = propertyPath.indexOf('.');
        if (dotIndex > 0) {
            return new ParsedProperty(
                    propertyPath.substring(0, dotIndex), propertyPath.substring(dotIndex + 1));
        }
        return new ParsedProperty(propertyPath, null);
    }

    @Override
    public Uni<Void> deleteSchema(SqlConnection connection, Long id) {
        debug(LOGGER, () -> "deleting schema with id %s".formatted(id));
        return connection.preparedQuery(DELETE_SCHEMA).execute(Tuple.of(id)).replaceWithVoid();
    }

    @Override
    public Uni<String> findById(SqlConnection connection, Long id) {
        debug(LOGGER, () -> "retrieving schema with id %s".formatted(id));
        return connection
                .preparedQuery(FIND_BY_ID)
                .execute(Tuple.of(id))
                .map(
                        rs ->
                                rs.stream()
                                        .findFirst()
                                        .map(r -> r.getJsonObject("schema_content"))
                                        .map(JsonObject::toString)
                                        .orElseThrow(
                                                () ->
                                                        new NotFoundException(
                                                                "No template found with id %s"
                                                                        .formatted(id))));
    }

    private static class SchemaCandidate {
        final Long id;
        final String schemaName;
        final String schemaVersion;
        final double preliminaryScore;
        final JsonObject schemaContent;
        double finalScore;

        SchemaCandidate(
                Long id,
                String schemaName,
                String schemaVersion,
                double preliminaryScore,
                JsonObject schemaContent) {
            this.id = id;
            this.schemaName = schemaName;
            this.schemaVersion = schemaVersion;
            this.preliminaryScore = preliminaryScore;
            this.schemaContent = schemaContent;
            this.finalScore = preliminaryScore;
        }
    }

    private record PatternPropertyData(
            String patternRegex, String pathPrefix, List<String> requiredSubPaths) {}

    private record ParsedProperty(String prefix, String subpath) {}
}
