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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.test.junit.QuarkusTest;
import it.extared.dpp.validator.json.JsonPropertyExtractor;
import it.extared.dpp.validator.utils.CommonUtils;
import jakarta.inject.Inject;
import java.util.Set;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class JsonPropertyExtractorTest {

    @Inject JsonPropertyExtractor extractor;

    @Test
    public void testJsonPropertyExtraction() throws JsonProcessingException {
        Set<String> props =
                extractor.extractAllPaths(CommonUtils.readJsonNode("valid-battery.json"));
        assertEquals(39, props.size());
        props = extractor.extractAllPaths(CommonUtils.readJsonNode("invalid-battery.json"));
        assertEquals(42, props.size());
    }
}
