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
package it.extared.dpp.validator.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.inject.spi.CDI;
import java.util.Map;
import java.util.function.Function;

public class JsonUtils {

    public static final String APPLICATION_JSON = "application/json";

    public static final String TEXT_JSON = "text/json";

    public static final String OBJECT_KEY = "object";

    public static final String ARRAY_KEY = "array";

    public static final String TYPE_KEY = "type";

    public static final String ITEMS_KEY = "items";

    public static final String PROPERTIES_KEY = "properties";

    public static final String PATTER_PROPERTIES_KEY = "patternProperties";

    public static final String ALL_OF_KEY = "allOf";

    public static final String ONE_OF_KEY = "oneOf";

    public static final String ANY_OF_KEY = "anyOf";

    public static final String CONST_KEY = "const";

    public static final String ENUM_KEY = "enum";

    public static final String PROPERTY_NAME_KEY = "propertyName";

    public static final String DISCRIMINATOR_KEY = "discriminator";

    public static final String REQUIRED_KEY = "required";

    public static final Function<JsonNode, JsonSchema> JSON_TO_SCHEMA =
            jn -> JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(jn);

    public static Map<String, Object> toMap(JsonNode node) {
        return objectMapper().convertValue(node, new TypeReference<Map<String, Object>>() {});
    }

    public static boolean nodeIsNotNull(JsonNode node) {
        return node != null && !node.isNull() && !node.isMissingNode();
    }

    public static JsonObject toVertxJson(JsonNode node) {
        return new JsonObject(toMap(node));
    }

    public static JsonObject toVertxJson(String content) {
        return new JsonObject(content);
    }

    public static JsonNode fromVertxJson(JsonObject object) {
        return objectMapper().convertValue(object.getMap(), JsonNode.class);
    }

    public static ObjectMapper objectMapper() {
        return CDI.current().select(ObjectMapper.class).get();
    }
}
