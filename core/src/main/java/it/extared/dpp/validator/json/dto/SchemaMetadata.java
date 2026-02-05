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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents metadata extracted from a JSON schemas (required properties, variants, and pattern
 * properties).
 */
public class SchemaMetadata {
    private Set<String> requiredPaths = new HashSet<>();
    private boolean hasVariants = false;
    private List<SchemaVariant> variants = new ArrayList<>();
    private List<PatternProperty> patternProperties = new ArrayList<>();

    public Set<String> getRequiredPaths() {
        return requiredPaths;
    }

    public void setRequiredPaths(Set<String> requiredPaths) {
        this.requiredPaths = requiredPaths;
    }

    public boolean isHasVariants() {
        return hasVariants;
    }

    public void setHasVariants(boolean hasVariants) {
        this.hasVariants = hasVariants;
    }

    public List<SchemaVariant> getVariants() {
        return variants;
    }

    public void setVariants(List<SchemaVariant> variants) {
        this.variants = variants;
    }

    public List<PatternProperty> getPatternProperties() {
        return patternProperties;
    }

    public void setPatternProperties(List<PatternProperty> patternProperties) {
        this.patternProperties = patternProperties;
    }

    @Override
    public String toString() {
        return "SchemaMetadata{"
                + "requiredPaths="
                + requiredPaths
                + ", hasVariants="
                + hasVariants
                + ", variants="
                + variants
                + ", patternProperties="
                + patternProperties
                + '}';
    }
}
