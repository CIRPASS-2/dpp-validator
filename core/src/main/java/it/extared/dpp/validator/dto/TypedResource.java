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
package it.extared.dpp.validator.dto;

import it.extared.dpp.validator.ValidationType;

/**
 * Represent a validation resource as a String togheter with its suppoerted {@link ValidationType}
 */
public class TypedResource {

    private String content;

    private ValidationType validationType;

    public String getContent() {
        return content;
    }

    public ValidationType getResourceType() {
        return validationType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final TypedResource resource;

        public Builder() {
            this.resource = new TypedResource();
        }

        public Builder withContent(String content) {
            resource.content = content;
            return this;
        }

        public Builder withResourceType(ValidationType type) {
            resource.validationType = type;
            return this;
        }

        public TypedResource build() {
            return resource;
        }
    }
}
