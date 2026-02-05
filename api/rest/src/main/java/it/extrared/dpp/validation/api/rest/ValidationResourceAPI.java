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
package it.extrared.dpp.validation.api.rest;

import io.smallrye.mutiny.Uni;
import it.extared.dpp.validator.dto.PagedResult;
import it.extared.dpp.validator.dto.ResourceMetadata;
import it.extared.dpp.validator.dto.SearchDto;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.jboss.resteasy.reactive.*;

/**
 * REST controller for method allowing to perform read/write operation over a valiation resource.
 */
@Path("/resource/v1")
public interface ValidationResourceAPI {

    class SearchParams {
        @RestQuery String name;
        @RestQuery String description;
        @RestQuery String version;
        @RestQuery Integer offset;
        @RestQuery Integer limit;

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

        public Integer getOffset() {
            return offset;
        }

        public void setOffset(Integer offset) {
            this.offset = offset;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public Integer getLimit() {
            return limit;
        }

        public void setLimit(Integer limit) {
            this.limit = limit;
        }

        public SearchDto toDto() {
            return SearchDto.builder()
                    .withName(getName())
                    .withDescription(getDescription())
                    .withVersion(getVersion())
                    .withLimit(getLimit())
                    .withOffset(getOffset())
                    .build();
        }

        public Map<String, Object> toMap() {
            Map<String, Object> params = new HashMap<>();
            params.put("name", getName());
            params.put("description", getDescription());
            params.put("version", getVersion());
            params.put("offset", getOffset());
            params.put("limit", getLimit());
            return params;
        }
    }

    @Operation(description = "Add a validation resource to the service repository.")
    @Parameter(
            in = ParameterIn.PATH,
            description =
                    "The payload type of the file part. Supported type values are: json,json_ld,turtle,rdf_xml,rdf_json,n_triples,n_quads,n3")
    @POST
    @Path("/{payloadType}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    Uni<RestResponse<Long>> addValidationResource(
            @RestPath PayloadType payloadType,
            @RestForm("file") InputStream content,
            @RestForm("meta") @PartType(MediaType.APPLICATION_JSON) @Valid
                    ResourceMetadata metadata)
            throws IOException;

    @Operation(description = "Retrieve a validation resource by its unique numeric identifier")
    @Parameter(
            in = ParameterIn.PATH,
            name = "resourceType",
            description = "The resource type to retrieve. One of template,schema.")
    @Parameter(
            in = ParameterIn.PATH,
            description = "The unique numeric identifier to retrieve a validation resource")
    @GET
    @Path("/{resourceType}/{id}")
    Uni<RestResponse<String>> getResourceById(
            @RestPath ResourceType resourceType, @RestPath Long id);

    @Operation(description = "Retrieve a validation resource by its name and version.")
    @Parameter(
            in = ParameterIn.PATH,
            name = "resourceType",
            description = "The resource type to retrieve. One of template,schema.")
    @GET
    @Path("/{resourceType}/{resourceName}/{resourceVersion}")
    Uni<RestResponse<String>> getResourceByNameAndVersion(
            @RestPath ResourceType resourceType,
            @RestPath String resourceName,
            @RestPath String resourceVersion,
            @RestPath Long id);

    @Operation(description = "Search for validation resources given input search parameters.")
    @Parameter(
            in = ParameterIn.PATH,
            name = "resourceType",
            description = "The resource type to search. One of template,schema.")
    @GET
    @Path("/{resourceType}")
    Uni<PagedResult<ResourceMetadata>> search(
            @RestPath ResourceType resourceType, @BeanParam SearchParams params);

    @Operation(description = "Delete a valiation resource by its unique numeric identifier.")
    @Parameter(
            in = ParameterIn.PATH,
            name = "resourceType",
            description = "The resource type to delete. One of template,schema.")
    @DELETE
    @Path("/{resourceType}/{id}")
    Uni<Void> deleteValidationResource(@RestPath ResourceType resourceType, @RestPath Long id);
}
