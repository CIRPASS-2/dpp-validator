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
import it.extared.dpp.validator.dto.*;
import it.extared.dpp.validator.exceptions.InvalidOpException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ValidatorService {

    @Inject Instance<Validator> validators;

    @Inject Instance<ValidationResourceManager> resourceManagers;

    private static final Logger LOGGER = Logger.getLogger(ValidatorService.class);

    public Uni<ValidationReport> validate(byte[] input, ValidationType validationType)
            throws IOException {
        return selectValidator(validationType).validate(input);
    }

    public Uni<ValidationReport> validate(
            String name, String version, byte[] input, ValidationType validationType)
            throws IOException {
        return selectValidator(validationType).validate(name, version, input);
    }

    public Uni<Long> addValidationResource(
            ResourceMetadata metadata, InputStream resource, ValidationType validationType)
            throws IOException {
        return selectResourceManager(validationType).addValidationResource(metadata, resource);
    }

    public Uni<Void> deleteValidationResource(Long id, ValidationType validationType) {
        return selectResourceManager(validationType).removeValidationResource(id);
    }

    public Uni<TypedResource> getResourceContentByNameAndVersion(
            String name, String version, ValidationType validationType) {
        return selectResourceManager(validationType).getByNameAndVersion(name, version);
    }

    public Uni<TypedResource> getResourceContent(Long id, ValidationType validationType) {
        return selectResourceManager(validationType).getResourceById(id);
    }

    public Uni<PagedResult<ResourceMetadata>> searchResources(
            SearchDto searchDto, ValidationType validationType) {
        return selectResourceManager(validationType).search(searchDto);
    }

    private Validator selectValidator(ValidationType validationType) {
        return validators.stream()
                .filter(v -> v.canHandle(validationType))
                .min(Comparator.comparingInt(Validator::priority))
                .map(
                        v -> {
                            LOGGER.debug(
                                    "selected validator %s"
                                            .formatted(v.getClass().getSimpleName()));
                            return v;
                        })
                .orElseThrow(
                        () ->
                                new InvalidOpException(
                                        "No validator found for type %s"
                                                .formatted(validationType)));
    }

    private ValidationResourceManager selectResourceManager(ValidationType validationType) {
        return resourceManagers.stream()
                .filter(rm -> rm.canHandle(validationType))
                .min(Comparator.comparingInt(ValidationResourceManager::priority))
                .map(
                        rm -> {
                            LOGGER.debug(
                                    "selected resource manager %s"
                                            .formatted(rm.getClass().getSimpleName()));
                            return rm;
                        })
                .orElseThrow(
                        () ->
                                new InvalidOpException(
                                        "No resource manager found for type %s"
                                                .formatted(validationType)));
    }
}
