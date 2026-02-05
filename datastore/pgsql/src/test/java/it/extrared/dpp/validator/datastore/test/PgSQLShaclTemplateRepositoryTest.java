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
package it.extrared.dpp.validator.datastore.test;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.runtime.util.StringUtil;
import io.quarkus.test.TestReactiveTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;
import it.extared.dpp.validator.dto.*;
import it.extared.dpp.validator.jsonld.JsonLdMetadataExtractor;
import it.extared.dpp.validator.jsonld.ShaclMetadataExtractor;
import it.extared.dpp.validator.jsonld.ShaclTemplateRepository;
import it.extared.dpp.validator.jsonld.dto.InputJsonLdMetadata;
import it.extared.dpp.validator.jsonld.dto.ShaclShapeMetadata;
import it.extared.dpp.validator.utils.CommonUtils;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class PgSQLShaclTemplateRepositoryTest {

    @Inject Pool pool;
    @Inject JsonLdMetadataExtractor extractor;
    @Inject ShaclMetadataExtractor shaclMetadataExtractor;

    @Inject ShaclTemplateRepository templateRepository;

    @Test
    @RunOnVertxContext
    @TestReactiveTransaction
    public void testExactMatch(UniAsserter asserter) {
        Uni<InputJsonLdMetadata> metadataUni =
                extractor.extractMetadataDeferred(
                        CommonUtils.readJsonLdString("type-match-ld.json"));
        Uni<MatchResult<String>> result =
                metadataUni.flatMap(
                        i -> pool.withConnection(c -> templateRepository.findBestMatch(c, i)));
        asserter.assertThat(
                () -> result,
                r -> {
                    assertEquals(MatchType.EXACT_TYPE_MATCH, r.getMatchType());
                });
    }

    @Test
    @RunOnVertxContext
    @TestReactiveTransaction
    public void testVocabularyMatch(UniAsserter asserter) {
        Uni<InputJsonLdMetadata> metadataUni =
                extractor.extractMetadataDeferred(
                        CommonUtils.readJsonLdString("vocabulary-match-ld.json"));
        Uni<MatchResult<String>> result =
                metadataUni.flatMap(
                        i -> pool.withConnection(c -> templateRepository.findBestMatch(c, i)));
        asserter.assertThat(
                () -> result,
                r -> {
                    assertEquals(MatchType.VOCABULARY_MATCH, r.getMatchType());
                });
    }

    @Test
    @RunOnVertxContext
    @TestReactiveTransaction
    public void testSearch(UniAsserter asserter) {
        SearchDto searchDto = SearchDto.builder().withName("dpp").withVersion("1.0").build();
        Uni<PagedResult<ResourceMetadata>> resultUni =
                pool.withConnection(c -> templateRepository.search(c, searchDto));
        asserter.assertThat(
                () -> resultUni,
                r -> {
                    assertEquals(2, r.getTotalElements());
                    assertEquals(2, r.getElements().size());
                });
    }

    @Test
    @RunOnVertxContext
    @TestReactiveTransaction
    public void testAddGetDeleteShaclTemplate(UniAsserter asserter) {
        String shacl = CommonUtils.readShaclString("test-addition.ttl");
        List<ShaclShapeMetadata> shapes = shaclMetadataExtractor.extractAllShapes(shacl);
        TemplateResourceMetadata resourceMetadata =
                new TemplateResourceMetadata(
                        "Test addition", "A shacl to test add functionality", "1.0.0", null);
        Uni<Long> idUni =
                pool.withTransaction(
                        c ->
                                templateRepository.addShaclTemplate(
                                        c, resourceMetadata, shapes, shacl));
        Uni<Void> result =
                idUni.invoke(Assertions::assertNotNull).call(this::getAndDelete).replaceWithVoid();
        asserter.assertNull(() -> result);
    }

    @Test
    @RunOnVertxContext
    @TestReactiveTransaction
    public void testGetByNameAndVersion(UniAsserter asserter) {
        Uni<MatchResult<String>> matchResultUni =
                pool.withConnection(
                        c ->
                                templateRepository.findByNameAndVersion(
                                        c, "Vehicle-DPP-AllTargets", "1.0.0"));
        asserter.assertThat(
                () -> matchResultUni,
                mr -> {
                    assertNotNull(mr.getResource());
                    assertEquals(MatchType.NAME_AND_VERSION, mr.getMatchType());
                });
    }

    private Uni<Void> getAndDelete(Long id) {
        return pool.withConnection(c -> templateRepository.findById(c, id))
                .invoke(st -> assertFalse(StringUtil.isNullOrEmpty(st)))
                .call(s -> pool.withTransaction(c -> templateRepository.deleteTemplate(c, id)))
                .replaceWithVoid();
    }
}
