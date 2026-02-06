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

import static it.extared.dpp.validator.dto.ResourceMetadata.BASE;
import static it.extared.dpp.validator.dto.ResourceMetadata.TEMPLATE;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Dto for resource metadata (id, name, description, version). */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "metadataType",
        visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ResourceMetadata.class, name = BASE),
    @JsonSubTypes.Type(value = TemplateResourceMetadata.class, name = TEMPLATE)
})
public class ResourceMetadata {

    public static final String BASE = "base";

    public static final String TEMPLATE = "template";

    private Long id;

    private String metadataType;

    @NotNull private String name;

    private String description;

    @NotNull
    @Pattern(
            regexp = "^\\d+(\\.\\d+)*$",
            message = "Version must be a valid version number (e.g., 1.0, 1.0.0, 1.2.3.4)")
    private String version;

    protected ResourceMetadata(
            String name, String description, String version, String metadataType) {
        this(metadataType);
        this.name = name;
        this.description = description;
        this.version = version;
    }

    protected ResourceMetadata(
            Long id, String name, String description, String version, String metadataType) {
        this(metadataType);
        this.id = id;
        this.name = name;
        this.description = description;
        this.version = version;
    }

    public ResourceMetadata(String name, String description, String version) {
        this();
        this.name = name;
        this.description = description;
        this.version = version;
    }

    public ResourceMetadata(Long id, String name, String description, String version) {
        this();
        this.id = id;
        this.name = name;
        this.description = description;
        this.version = version;
    }

    public ResourceMetadata() {
        this.metadataType = BASE;
    }

    protected ResourceMetadata(String metadataType) {
        this.metadataType = metadataType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMetadataType() {
        return metadataType;
    }

    public void setMetadataType(String metadataType) {
        this.metadataType = metadataType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
