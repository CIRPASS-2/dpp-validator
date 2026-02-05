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

import java.util.List;

/**
 * Dto class for a search result with pagination information.
 *
 * @param <T>
 */
public class PagedResult<T> {

    private List<T> elements;

    private Long totalElements;

    private Integer pageSize;

    public List<T> getElements() {
        return elements;
    }

    public Long getTotalElements() {
        return totalElements;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    /**
     * Create a builder for a Paged Result.
     *
     * @return the builder.
     * @param <T>
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * The Builder of a PagedResult.
     *
     * @param <T>
     */
    public static class Builder<T> {
        private final PagedResult<T> result;

        private Builder() {
            this.result = new PagedResult<>();
        }

        public Builder<T> withElements(List<T> elements) {
            this.result.elements = elements;
            return this;
        }

        public Builder<T> withTotalElements(Long totalElements) {
            this.result.totalElements = totalElements;
            return this;
        }

        public Builder<T> withPageSize(Integer pageSize) {
            this.result.pageSize = pageSize;
            return this;
        }

        public PagedResult<T> build() {
            return result;
        }
    }
}
