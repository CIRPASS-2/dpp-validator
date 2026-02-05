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
package it.extared.dpp.validator.jsonld;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;
import it.extared.dpp.validator.ValidationResourceManager;
import it.extared.dpp.validator.ValidationType;
import it.extared.dpp.validator.dto.*;
import it.extared.dpp.validator.jsonld.dto.ShaclShapeMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class ShaclTemplateManager implements ValidationResourceManager {

    @Inject Pool pool;

    @Inject ShaclTemplateRepository shaclTemplateRepository;

    @Inject ShaclMetadataExtractor metadataExtractor;

    @Inject ObjectMapper objectMapper;

    @Override
    public Uni<Long> addValidationResource(ResourceMetadata resourceMetadata, InputStream resource)
            throws IOException {
        byte[] content = resource.readAllBytes();
        String strContent = new String(content);
        List<ShaclShapeMetadata> metadataList =
                metadataExtractor.extractAllShapes(new String(content));
        return pool.withTransaction(
                c ->
                        shaclTemplateRepository.addShaclTemplate(
                                c, resourceMetadata, metadataList, strContent));
    }

    @Override
    public Uni<Void> removeValidationResource(Long resourceId) {
        return pool.withConnection(c -> shaclTemplateRepository.deleteTemplate(c, resourceId));
    }

    @Override
    public Uni<PagedResult<ResourceMetadata>> search(SearchDto searchDto) {
        return pool.withConnection(c -> shaclTemplateRepository.search(c, searchDto));
    }

    @Override
    public Uni<TypedResource> getResourceById(Long id) {
        return pool.withConnection(
                c ->
                        shaclTemplateRepository
                                .findById(c, id)
                                .map(
                                        s ->
                                                TypedResource.builder()
                                                        .withResourceType(ValidationType.RDF)
                                                        .withContent(s)
                                                        .build()));
    }

    @Override
    public Uni<TypedResource> getByNameAndVersion(String name, String version) {
        Uni<MatchResult<String>> matchResult =
                pool.withConnection(
                        c -> shaclTemplateRepository.findByNameAndVersion(c, name, version));
        return matchResult.map(
                m ->
                        TypedResource.builder()
                                .withResourceType(ValidationType.RDF)
                                .withContent(m.getResource())
                                .build());
    }

    @Override
    public boolean canHandle(ValidationType validationType) {
        return Objects.equals(validationType, ValidationType.RDF);
    }

    @Override
    public Integer priority() {
        return 99;
    }
}
