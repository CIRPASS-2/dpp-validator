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
import static it.extared.dpp.validator.utils.JsonUtils.*;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.runtime.util.StringUtil;
import it.extared.dpp.validator.json.dto.DiscriminatorInfo;
import it.extared.dpp.validator.json.dto.PatternProperty;
import it.extared.dpp.validator.json.dto.SchemaMetadata;
import it.extared.dpp.validator.json.dto.SchemaVariant;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;
import org.jboss.logging.Logger;

@ApplicationScoped
public class JsonSchemaMetadataExtractor {

    private static final Logger LOGGER = Logger.getLogger(JsonSchemaMetadataExtractor.class);

    public SchemaMetadata extractMetadata(JsonNode schema) {
        SchemaMetadata metadata = new SchemaMetadata();
        debug(LOGGER, () -> "extracting metadata from schema \n %s".formatted(schema));
        handleSchema(schema, metadata);
        return metadata;
    }

    private void handleSchema(JsonNode schema, SchemaMetadata metadata) {
        Set<String> paths = extractRequiredPaths(schema, "");
        metadata.getRequiredPaths().addAll(paths);
        List<PatternProperty> patternProps = extractPatternProperties(schema, "");
        metadata.getPatternProperties().addAll(patternProps);
        if (schema.has(ALL_OF_KEY)) {
            handleAllOf(schema.get(ALL_OF_KEY), metadata);
        }
        if (schema.has(ONE_OF_KEY)) {
            metadata.getVariants()
                    .addAll(extractVariants(schema.get(ONE_OF_KEY), ONE_OF_KEY, schema));
            metadata.setHasVariants(true);
        }
        if (schema.has(ANY_OF_KEY)) {
            metadata.getVariants()
                    .addAll(extractVariants(schema.get(ANY_OF_KEY), ANY_OF_KEY, schema));
            metadata.setHasVariants(true);
        }
    }

    private Set<String> extractRequiredPaths(JsonNode schema, String currentPath) {
        debug(
                LOGGER,
                () ->
                        "extracting required path under current path %s"
                                .formatted(
                                        StringUtil.isNullOrEmpty(currentPath)
                                                ? "root"
                                                : currentPath));
        Set<String> paths = new HashSet<>();
        if (schema == null || !schema.isObject()) return paths;

        JsonNode requiredNode = schema.get(REQUIRED_KEY);
        JsonNode propertiesNode = schema.get(PROPERTIES_KEY);

        if (requiredNode != null && requiredNode.isArray()) {
            debug(LOGGER, () -> "extracting required properties %s".formatted(requiredNode));
            requiredNode.forEach(
                    req -> addPathsFromRequiredNode(req, paths, propertiesNode, currentPath));
        }

        return paths;
    }

    private void addPathsFromRequiredNode(
            JsonNode reqNode, Set<String> paths, JsonNode propertiesNode, String currentPath) {
        String propName = reqNode.asText();
        String fullPath = currentPath.isEmpty() ? propName : currentPath + "." + propName;
        debug(LOGGER, () -> "adding full froom required node, %s".formatted(fullPath));
        paths.add(fullPath);
        if (propertiesNode != null && propertiesNode.has(propName)) {
            JsonNode propSchema = propertiesNode.get(propName);
            paths.addAll(recurseIntoProperty(propSchema, fullPath));
        }
    }

    private Set<String> recurseIntoProperty(JsonNode propSchema, String fullPath) {
        Set<String> paths = new HashSet<>();
        JsonNode propType = propSchema.get(TYPE_KEY);

        if (propType != null) {
            if (propType.asText().equals(OBJECT_KEY)) {
                paths.addAll(extractRequiredPaths(propSchema, fullPath));

                if (propSchema.has(PATTER_PROPERTIES_KEY)) {
                    paths.add(fullPath + ".<pattern>");
                }
            } else if (propType.asText().equals(ARRAY_KEY)) {
                JsonNode itemsNode = propSchema.get(ITEMS_KEY);
                if (itemsNode != null && itemsNode.isObject()) {
                    JsonNode itemType = itemsNode.get(TYPE_KEY);
                    if (itemType != null && itemType.asText().equals(OBJECT_KEY)) {
                        paths.addAll(extractRequiredPaths(itemsNode, fullPath + "[]"));
                    }
                }
            }
        }

        return paths;
    }

    private void handleAllOf(JsonNode allOfNode, SchemaMetadata metadata) {
        if (!allOfNode.isArray()) return;
        debug(LOGGER, () -> "adding all of schemas %s".formatted(allOfNode));
        allOfNode.forEach(n -> handleSchema(n, metadata));
    }

    private List<SchemaVariant> extractVariants(
            JsonNode variantsNode, String variantType, JsonNode parentSchema) {
        List<SchemaVariant> variants = new ArrayList<>();
        if (!variantsNode.isArray()) return variants;

        DiscriminatorInfo discriminator = detectDiscriminator(variantsNode, parentSchema);

        int index = 0;
        for (JsonNode variantSchema : variantsNode) {
            SchemaVariant variant = new SchemaVariant();
            variant.setVariantType(variantType);
            variant.setVariantIndex(index);
            variant.setRequiredPaths(extractRequiredPaths(variantSchema, ""));

            if (discriminator != null) {
                variant.setDiscriminatorPath(discriminator.getPath());
                variant.setDiscriminatorValue(
                        extractDiscriminatorValue(variantSchema, discriminator));
            }

            variants.add(variant);
            index++;
        }

        return variants;
    }

