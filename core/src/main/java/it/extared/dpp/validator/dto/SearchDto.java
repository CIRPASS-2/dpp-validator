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

public class SearchDto {

    private String name;
    private String description;
    private String version;
    private Integer offset;
    private Integer limit;

    public SearchDto() {
        this.offset = 0;
        this.limit = 20;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getVersion() {
        return version;
    }

    public Integer getOffset() {
        return offset;
    }

    public Integer getLimit() {
        return limit;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final SearchDto searchDto;

        private Builder() {
            this.searchDto = new SearchDto();
        }

        public Builder withOffset(Integer offset) {
            if (offset != null) this.searchDto.offset = offset;
            return this;
        }

        public Builder withLimit(Integer limit) {
            if (limit != null) this.searchDto.limit = limit;
            return this;
        }

        public Builder withName(String name) {
            this.searchDto.name = name;
            return this;
        }

        public Builder withDescription(String description) {
            this.searchDto.description = description;
            return this;
        }

        public Builder withVersion(String version) {
            this.searchDto.version = version;
            return this;
        }

        public SearchDto build() {
            return this.searchDto;
        }
    }

    @Override
    public String toString() {
        return "SearchDto{"
                + "name='"
                + name
                + '\''
                + ", description='"
                + description
                + '\''
                + ", version='"
                + version
                + '\''
                + ", offset="
                + offset
                + ", limit="
                + limit
                + '}';
    }
}
