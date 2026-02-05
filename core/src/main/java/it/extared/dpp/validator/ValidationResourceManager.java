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
package it.extared.dpp.validator;

import io.smallrye.mutiny.Uni;
import it.extared.dpp.validator.dto.PagedResult;
import it.extared.dpp.validator.dto.ResourceMetadata;
import it.extared.dpp.validator.dto.SearchDto;
import it.extared.dpp.validator.dto.TypedResource;
import java.io.IOException;
import java.io.InputStream;

public interface ValidationResourceManager {

    /**
     * Given a validation resource and its metadata it should persist it.
     *
     * @param resourceMetadata the metadata of the resource as a {@link ResourceMetadata}
     * @param resource the actual validation resource.
     * @return the unique numeric identifier of the persisted resource.
     * @throws IOException if something goes wrong reading the input or persisting it.
     */
    Uni<Long> addValidationResource(ResourceMetadata resourceMetadata, InputStream resource)
            throws IOException;

    /**
     * Remove a validation resource by its unique numeric identifier.
     *
     * @param resourceId the unique numeric identifer.
     * @return empty uni
     */
    Uni<Void> removeValidationResource(Long resourceId);

    /**
     * Give search parameters return the metadata of the matched validation results as a {@link
     * PagedResult<ResourceMetadata>}
     *
     * @param searchDto an instace of {@link SearchDto} containing search parameters.
     * @return the result of the search.
     */
    Uni<PagedResult<ResourceMetadata>> search(SearchDto searchDto);

    /**
     * Return the validation resource associated with the input unique numeric id.
     *
     * @param id the unique numeric identifier of the resource to be retrieved.
     * @return the validation resource as a {@link TypedResource}
     */
    Uni<TypedResource> getResourceById(Long id);

    /**
     * Return the validation resource associated with the input name and version.
     *
     * @param name the name of the resource to be retrieved.
     * @param version the version of the resource to be retrieved
     * @return the validation resource as a {@link TypedResource}
     */
    Uni<TypedResource> getByNameAndVersion(String name, String version);

    /**
     * @param type the type of the validation associated to the resources handled by a manager.
     * @return true if its supports it, false otherwise.
     */
    boolean canHandle(ValidationType type);

    /**
     * Priority of the ResourceManager implementation, where 0 is highest priority.
     *
     * @return the priority number.
     */
    Integer priority();
}
