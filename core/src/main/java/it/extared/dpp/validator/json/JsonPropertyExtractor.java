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

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.runtime.util.StringUtil;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jboss.logging.Logger;

@ApplicationScoped
public class JsonPropertyExtractor {

    private static final Logger LOGGER = Logger.getLogger(JsonPropertyExtractor.class);

    public Set<String> extractAllPaths(JsonNode json) {
        Set<String> paths = new HashSet<>();
        debug(LOGGER, () -> "extracting paths from json \n %s".formatted(json));
        extractPathsRecursive(json, "", paths);
        return paths;
    }

    private void extractPathsRecursive(JsonNode node, String currentPath, Set<String> paths) {
        if (node == null) return;
        debug(
                LOGGER,
                () ->
                        "extracting paths under property name %s from node \n %s"
                                .formatted(
                                        StringUtil.isNullOrEmpty(currentPath)
                                                ? "root"
                                                : currentPath,
                                        node));
        if (node.isObject()) {
            if (!currentPath.isEmpty()) {
                debug(LOGGER, () -> "adding path %s".formatted(currentPath));
                paths.add(currentPath);
            }

            for (Map.Entry<String, JsonNode> field : node.properties()) {
                String key = field.getKey();
                JsonNode value = field.getValue();

                String fullPath = currentPath.isEmpty() ? key : currentPath + "." + key;
                debug(LOGGER, () -> "adding full path %s".formatted(fullPath));
                paths.add(fullPath);

                extractPathsRecursive(value, fullPath, paths);
            }
        } else if (node.isArray()) {
            String arrayPath = currentPath + "[]";
            debug(LOGGER, () -> "adding array path %s".formatted(arrayPath));
            paths.add(arrayPath);

            if (!node.isEmpty()) {
                JsonNode firstElement = node.get(0);
                if (firstElement.isObject() || firstElement.isArray()) {
                    debug(
                            LOGGER,
                            () -> "adding paths from array element %s".formatted(firstElement));
                    extractPathsRecursive(firstElement, arrayPath, paths);
                }
            }
        }
    }
}
