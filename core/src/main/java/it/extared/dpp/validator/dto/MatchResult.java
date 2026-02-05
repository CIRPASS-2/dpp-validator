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

import java.util.Objects;

/**
 * Represent a match result of a query searching for a validation resource.
 *
 * @param <T>
 */
public class MatchResult<T> {

    private String name;
    private String version;
    private T resource;

    private MatchType matchType;

    public MatchResult(MatchType matchType) {
        this.matchType = matchType;
    }

    public MatchResult(String name, String version, T resource, MatchType matchType) {
        this.name = name;
        this.version = version;
        this.resource = resource;
        this.matchType = matchType;
    }

    public T getResource() {
        return resource;
    }

    public void setResource(T resource) {
        this.resource = resource;
    }

    public MatchType getMatchType() {
        return matchType;
    }

    public void setMatchType(MatchType matchType) {
        this.matchType = matchType;
    }

    public boolean hasNoTemplate() {
        return Objects.equals(MatchType.NONE, matchType);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public static <T> MatchResult<T> emptyResult() {
        return new MatchResult<>(MatchType.NONE);
    }
}
