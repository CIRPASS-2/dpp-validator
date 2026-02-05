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
package it.extared.dpp.validator.json.dto;

import java.util.ArrayList;
import java.util.List;

/** Represents a pattern property found in a JSON schema. */
public class PatternProperty {

    private String patternRegex;
    private String pathPrefix;
    private List<String> requiredSubPaths = new ArrayList<>();

    public String getPatternRegex() {
        return patternRegex;
    }

    public void setPatternRegex(String patternRegex) {
        this.patternRegex = patternRegex;
    }

    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    public List<String> getRequiredSubPaths() {
        return requiredSubPaths;
    }

    public void setRequiredSubPaths(List<String> requiredSubPaths) {
        this.requiredSubPaths = requiredSubPaths;
    }

    @Override
    public String toString() {
        return "PatternProperty{"
                + "patternRegex='"
                + patternRegex
                + '\''
                + ", pathPrefix='"
                + pathPrefix
                + '\''
                + ", requiredSubPaths="
                + requiredSubPaths
                + '}';
    }
}
