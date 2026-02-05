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
import static it.extared.dpp.validator.utils.JsonLdUtils.extractNamespace;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;
import io.quarkus.runtime.util.StringUtil;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import it.extared.dpp.validator.exceptions.InvalidOpException;
import it.extared.dpp.validator.jsonld.dto.InputJsonLdMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.*;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import org.jboss.logging.Logger;

@ApplicationScoped
public class JsonLdMetadataExtractor {

    private static final Logger LOGGER = Logger.getLogger(JsonLdMetadataExtractor.class);

    public Uni<InputJsonLdMetadata> extractMetadataDeferred(String jsonLd) {
        return Uni.createFrom()
                .deferred(Unchecked.supplier(() -> Uni.createFrom().item(extractMetadata(jsonLd))));
    }

    private InputJsonLdMetadata extractMetadata(String jsonLd) throws JsonLdError {
        InputJsonLdMetadata metadata = new InputJsonLdMetadata();

        JsonDocument document = JsonDocument.of(new StringReader(jsonLd));

        metadata.setContextUri(
                document.getContextUrl() != null ? document.getContextUrl().toString() : null);

        JsonStructure structure =
                document.getJsonContent()
                        .orElseThrow(
                                () ->
                                        new InvalidOpException(
                                                "The input json-ld seems to be empty"));
        String vocab = extractVocabFromExpanded(structure);
        metadata.setVocabularyUri(vocab);
        debug(LOGGER, () -> "expanding json-ld");
        JsonArray expanded = JsonLd.expand(document).get();

        String type = extractTypeFromExpanded(expanded);
        debug(LOGGER, () -> "extracted type from json-ld is %s".formatted(type));
        metadata.setType(type);
        // not found a @vocab in @context? lets count namespaces
        if (StringUtil.isNullOrEmpty(metadata.getVocabularyUri())) {
            debug(LOGGER, () -> "@vocab not found determining vocabulary uri my ns count");
            Map<String, Integer> namespaceCounts = new HashMap<>();
            countNamespacesInExpanded(expanded, namespaceCounts);
            metadata.setVocabularyUri(
                    namespaceCounts.entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse(null));
            debug(
                    LOGGER,
                    () ->
                            "vocabulary uri found by ns count %s"
                                    .formatted(metadata.getVocabularyUri()));
        }
        return metadata;
    }

    private String extractVocabFromExpanded(JsonStructure jsonStructure) {
        if (jsonStructure.getValueType() == JsonValue.ValueType.OBJECT) {
            JsonObject object = jsonStructure.asJsonObject();
            JsonValue value = object.get("@context");
            if (value != null && value.getValueType() == JsonValue.ValueType.OBJECT) {
                if (value.asJsonObject().containsKey("@vocab")) {
                    JsonValue vocab = value.asJsonObject().get("@vocab");
                    if (vocab != null && vocab.getValueType() == JsonValue.ValueType.STRING)
                        return ((JsonString) vocab).getString();
                }
            }
        }
        return null;
    }

    private String extractTypeFromExpanded(JsonArray expanded) {
        if (expanded.isEmpty()) {
            return null;
        }

        JsonValue firstItem = expanded.getFirst();

        if (firstItem.getValueType() != JsonValue.ValueType.OBJECT) {
            return null;
        }

        JsonObject obj = firstItem.asJsonObject();

        if (!obj.containsKey("@type")) {
            return null;
        }

        JsonValue typeValue = obj.get("@type");

        if (typeValue.getValueType() == JsonValue.ValueType.ARRAY) {
            JsonArray typeArray = typeValue.asJsonArray();
            if (!typeArray.isEmpty()) {
                JsonValue firstType = typeArray.getFirst();
                if (firstType.getValueType() == JsonValue.ValueType.STRING) {
                    return ((JsonString) firstType).getString();
                }
            }
        } else if (typeValue.getValueType() == JsonValue.ValueType.STRING) {
            return ((JsonString) typeValue).getString();
        }

        return null;
    }

    private void countNamespacesInExpanded(
            JsonArray expanded, Map<String, Integer> namespaceCounts) {

        for (JsonValue item : expanded) {
            if (item.getValueType() == JsonValue.ValueType.OBJECT) {
                JsonObject obj = item.asJsonObject();
                countNamespacesInObject(obj, namespaceCounts);
            }
        }
    }

    private void countNamespacesInObject(JsonObject obj, Map<String, Integer> namespaceCounts) {

        for (String key : obj.keySet()) {
            if (key.startsWith("@")) {
                continue;
            }
            String namespace = extractNamespace(key);
            if (namespace != null) {
                namespaceCounts.merge(namespace, 1, Integer::sum);
            }

            JsonValue value = obj.get(key);

            if (value.getValueType() == JsonValue.ValueType.ARRAY) {
                JsonArray array = value.asJsonArray();
                for (JsonValue arrayItem : array) {
                    if (arrayItem.getValueType() == JsonValue.ValueType.OBJECT) {
                        countNamespacesInObject(arrayItem.asJsonObject(), namespaceCounts);
                    }
                }
            } else if (value.getValueType() == JsonValue.ValueType.OBJECT) {
                countNamespacesInObject(value.asJsonObject(), namespaceCounts);
            }
        }
    }
}
