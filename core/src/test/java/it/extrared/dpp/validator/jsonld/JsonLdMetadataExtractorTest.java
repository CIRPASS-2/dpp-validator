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
package it.extrared.dpp.validator.jsonld;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import it.extared.dpp.validator.jsonld.JsonLdMetadataExtractor;
import it.extared.dpp.validator.jsonld.dto.InputJsonLdMetadata;
import it.extared.dpp.validator.utils.CommonUtils;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class JsonLdMetadataExtractorTest {

    @Inject JsonLdMetadataExtractor extractor;

    @Test
    @RunOnVertxContext
    public void extractMetadata(UniAsserter asserter) {
        Uni<InputJsonLdMetadata> metadataUni =
                extractor.extractMetadataDeferred(CommonUtils.readJsonLdString("vehicle-ld.json"));
        asserter.assertThat(
                () -> metadataUni,
                m -> {
                    assertEquals("http://example.org/vehicle-dpp#", m.getVocabularyUri());
                    assertTrue(m.getType().startsWith("http"));
                    assertTrue(m.getType().endsWith("VehicleDPP"));
                });
    }

    @Test
    @RunOnVertxContext
    public void extractMetadataNoVocab(UniAsserter asserter) {
        Uni<InputJsonLdMetadata> metadataUni =
                extractor.extractMetadataDeferred(
                        CommonUtils.readJsonLdString("no-vocab-vehicle-ld.json"));
        asserter.assertThat(
                () -> metadataUni,
                m -> {
                    assertEquals("http://example.org/vehicle-dpp#", m.getVocabularyUri());
                    assertTrue(m.getType().startsWith("http"));
                    assertTrue(m.getType().endsWith("VehicleDPP"));
                });
    }
}
