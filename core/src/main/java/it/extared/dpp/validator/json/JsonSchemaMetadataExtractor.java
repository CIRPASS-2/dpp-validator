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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.runtime.util.StringUtil;
import it.extared.dpp.validator.json.dto.DiscriminatorInfo;
import it.extared.dpp.validator.json.dto.PatternProperty;
import it.extared.dpp.validator.json.dto.SchemaMetadata;
import it.extared.dpp.validator.json.dto.SchemaVariant;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.*;
import org.jboss.logging.Logger;

@ApplicationScoped
public class JsonSchemaMetadataExtractor {

    private static final Logger LOGGER = Logger.getLogger(JsonSchemaMetadataExtractor.class);

    private static final String REF_KEY = "$ref";

    @Inject ObjectMapper objectMapper;

    public SchemaMetadata extractMetadata(JsonNode schema) {
        SchemaMetadata metadata = new SchemaMetadata();
        debug(LOGGER, () -> "extracting metadata from schema \n %s".formatted(schema));
        // Use an identity-based set to track visited nodes and break circular ref chains
        Set<JsonNode> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        handleSchema(schema, schema, metadata, visited);
        return metadata;
    }

    private void handleSchema(
            JsonNode schema, JsonNode root, SchemaMetadata metadata, Set<JsonNode> visited) {
        JsonNode resolved = resolveRef(schema, root, visited);
        if (resolved == null) return;

        JsonNode flat = flattenAllOf(resolved, root, visited);

        Set<String> paths = extractRequiredPaths(flat, root, "", visited);
        metadata.getRequiredPaths().addAll(paths);
        List<PatternProperty> patternProps = extractPatternProperties(flat, root, "", visited);
        metadata.getPatternProperties().addAll(patternProps);

        // Use resolved (not flat) because flattenAllOf removes the allOf key
        if (resolved.has(ALL_OF_KEY)) {
            handleAllOf(resolved.get(ALL_OF_KEY), root, metadata, visited);
        }
        if (flat.has(ONE_OF_KEY)) {
            metadata.getVariants()
                    .addAll(extractVariants(flat.get(ONE_OF_KEY), ONE_OF_KEY, flat, root, visited));
            metadata.setHasVariants(true);
        }
        if (flat.has(ANY_OF_KEY)) {
            metadata.getVariants()
                    .addAll(extractVariants(flat.get(ANY_OF_KEY), ANY_OF_KEY, flat, root, visited));
            metadata.setHasVariants(true);
        }
    }

    private JsonNode resolveRef(JsonNode schema, JsonNode root, Set<JsonNode> visited) {
        if (schema == null) return null;
        if (!schema.has(REF_KEY)) return schema;

        String ref = schema.get(REF_KEY).asText();
        if (!ref.startsWith("#/")) return schema;

        String[] parts = ref.substring(2).split("/");
        JsonNode resolved = root;
        for (String part : parts) {
            resolved = resolved.get(part);
            if (resolved == null) return null;
        }

        // Guard: if we've already visited this exact node instance, stop
        if (!visited.add(resolved)) return null;

        return resolveRef(resolved, root, visited);
    }

    private JsonNode flattenAllOf(JsonNode schema, JsonNode root, Set<JsonNode> visited) {
        if (!schema.has(ALL_OF_KEY)) return schema;

        ObjectNode merged = objectMapper.createObjectNode();
        ObjectNode mergedProperties = objectMapper.createObjectNode();
        ArrayNode mergedRequired = objectMapper.createArrayNode();

        schema.properties().stream()
                .filter(e -> !e.getKey().equals(ALL_OF_KEY))
                .forEach(e -> merged.set(e.getKey(), e.getValue()));

        for (JsonNode entry : schema.get(ALL_OF_KEY)) {
            JsonNode resolved = resolveRef(entry, root, visited);
            if (resolved == null) continue;
            JsonNode flattened = flattenAllOf(resolved, root, visited);

            if (flattened.has(PROPERTIES_KEY)) {
                flattened
                        .get(PROPERTIES_KEY)
                        .properties()
                        .forEach(e -> mergedProperties.set(e.getKey(), e.getValue()));
            }
            if (flattened.has(REQUIRED_KEY)) {
                flattened.get(REQUIRED_KEY).forEach(mergedRequired::add);
            }
            flattened.properties().stream()
                    .filter(
                            e ->
                                    !e.getKey().equals(PROPERTIES_KEY)
                                            && !e.getKey().equals(REQUIRED_KEY)
                                            && !e.getKey().equals(ALL_OF_KEY))
                    .forEach(e -> merged.set(e.getKey(), e.getValue()));
        }

        if (!mergedProperties.isEmpty()) merged.set(PROPERTIES_KEY, mergedProperties);
        if (!mergedRequired.isEmpty()) merged.set(REQUIRED_KEY, mergedRequired);
        return merged;
    }

