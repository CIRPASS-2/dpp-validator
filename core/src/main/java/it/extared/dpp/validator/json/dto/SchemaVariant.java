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

import java.util.HashSet;
import java.util.Set;

/** Represent a schema variant in a JSON schema (anyOf,allOf,oneOf). */
public class SchemaVariant {

    private String variantType;
    private int variantIndex;
    private Set<String> requiredPaths = new HashSet<>();
    private String discriminatorPath;
    private String discriminatorValue;

    public String getVariantType() {
        return variantType;
    }

    public void setVariantType(String variantType) {
        this.variantType = variantType;
    }

    public Set<String> getRequiredPaths() {
        return requiredPaths;
    }

    public void setRequiredPaths(Set<String> requiredPaths) {
        this.requiredPaths = requiredPaths;
    }

    public int getVariantIndex() {
        return variantIndex;
    }

    public void setVariantIndex(int variantIndex) {
        this.variantIndex = variantIndex;
    }

    public String getDiscriminatorPath() {
        return discriminatorPath;
    }

    public void setDiscriminatorPath(String discriminatorPath) {
        this.discriminatorPath = discriminatorPath;
    }

    public String getDiscriminatorValue() {
        return discriminatorValue;
    }

    public void setDiscriminatorValue(String discriminatorValue) {
        this.discriminatorValue = discriminatorValue;
    }

    @Override
    public String toString() {
        return "SchemaVariant{"
                + "variantType='"
                + variantType
                + '\''
                + ", variantIndex="
                + variantIndex
                + ", requiredPaths="
                + requiredPaths
                + ", discriminatorPath='"
                + discriminatorPath
                + '\''
                + ", discriminatorValue='"
                + discriminatorValue
                + '\''
                + '}';
    }
}
