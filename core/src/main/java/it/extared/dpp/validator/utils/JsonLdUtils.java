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

import com.fasterxml.jackson.databind.JsonNode;

public class JsonLdUtils {

    public static final String APPLICATION_LD_JSON = "application/ld+json";

    public static final String CONTEXT = "@context";

    public static boolean isJsonLd(JsonNode jsonNode) {
        return jsonNode.has(CONTEXT);
    }

    public static String extractNamespace(String uri) {
        if (uri == null) return null;

        int hashIndex = uri.lastIndexOf('#');
        if (hashIndex > 0) {
            return uri.substring(0, hashIndex + 1);
        }

        int slashIndex = uri.lastIndexOf('/');
        if (slashIndex > 0) {
            return uri.substring(0, slashIndex + 1);
        }

        return uri;
    }
}