    private Set<String> extractRequiredPaths(
            JsonNode schema, JsonNode root, String currentPath, Set<JsonNode> visited) {
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

        JsonNode resolved = flattenAllOf(resolveRefSafe(schema, root, visited), root, visited);
        if (resolved == null) return paths;

        JsonNode requiredNode = resolved.get(REQUIRED_KEY);
        JsonNode propertiesNode = resolved.get(PROPERTIES_KEY);

        Set<String> requiredFields = new HashSet<>();
        if (requiredNode != null && requiredNode.isArray()) {
            debug(LOGGER, () -> "extracting required properties %s".formatted(requiredNode));
            requiredNode.forEach(
                    req -> {
                        requiredFields.add(req.asText());
                        addPathsFromRequiredNode(
                                req, paths, propertiesNode, root, currentPath, visited);
                    });
        }

        // Traverse non-required properties too to find nested required paths.
        if (propertiesNode != null) {
            for (Map.Entry<String, JsonNode> entry : propertiesNode.properties()) {
                String propName = entry.getKey();
                if (requiredFields.contains(propName)) continue;
                String fullPath = currentPath.isEmpty() ? propName : currentPath + "." + propName;
                Set<JsonNode> siblingVisited = Collections.newSetFromMap(new IdentityHashMap<>());
                siblingVisited.addAll(visited);
                paths.addAll(recurseIntoProperty(entry.getValue(), root, fullPath, siblingVisited));
            }
        }
        return paths;
    }

    private void addPathsFromRequiredNode(
            JsonNode reqNode,
            Set<String> paths,
            JsonNode propertiesNode,
            JsonNode root,
            String currentPath,
            Set<JsonNode> visited) {
        String propName = reqNode.asText();
        String fullPath = currentPath.isEmpty() ? propName : currentPath + "." + propName;
        debug(LOGGER, () -> "adding full from required node, %s".formatted(fullPath));
        paths.add(fullPath);
        if (propertiesNode != null && propertiesNode.has(propName)) {
            paths.addAll(
                    recurseIntoProperty(propertiesNode.get(propName), root, fullPath, visited));
        }
    }

    private Set<String> recurseIntoProperty(
            JsonNode propSchema, JsonNode root, String fullPath, Set<JsonNode> visited) {
        Set<String> paths = new HashSet<>();
        if (propSchema == null) return paths;

        JsonNode resolved = resolveRefSafe(propSchema, root, visited);
        if (resolved == null) return paths;

        JsonNode flat = flattenAllOf(resolved, root, visited);
        JsonNode propType = flat.get(TYPE_KEY);

        if (propType != null) {
            if (propType.asText().equals(OBJECT_KEY)) {
                paths.addAll(extractRequiredPaths(flat, root, fullPath, visited));
                if (flat.has(PATTER_PROPERTIES_KEY)) {
                    paths.add(fullPath + ".<pattern>");
                }
            } else if (propType.asText().equals(ARRAY_KEY)) {
                JsonNode itemsNode = flat.get(ITEMS_KEY);
                if (itemsNode != null && itemsNode.isObject()) {
                    JsonNode itemResolved = resolveRefSafe(itemsNode, root, visited);
                    if (itemResolved != null) {
                        JsonNode itemFlat = flattenAllOf(itemResolved, root, visited);
                        JsonNode itemType = itemFlat.get(TYPE_KEY);
                        if (itemType != null && itemType.asText().equals(OBJECT_KEY)) {
                            paths.addAll(
                                    extractRequiredPaths(itemFlat, root, fullPath + "[]", visited));
                        }
                    }
                }
            }
        } else if (flat.has(PROPERTIES_KEY) || flat.has(REQUIRED_KEY)) {
            paths.addAll(extractRequiredPaths(flat, root, fullPath, visited));
        }

        return paths;
    }

    /**
     * Resolves a $ref without adding the target node to visited — used when we only want to read
     * the node content without committing to a traversal path. Returns null if the ref has already
     * been visited (cycle detected).
     */
    private JsonNode resolveRefSafe(JsonNode schema, JsonNode root, Set<JsonNode> visited) {
        if (schema == null) return null;
        if (!schema.has(REF_KEY)) return schema;
        return resolveRef(schema, root, visited);
    }

