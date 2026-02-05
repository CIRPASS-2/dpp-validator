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
package it.extared.dpp.validator.jsonld.dto;

/** Represents the metadata extracted from a JSON-LD to be validated. */
public class InputJsonLdMetadata {
    private String type;
    private String contextUri;
    private String vocabularyUri;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContextUri() {
        return contextUri;
    }

    public void setContextUri(String contextUri) {
        this.contextUri = contextUri;
    }

    public String getVocabularyUri() {
        return vocabularyUri;
    }

    public void setVocabularyUri(String vocabularyUri) {
        this.vocabularyUri = vocabularyUri;
    }

    @Override
    public String toString() {
        return "InputJsonLdMetadata{"
                + "type='"
                + type
                + '\''
                + ", contextUri='"
                + contextUri
                + '\''
                + ", vocabularyUri='"
                + vocabularyUri
                + '}';
    }
}
