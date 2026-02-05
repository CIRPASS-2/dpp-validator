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
package it.extrared.dpp.validator.json;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.test.junit.QuarkusTest;
import it.extared.dpp.validator.json.JsonSchemaMetadataExtractor;
import it.extared.dpp.validator.json.dto.SchemaMetadata;
import it.extared.dpp.validator.utils.CommonUtils;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class JsonSchemaMetadataExtractorTest {

    @Inject JsonSchemaMetadataExtractor extractor;

    @Test
    public void testExtraction() throws JsonProcessingException {
        SchemaMetadata schema =
                extractor.extractMetadata(
                        CommonUtils.readJsonSchemaNode("test-schema-simple.json"));
        assertEquals(28, schema.getRequiredPaths().size());
        assertFalse(schema.isHasVariants());
    }

    @Test
    public void testExtraction2() {
        SchemaMetadata schema =
                extractor.extractMetadata(
                        CommonUtils.readJsonSchemaNode("test-schema-variants.json"));
        assertEquals(30, schema.getRequiredPaths().size());
        assertTrue(schema.isHasVariants());
        assertEquals(3, schema.getVariants().size());
    }

    @Test
    public void testExtraction3() {
        SchemaMetadata schema =
                extractor.extractMetadata(
                        CommonUtils.readJsonSchemaNode("test-schema-pattern-props.json"));
        assertEquals(28, schema.getRequiredPaths().size());
        assertFalse(schema.isHasVariants());
        assertEquals(4, schema.getPatternProperties().size());
    }
}