    private void handleAllOf(
            JsonNode allOfNode, JsonNode root, SchemaMetadata metadata, Set<JsonNode> visited) {
        if (!allOfNode.isArray()) return;
        debug(LOGGER, () -> "adding all of schemas %s".formatted(allOfNode));
        allOfNode.forEach(n -> handleSchema(n, root, metadata, visited));
    }

    private List<SchemaVariant> extractVariants(
            JsonNode variantsNode,
            String variantType,
            JsonNode parentSchema,
            JsonNode root,
            Set<JsonNode> visited) {
        List<SchemaVariant> variants = new ArrayList<>();
        if (!variantsNode.isArray()) return variants;

        DiscriminatorInfo discriminator =
                detectDiscriminator(variantsNode, parentSchema, root, visited);

        int index = 0;
        for (JsonNode variantSchema : variantsNode) {
            JsonNode resolved = resolveRefSafe(variantSchema, root, visited);
            if (resolved == null) {
                index++;
                continue;
            }
            JsonNode resolvedVariant = flattenAllOf(resolved, root, visited);
            SchemaVariant variant = new SchemaVariant();
            variant.setVariantType(variantType);
            variant.setVariantIndex(index);
            Set<JsonNode> variantVisited = Collections.newSetFromMap(new IdentityHashMap<>());
            variant.setRequiredPaths(
                    extractRequiredPaths(resolvedVariant, root, "", variantVisited));

            if (discriminator != null) {
                variant.setDiscriminatorPath(discriminator.getPath());
                variant.setDiscriminatorValue(
                        extractDiscriminatorValue(resolvedVariant, discriminator));
            }

            variants.add(variant);
            index++;
        }

        return variants;
    }

    private DiscriminatorInfo detectDiscriminator(
            JsonNode variantsNode, JsonNode parentSchema, JsonNode root, Set<JsonNode> visited) {
        debug(LOGGER, () -> "retrieving discriminator for polymorphic json");
        if (parentSchema.has(DISCRIMINATOR_KEY)) {
            JsonNode disc = parentSchema.get(DISCRIMINATOR_KEY);
            if (disc.has(PROPERTY_NAME_KEY)) {
                JsonNode nodeDisc = disc.get(PROPERTY_NAME_KEY);
                debug(LOGGER, () -> "found discriminator %s".formatted(nodeDisc));
                return new DiscriminatorInfo(disc.asText());
            }
        }

        Map<String, Set<String>> candidateFields = new HashMap<>();

        for (JsonNode variant : variantsNode) {
            JsonNode resolved = resolveRefSafe(variant, root, visited);
            if (resolved == null) continue;
            JsonNode flat = flattenAllOf(resolved, root, visited);
            if (flat.has(PROPERTIES_KEY))
                flat.get(PROPERTIES_KEY)
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

    private List<PatternProperty> extractPatternProperties(
            JsonNode schema, JsonNode root, String currentPath, Set<JsonNode> visited) {
        List<PatternProperty> patterns = new ArrayList<>();

        if (!schema.has(PATTER_PROPERTIES_KEY)) return patterns;

        JsonNode patternPropsNode = schema.get(PATTER_PROPERTIES_KEY);
        patternPropsNode
                .properties()
                .forEach(entry -> addPatternProperty(patterns, currentPath, entry, root, visited));

        if (schema.has(PROPERTIES_KEY)) {
            for (Map.Entry<String, JsonNode> entry : schema.get(PROPERTIES_KEY).properties()) {
                String propName = entry.getKey();
                JsonNode propSchema = resolveRefSafe(entry.getValue(), root, visited);
                if (propSchema == null) continue;
                String fullPath = currentPath.isEmpty() ? propName : currentPath + "." + propName;
                patterns.addAll(extractPatternProperties(propSchema, root, fullPath, visited));
            }
        }

        return patterns;
    }

    private void addPatternProperty(
            List<PatternProperty> patterns,
            String currentPath,
            Map.Entry<String, JsonNode> patternProp,
            JsonNode root,
            Set<JsonNode> visited) {
        String pattern = patternProp.getKey();
        JsonNode patternSchema = resolveRefSafe(patternProp.getValue(), root, visited);
        if (patternSchema == null) return;
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
            Set<JsonNode> patternVisited = Collections.newSetFromMap(new IdentityHashMap<>());
            pp.setRequiredSubPaths(
                    new ArrayList<>(extractRequiredPaths(patternSchema, root, "", patternVisited)));
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
