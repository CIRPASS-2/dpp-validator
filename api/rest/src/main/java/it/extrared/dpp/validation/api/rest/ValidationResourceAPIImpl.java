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
import it.extared.dpp.validator.ValidatorService;
import it.extared.dpp.validator.dto.PagedResult;
import it.extared.dpp.validator.dto.ResourceMetadata;
import it.extared.dpp.validator.dto.SearchDto;
import it.extared.dpp.validator.dto.TypedResource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import org.jboss.resteasy.reactive.RestResponse;

@ApplicationScoped
public class ValidationResourceAPIImpl implements ValidationResourceAPI {
    @Inject ValidatorService service;

    @Override
    public Uni<RestResponse<Long>> addValidationResource(
            PayloadType resourceType, InputStream content, ResourceMetadata metadata)
            throws IOException {
        return service.addValidationResource(
                        metadata, resourceType.convert(content), resourceType.asValidationType())
                .map(
                        id ->
                                RestResponse.ResponseBuilder.create(Response.Status.CREATED, id)
                                        .build());
    }

    @Override
    public Uni<RestResponse<String>> getResourceById(ResourceType resourceType, Long id) {
        return service.getResourceContent(id, resourceType.asValidationType())
                .map(this::asResponse);
    }

    @Override
    public Uni<PagedResult<ResourceMetadata>> search(
            ResourceType resourceType, SearchParams params) {
        return service.searchResources(
                params == null ? new SearchDto() : params.toDto(), resourceType.asValidationType());
    }

    @Override
    public Uni<Void> deleteValidationResource(ResourceType resourceType, Long id) {
        return service.deleteValidationResource(id, resourceType.asValidationType());
    }

    @Override
    public Uni<RestResponse<String>> getResourceByNameAndVersion(
            ResourceType resourceType, String resourceName, String resourceVersion, Long id) {
        return service.getResourceContentByNameAndVersion(
                        resourceName, resourceVersion, resourceType.asValidationType())
                .map(this::asResponse);
    }

    private RestResponse<String> asResponse(TypedResource tr) {
        return RestResponse.ResponseBuilder.ok(tr.getContent())
                .header("Content-Type", tr.getResourceType().getContentType())
                .build();
    }
}
