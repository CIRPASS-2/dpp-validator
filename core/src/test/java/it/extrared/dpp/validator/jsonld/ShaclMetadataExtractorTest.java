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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.test.junit.QuarkusTest;
import it.extared.dpp.validator.jsonld.ShaclMetadataExtractor;
import it.extared.dpp.validator.jsonld.dto.ShaclShapeMetadata;
import it.extared.dpp.validator.utils.CommonUtils;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ShaclMetadataExtractorTest {

    @Inject ShaclMetadataExtractor extractor;

    @Test
    public void testMetadataExtraction() {
        String shacl = CommonUtils.readShaclString("vehicle_shacl.ttl");
        List<ShaclShapeMetadata> shapeMetadata = extractor.extractAllShapes(shacl);
        shapeMetadata =
                shapeMetadata.stream()
                        .peek(
                                sm -> {
                                    assertNotNull(sm.getTargetClass());
                                    assertNotNull(sm.getVocabularyUri());
                                })
                        .toList();
        assertEquals(14L, shapeMetadata.size());
    }

    @Test
    public void testMetadataExtraction2() {
        String shacl = CommonUtils.readShaclString("electronics_shacl.ttl");
        List<ShaclShapeMetadata> shapeMetadata = extractor.extractAllShapes(shacl);
        assertEquals(18L, shapeMetadata.size());
        assertEquals(1L, shapeMetadata.stream().filter(m -> m.getTargetClass() != null).count());
    }

    @Test
    public void testMetadataExtraction3() {
        String shacl = CommonUtils.readShaclString("battery_pass_shacl.ttl");
        List<ShaclShapeMetadata> shapeMetadata = extractor.extractAllShapes(shacl);
        assertEquals(14L, shapeMetadata.size());
        assertEquals(4L, shapeMetadata.stream().filter(m -> m.getTargetClass() != null).count());
    }
}
