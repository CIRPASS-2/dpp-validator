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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.util.StringUtil;
import io.quarkus.test.TestReactiveTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;
import it.extared.dpp.validator.dto.*;
import it.extared.dpp.validator.json.JsonPropertyExtractor;
import it.extared.dpp.validator.json.JsonSchemaMetadataExtractor;
import it.extared.dpp.validator.json.dto.SchemaMetadata;
import it.extared.dpp.validator.utils.CommonUtils;
import it.extrared.dpp.validator.datastore.pgsql.PgSQLJsonSchemaRepository;
import jakarta.inject.Inject;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class PgSQLJsonSchemaRepositoryTest {

    @Inject Pool pool;

    @Inject PgSQLJsonSchemaRepository repository;

    @Inject JsonPropertyExtractor extractor;

    @Inject JsonSchemaMetadataExtractor metadataExtractor;

    @Inject ObjectMapper objectMapper;

    @Test
    @RunOnVertxContext
    @TestReactiveTransaction
    public void testFindBestMatch(UniAsserter asserter) {
        Set<String> props = extractor.extractAllPaths(CommonUtils.readJsonNode("battery.json"));
        Uni<MatchResult<JsonNode>> jsonNodeUni =
                pool.withTransaction(
                        c -> repository.findBestMatch(c, props.toArray(new String[0])));
        asserter.assertThat(
                () -> jsonNodeUni,
                mr -> {
                    assertEquals("battery_passport", mr.getName());
                    assertEquals("1.0.0", mr.getVersion());
                    assertEquals(MatchType.SIMILARITY_MATCH, mr.getMatchType());
                    assertNotNull(mr.getResource());
                });
    }

    @Test
    @RunOnVertxContext
    @TestReactiveTransaction
    public void testFindBestMatchWithVariants(UniAsserter asserter) {
        Set<String> props = extractor.extractAllPaths(CommonUtils.readJsonNode("electronics.json"));
        Uni<MatchResult<JsonNode>> jsonNodeUni =
                pool.withTransaction(
                        c -> repository.findBestMatch(c, props.toArray(new String[0])));
        asserter.assertThat(
                () -> jsonNodeUni,
                mr -> {
                    assertEquals("electronics_dpp", mr.getName());
                    assertEquals("1.0.0", mr.getVersion());
                    assertEquals(MatchType.SIMILARITY_MATCH, mr.getMatchType());
                    assertNotNull(mr.getResource());
                });
    }

    @Test
    @RunOnVertxContext
    @TestReactiveTransaction
    public void testFindBestMatchWithPatterns(UniAsserter asserter) {
        Set<String> props = extractor.extractAllPaths(CommonUtils.readJsonNode("vehicle.json"));
        Uni<MatchResult<JsonNode>> jsonNodeUni =
                pool.withTransaction(
                        c -> repository.findBestMatch(c, props.toArray(new String[0])));
        asserter.assertThat(
                () -> jsonNodeUni,
                mr -> {
                    assertEquals("vehicle_dpp", mr.getName());
                    assertEquals("1.0.0", mr.getVersion());
                    assertEquals(MatchType.SIMILARITY_MATCH, mr.getMatchType());
                    assertNotNull(mr.getResource());
                });
    }

    @Test
    @RunOnVertxContext
    @TestReactiveTransaction
    public void testSearch(UniAsserter asserter) {
        SearchDto searchDto = SearchDto.builder().withName("dpp").withVersion("1.0").build();
        Uni<PagedResult<ResourceMetadata>> resultUni =
                pool.withConnection(c -> repository.search(c, searchDto));
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
    public void testGetByNameAndVersion(UniAsserter asserter) {
        Uni<MatchResult<JsonNode>> matchResultUni =
                pool.withConnection(
                        c -> repository.findByNameAndVersion(c, "vehicle_dpp", "1.0.0"));
        asserter.assertThat(
                () -> matchResultUni,
                mr -> {
                    assertNotNull(mr.getResource());
                    assertEquals(MatchType.NAME_AND_VERSION, mr.getMatchType());
                });
    }

    @Test
    @RunOnVertxContext
    @TestReactiveTransaction
    public void testAddGetDeleteSchema(UniAsserter asserter) {
        JsonNode jsonNode = CommonUtils.readJsonSchemaNode("test-addition.json");
        SchemaMetadata metadata = metadataExtractor.extractMetadata(jsonNode);
        ResourceMetadata resourceMetadata =
                new ResourceMetadata(
                        "Test addition", "A schema to test add functionality", "1.0.0");
        Uni<Long> idUni =
                pool.withTransaction(
                        c -> repository.addJsonSchema(c, resourceMetadata, metadata, jsonNode));
        Uni<Void> result =
                idUni.invoke(Assertions::assertNotNull).call(this::getAndDelete).replaceWithVoid();
        asserter.assertNull(() -> result);
    }

    private Uni<Void> getAndDelete(Long id) {
        return pool.withConnection(c -> repository.findById(c, id))
                .invoke(st -> assertFalse(StringUtil.isNullOrEmpty(st)))
                .call(s -> pool.withTransaction(c -> repository.deleteSchema(c, id)))
                .replaceWithVoid();
    }
}
