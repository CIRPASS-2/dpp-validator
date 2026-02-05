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
package it.extrared.dpp.validator.mocks;

import io.quarkus.arc.Unremovable;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.SqlConnection;
import it.extared.dpp.validator.dto.*;
import it.extared.dpp.validator.jsonld.ShaclTemplateRepository;
import it.extared.dpp.validator.jsonld.dto.InputJsonLdMetadata;
import it.extared.dpp.validator.jsonld.dto.ShaclShapeMetadata;
import it.extared.dpp.validator.utils.CommonUtils;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@Unremovable
@ApplicationScoped
public class MockShaclRepository implements ShaclTemplateRepository {

    @Override
    public Uni<PagedResult<ResourceMetadata>> search(SqlConnection conn, SearchDto searchDto) {
        List<ResourceMetadata> resources =
                List.of(
                        new TemplateResourceMetadata(1L, "res-1", "desc-1", "1.0", null),
                        new TemplateResourceMetadata(1L, "res-2", "desc-2", "1.0", null),
                        new TemplateResourceMetadata(
                                1L, "res-3", "desc-3", "1.0", "http://context.ld"));
        PagedResult.Builder<ResourceMetadata> builder = PagedResult.builder();
        return Uni.createFrom()
                .item(
                        builder.withElements(resources)
                                .withPageSize(3)
                                .withTotalElements(3L)
                                .build());
    }

    @Override
    public Uni<Void> deleteTemplate(SqlConnection conn, Long id) {
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<MatchResult<String>> findBestMatch(
            SqlConnection conn, InputJsonLdMetadata jsonLdMetadata) {
        return Uni.createFrom()
                .item(
                        new MatchResult<>(
                                "Vehicle DPP",
                                "1.1.0",
                                CommonUtils.readShaclString("vehicle_shacl.ttl"),
                                MatchType.EXACT_TYPE_MATCH));
    }

    @Override
    public Uni<MatchResult<String>> findByNameAndVersion(
            SqlConnection conn, String name, String version) {
        return Uni.createFrom()
                .item(
                        new MatchResult<>(
                                "Vehicle DPP",
                                "1.1.0",
                                CommonUtils.readShaclString("vehicle_shacl.ttl"),
                                MatchType.NAME_AND_VERSION));
    }

    @Override
    public Uni<Long> addShaclTemplate(
            SqlConnection conn,
            ResourceMetadata resourceMetadata,
            List<ShaclShapeMetadata> metadataList,
            String template) {
        return Uni.createFrom().item(1L);
    }

    @Override
    public Uni<String> findById(SqlConnection connection, Long id) {
        return Uni.createFrom().item(CommonUtils.readShaclString("vehicle_shacl.ttl"));
    }
}
