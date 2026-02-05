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

/** Represent the metadata extracted from a shape in a SHACL. */
public class ShaclShapeMetadata {

    private String shapeId;
    private String targetClass;
    private String vocabularyUri;
    private String contextUri;
    private String ontologyUri;

    public String getShapeId() {
        return shapeId;
    }

    public void setShapeId(String shapeId) {
        this.shapeId = shapeId;
    }

    public String getTargetClass() {
        return targetClass;
    }

    public void setTargetClass(String targetClass) {
        this.targetClass = targetClass;
    }

    public String getVocabularyUri() {
        return vocabularyUri;
    }

    public void setVocabularyUri(String vocabularyUri) {
        this.vocabularyUri = vocabularyUri;
    }

    public String getContextUri() {
        return contextUri;
    }

    public void setContextUri(String contextUri) {
        this.contextUri = contextUri;
    }

    public String getOntologyUri() {
        return ontologyUri;
    }

    public void setOntologyUri(String ontologyUri) {
        this.ontologyUri = ontologyUri;
    }

    @Override
    public String toString() {
        return "ShaclShapeMetadata{"
                + "shapeId='"
                + shapeId
                + '\''
                + ", targetClass='"
                + targetClass
                + '\''
                + ", vocabularyUri='"
                + vocabularyUri
                + '\''
                + ", contextUri='"
                + contextUri
                + '\''
                + ", ontologyUri='"
                + ontologyUri
                + '\''
                + '}';
    }
}
