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

import static it.extared.dpp.validator.utils.CommonUtils.debug;

import io.quarkus.runtime.util.StringUtil;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.sqlclient.Pool;
import it.extared.dpp.validator.ValidationType;
import it.extared.dpp.validator.Validator;
import it.extared.dpp.validator.dto.InvalidProperty;
import it.extared.dpp.validator.dto.MatchResult;
import it.extared.dpp.validator.dto.ValidationReport;
import it.extared.dpp.validator.exceptions.NotFoundException;
import it.extared.dpp.validator.jsonld.dto.InputJsonLdMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SemanticValidator implements Validator {

    @Inject JsonLdMetadataExtractor extractor;

    @Inject ShaclTemplateRepository repository;

    @Inject Pool pool;

    @Inject Vertx vertx;

    private static final Logger LOGGER = Logger.getLogger(SemanticValidator.class);

    @Override
    public Uni<ValidationReport> validate(byte[] input) throws IOException {
        debug(LOGGER, () -> "validating by smart match");
        Uni<InputJsonLdMetadata> metadata = extractor.extractMetadataDeferred(new String(input));
        metadata = vertx.executeBlocking(metadata);
        return metadata.flatMap(m -> findTemplateAndValidate(input, m));
    }

    @Override
    public Uni<ValidationReport> validate(String resourceName, String version, byte[] input)
            throws IOException {
        debug(
                LOGGER,
                () -> "validating by name %s and version %s".formatted(resourceName, version));
        Uni<MatchResult<String>> matchResult =
                pool.withConnection(c -> repository.findByNameAndVersion(c, resourceName, version));
        return matchResult.map(m -> getValidationReport(input, m));
    }

    private Uni<ValidationReport> findTemplateAndValidate(
            byte[] input, InputJsonLdMetadata metadata) {
        Uni<MatchResult<String>> matchResult =
                pool.withConnection(
                        c ->
                                repository
                                        .findBestMatch(c, metadata)
                                        .flatMap(
                                                r -> {
                                                    if (r.hasNoTemplate()) {
                                                        throw new NotFoundException(
                                                                "No template suitable to validate the input was found");
                                                    }
                                                    return Uni.createFrom().item(r);
                                                }));
        return matchResult.map(m -> getValidationReport(input, m));
    }

    private ValidationReport getValidationReport(byte[] input, MatchResult<String> matchResult) {
        ValidationReport.Builder report = validate(input, matchResult);
        return report.withMessage(
                        "Validation performed using template found by %s"
                                .formatted(matchResult.getMatchType().name()))
                .build();
    }

    private ValidationReport.Builder validate(byte[] inputData, MatchResult<String> match) {
        Model dataModel = ModelFactory.createDefaultModel();
        dataModel.read(new ByteArrayInputStream(inputData), null, "JSON-LD");
        Graph dataGraph = dataModel.getGraph();
        Model shapesModel = ModelFactory.createDefaultModel();
        shapesModel.read(new ByteArrayInputStream(match.getResource().getBytes()), null, "TURTLE");
        Graph shapesGraph = shapesModel.getGraph();
        Shapes shapes = Shapes.parse(shapesGraph);
        org.apache.jena.shacl.ValidationReport report =
                ShaclValidator.get().validate(shapes, dataGraph);

        return asDto(match, report);
    }

    private ValidationReport.Builder asDto(
            MatchResult<String> match, org.apache.jena.shacl.ValidationReport jenaReport) {
        ValidationReport.Builder builder = ValidationReport.builder();
        builder.withValid(jenaReport.conforms());
        if (!jenaReport.conforms()) {
            List<InvalidProperty> invalidProperties = new ArrayList<>();
            Model reportModel = jenaReport.getModel();

            Resource reportResource = jenaReport.getResource();
            StmtIterator resultIter =
                    reportModel.listStatements(
                            reportResource,
                            reportModel.getProperty("http://www.w3.org/ns/shacl#result"),
                            (RDFNode) null);
            while (resultIter.hasNext()) {
                Resource resultNode = resultIter.next().getResource();
                invalidProperties.add(extractViolation(resultNode, reportModel));
            }
            builder.withInvalidProperties(invalidProperties);
        }
        builder.withResourceName(match.getName())
                .withResourceVersion(match.getVersion())
                .withValidationType(ValidationType.RDF);
        return builder;
    }

    private InvalidProperty extractViolation(Resource resultNode, Model reportModel) {
        String focusNode =
                getPropertyValue(resultNode, reportModel, "http://www.w3.org/ns/shacl#focusNode");
        String resultPath =
                getPropertyValue(resultNode, reportModel, "http://www.w3.org/ns/shacl#resultPath");
        String message =
                getPropertyValue(
                        resultNode, reportModel, "http://www.w3.org/ns/shacl#resultMessage");
        String prop = "[%s]%s".formatted(valueOrNA(focusNode), valueOrNA(resultPath));
        return new InvalidProperty(prop, message);
    }

    private String valueOrNA(String value) {
        return StringUtil.isNullOrEmpty(value) ? "N/A" : value;
    }

    private String getPropertyValue(Resource resource, Model model, String propertyUri) {
        Property property = model.getProperty(propertyUri);
        Statement stmt = resource.getProperty(property);

        if (stmt == null) {
            return null;
        }

        RDFNode object = stmt.getObject();
        if (object.isURIResource()) {
            return object.asResource().getURI();
        } else if (object.isLiteral()) {
            return object.asLiteral().getString();
        }

        return object.toString();
    }

    @Override
    public boolean canHandle(ValidationType type) {
        return Objects.equals(ValidationType.RDF, type);
    }

    @Override
    public Integer priority() {
        return 99;
    }
}