    private DiscriminatorInfo detectDiscriminator(JsonNode variantsNode, JsonNode parentSchema) {
        debug(LOGGER, () -> "retrieving discriminator for polymorphic json");
        if (parentSchema.has(DISCRIMINATOR_KEY)) {
            JsonNode disc = parentSchema.get(DISCRIMINATOR_KEY);
            if (disc.has(PROPERTY_NAME_KEY)) {
                JsonNode nodeDisc = disc.get(PROPERTY_NAME_KEY);
                debug(LOGGER, () -> "found discriminator %s".formatted(nodeDisc));
                return new DiscriminatorInfo(disc.asText());
            }
        }

        // ok we don't have a discriminator explicitly defined.
        // let's search in all the variants for a common property that has a different value in each
        // variant
        Map<String, Set<String>> candidateFields = new HashMap<>();

        for (JsonNode variant : variantsNode) {
            if (variant.has(PROPERTIES_KEY))
                variant.get(PROPERTIES_KEY)
                        .properties()
                        .forEach(entry -> addCandidateField(candidateFields, entry));
        }
        for (Map.Entry<String, Set<String>> entry : candidateFields.entrySet()) {
            if (entry.getValue().size() == variantsNode.size()) {
                debug(
                        LOGGER,
                        () ->
                                "extracting discriminator %s from variant %s"
                                        .formatted(entry.getKey(), variantsNode));
                return new DiscriminatorInfo(entry.getKey());
            }
        }

        return null;
    }

    // add the candidate if we have enum or const keys that might it likely being a discriminator
    private void addCandidateField(
            Map<String, Set<String>> candidateFields, Map.Entry<String, JsonNode> variantProperty) {
        String fieldName = variantProperty.getKey();
        JsonNode fieldSchema = variantProperty.getValue();

        if (fieldSchema.has(CONST_KEY)) {
            candidateFields
                    .computeIfAbsent(fieldName, k -> new HashSet<>())
                    .add(fieldSchema.get(CONST_KEY).asText());
        } else if (fieldSchema.has(ENUM_KEY) && fieldSchema.get(ENUM_KEY).size() == 1) {
            candidateFields
                    .computeIfAbsent(fieldName, k -> new HashSet<>())
                    .add(fieldSchema.get(ENUM_KEY).get(0).asText());
        }
    }

    private String extractDiscriminatorValue(
            JsonNode variantSchema, DiscriminatorInfo discriminator) {
        if (variantSchema.has(PROPERTIES_KEY)) {
            JsonNode props = variantSchema.get(PROPERTIES_KEY);
            if (props.has(discriminator.getPath())) {
                JsonNode discProp = props.get(discriminator.getPath());
                if (discProp.has(CONST_KEY)) {
                    return discProp.get(CONST_KEY).asText();
                } else if (discProp.has(ENUM_KEY) && discProp.get(ENUM_KEY).size() == 1) {
                    return discProp.get(ENUM_KEY).get(0).asText();
                }
            }
        }
        return null;
    }

    private List<PatternProperty> extractPatternProperties(JsonNode schema, String currentPath) {
        List<PatternProperty> patterns = new ArrayList<>();

        if (!schema.has(PATTER_PROPERTIES_KEY)) return patterns;

        JsonNode patternPropsNode = schema.get(PATTER_PROPERTIES_KEY);
        patternPropsNode
                .properties()
                .forEach(entry -> addPatternProperty(patterns, currentPath, entry));

        if (schema.has(PROPERTIES_KEY)) {
            for (Map.Entry<String, JsonNode> entry : schema.get(PROPERTIES_KEY).properties()) {

                String propName = entry.getKey();
                JsonNode propSchema = entry.getValue();
                String fullPath = currentPath.isEmpty() ? propName : currentPath + "." + propName;

                patterns.addAll(extractPatternProperties(propSchema, fullPath));
            }
        }

        return patterns;
    }

    private void addPatternProperty(
            List<PatternProperty> patterns,
            String currentPath,
            Map.Entry<String, JsonNode> patternProp) {
        String pattern = patternProp.getKey();
        JsonNode patternSchema = patternProp.getValue();
        debug(LOGGER, () -> "adding pattern property %s".formatted(pattern));
        PatternProperty pp = new PatternProperty();
        pp.setPatternRegex(pattern);
        String extractedPrefix = extractPrefixFromPattern(pattern);
        String fullPrefix =
                currentPath.isEmpty() ? extractedPrefix : currentPath + "." + extractedPrefix;
        pp.setPathPrefix(fullPrefix);

        JsonNode patternType = patternSchema.get(TYPE_KEY);
        if (patternType != null && patternType.asText().equals(OBJECT_KEY)) {
            debug(LOGGER, () -> "pattern property is an object. extracting subpaths");
            pp.setRequiredSubPaths(new ArrayList<>(extractRequiredPaths(patternSchema, "")));
        }

        patterns.add(pp);
    }

    private String extractPrefixFromPattern(String patternRegex) {
        String pattern = patternRegex.startsWith("^") ? patternRegex.substring(1) : patternRegex;
        debug(
                LOGGER,
                () ->
                        "extract prefix of pattern property from pattern %s property"
                                .formatted(pattern));
        int endOfPrefix = findEndOfFixedPrefix(pattern);

        if (endOfPrefix > 0) {
            String prefix = pattern.substring(0, endOfPrefix);
            debug(LOGGER, () -> "prefix is %s".formatted(prefix));
        }
        debug(LOGGER, () -> "prefix is empty");
        return "";
    }

    private int findEndOfFixedPrefix(String pattern) {
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '[' || c == '(' || c == '.' || c == '*' || c == '+' || c == '?' || c == '{'
                    || c == '|' || c == '$') {
                return i;
            }
            if (c == '\\' && i + 1 < pattern.length()) {
                i++;
            }
        }
        return pattern.length();
    }
}
