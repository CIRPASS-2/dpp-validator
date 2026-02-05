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

import static it.extared.dpp.validator.utils.CommonUtils.debug;
import static it.extared.dpp.validator.utils.JsonUtils.JSON_TO_SCHEMA;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.ValidationMessage;
import io.quarkus.arc.Unremovable;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;
import it.extared.dpp.validator.ValidationType;
import it.extared.dpp.validator.Validator;
import it.extared.dpp.validator.dto.InvalidProperty;
import it.extared.dpp.validator.dto.MatchResult;
import it.extared.dpp.validator.dto.MatchType;
import it.extared.dpp.validator.dto.ValidationReport;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import org.jboss.logging.Logger;

@Unremovable
@ApplicationScoped
public class PlainJsonValidator implements Validator {
    @Inject Pool pool;
    @Inject JsonSchemaRepository repository;
    @Inject JsonPropertyExtractor extractor;
    @Inject ObjectMapper objectMapper;

    private static final Logger LOGGER = Logger.getLogger(PlainJsonValidator.class);

    @Override
    public Uni<ValidationReport> validate(byte[] input) throws IOException {
        debug(LOGGER, () -> "validating json by similarity match");
        JsonNode jsonNode = objectMapper.readTree(input);
        Set<String> paths = extractor.extractAllPaths(jsonNode);
        Uni<MatchResult<JsonNode>> schema =
                pool.withConnection(
                        conn -> repository.findBestMatch(conn, paths.toArray(new String[0])));
        return schema.map(s -> getValidationReport(jsonNode, s));
    }

    @Override
    public Uni<ValidationReport> validate(String resourceName, String version, byte[] input)
            throws IOException {
        debug(
                LOGGER,
                () -> "validating json by name %s and version %s".formatted(resourceName, version));
        JsonNode jsonNode = objectMapper.readTree(input);
        Uni<MatchResult<JsonNode>> schema =
                pool.withConnection(c -> repository.findByNameAndVersion(c, resourceName, version));
        return schema.map(s -> getValidationReport(jsonNode, s));
    }

    private ValidationReport getValidationReport(
            JsonNode input, MatchResult<JsonNode> matchResult) {
        if (Objects.equals(matchResult.getMatchType(), MatchType.NONE)) {
            return ValidationReport.builder()
                    .withValid(false)
                    .withValidationType(ValidationType.PLAIN_JSON)
                    .withMessage("No JSON matchResult found matching by similarity the input JSON")
                    .build();
        }
        debug(LOGGER, () -> "validating json %s and building report".formatted(input));
        return asValidationReport(
                matchResult, JSON_TO_SCHEMA.apply(matchResult.getResource()).validate(input));
    }

    public ValidationReport asValidationReport(
            MatchResult<JsonNode> matchResult, Set<ValidationMessage> validationMessages) {
        ValidationReport.Builder builder = ValidationReport.builder();
        builder.withValid(validationMessages == null || validationMessages.isEmpty());
        if (validationMessages != null && !validationMessages.isEmpty()) {
            builder.withInvalidProperties(
                    validationMessages.stream()
                            .map(m -> new InvalidProperty(m.getProperty(), m.getMessage()))
                            .toList());
        }
        builder.withMessage(
                        "Validation performed using template found by %s"
                                .formatted(matchResult.getMatchType().name()))
                .withResourceName(matchResult.getName())
                .withResourceVersion(matchResult.getVersion())
                .withValidationType(ValidationType.PLAIN_JSON);
        return builder.build();
    }

    @Override
    public boolean canHandle(ValidationType type) {
        return Objects.equals(ValidationType.PLAIN_JSON, type);
    }

    @Override
    public Integer priority() {
        return 99;
    }
}
