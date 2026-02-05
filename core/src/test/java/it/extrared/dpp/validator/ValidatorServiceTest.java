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
package it.extrared.dpp.validator;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import it.extared.dpp.validator.ValidationType;
import it.extared.dpp.validator.ValidatorService;
import it.extared.dpp.validator.dto.*;
import it.extared.dpp.validator.utils.CommonUtils;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ValidatorServiceTest {

    @Inject ValidatorService service;

    @Inject ObjectMapper objectMapper;

    @Test
    @RunOnVertxContext
    public void testJSONValidation(UniAsserter uniAsserter) throws IOException {
        Uni<ValidationReport> reportUni =
                service.validate(
                        CommonUtils.readJsonBytes("valid-battery.json"), ValidationType.PLAIN_JSON);
        uniAsserter.assertTrue(() -> reportUni.map(ValidationReport::isValid));
    }

    @Test
    @RunOnVertxContext
    public void testFailingJSONValidation(UniAsserter uniAsserter) throws IOException {
        Uni<ValidationReport> reportUni =
                service.validate(
                        CommonUtils.readJsonBytes("invalid-battery.json"),
                        ValidationType.PLAIN_JSON);
        uniAsserter.assertThat(
                () -> reportUni,
                vr -> {
                    assertFalse(vr.isValid());
                    assertEquals(10, vr.getInvalidProperties().size());
                });
    }

    @Test
    @RunOnVertxContext
    public void testJSONValidationByNameAndVersion(UniAsserter uniAsserter) throws IOException {
        Uni<ValidationReport> reportUni =
                service.validate(
                        "battery_passport",
                        "1.0.0",
                        CommonUtils.readJsonBytes("valid-battery.json"),
                        ValidationType.PLAIN_JSON);
        uniAsserter.assertTrue(() -> reportUni.map(ValidationReport::isValid));
    }

    @Test
    @RunOnVertxContext
    public void testAddSchema(UniAsserter uniAsserter) throws IOException {
        ResourceMetadata metadata = new ResourceMetadata();
        metadata.setName("Test schema 1");
        metadata.setDescription("Simply a test schema 1");
        metadata.setVersion("1.0.0");
        ByteArrayInputStream bais =
                new ByteArrayInputStream(
                        CommonUtils.readJsonSchemaBytes("test-schema-simple.json"));
        Uni<Long> reportUni =
                service.addValidationResource(metadata, bais, ValidationType.PLAIN_JSON);
        uniAsserter.assertEquals(() -> reportUni, 1L);
    }

    @Test
    @RunOnVertxContext
    public void testGetSchema(UniAsserter uniAsserter) {
        Uni<TypedResource> resourceUni = service.getResourceContent(1L, ValidationType.PLAIN_JSON);
        uniAsserter.assertThat(
                () -> resourceUni,
                r -> {
                    assertNotNull(r);
                    assertNotNull(r.getContent());
                    assertEquals(ValidationType.PLAIN_JSON, r.getResourceType());
                });
    }

    @Test
    @RunOnVertxContext
    public void testDeleteSchema(UniAsserter uniAsserter) {
        Uni<Void> deleteUni = service.deleteValidationResource(1L, ValidationType.PLAIN_JSON);
        uniAsserter.assertNull(() -> deleteUni);
    }

    @Test
    @RunOnVertxContext
    public void testSearchSchemaResources(UniAsserter uniAsserter) {
        SearchDto searchDto =
                SearchDto.builder()
                        .withDescription("desc")
                        .withLimit(10)
                        .withOffset(0)
                        .withName("name")
                        .withVersion("1.0.0")
                        .build();
        Uni<PagedResult<ResourceMetadata>> resourceUni =
                service.searchResources(searchDto, ValidationType.PLAIN_JSON);
        uniAsserter.assertEquals(() -> resourceUni.map(l -> l.getElements().size()), 3);
    }

    @Test
    @RunOnVertxContext
    public void testJSONLDValidation(UniAsserter uniAsserter) throws IOException {
        Uni<ValidationReport> reportUni =
                service.validate(
                        CommonUtils.readJsonLdString("vehicle-ld.json").getBytes(),
                        ValidationType.RDF);
        uniAsserter.assertTrue(() -> reportUni.map(ValidationReport::isValid));
    }

    @Test
    @RunOnVertxContext
    public void testFailingJSONLDValidation(UniAsserter uniAsserter) throws IOException {
        Uni<ValidationReport> reportUni =
                service.validate(
                        CommonUtils.readJsonLdString("invalid-vehicle-ld.json").getBytes(),
                        ValidationType.RDF);
        uniAsserter.assertThat(
                () -> reportUni,
                vr -> {
                    assertFalse(vr.isValid());
                    assertEquals(1, vr.getInvalidProperties().size());
                });
    }

    @Test
    @RunOnVertxContext
    public void testJSONLDValidationByNameAndVersion(UniAsserter uniAsserter) throws IOException {
        Uni<ValidationReport> reportUni =
                service.validate(
                        "Vehicle DPP",
                        "1.0.0",
                        CommonUtils.readJsonLdString("vehicle-ld.json").getBytes(),
                        ValidationType.RDF);
        uniAsserter.assertTrue(() -> reportUni.map(ValidationReport::isValid));
    }

    @Test
    @RunOnVertxContext
    public void testAddTemplate(UniAsserter uniAsserter) throws IOException {
        ResourceMetadata metadata = new ResourceMetadata();
        metadata.setName("Test template 1");
        metadata.setDescription("Simply a test template 1");
        metadata.setVersion("1.0.0");
        ByteArrayInputStream bais =
                new ByteArrayInputStream(
                        CommonUtils.readShaclString("electronics_shacl.ttl").getBytes());
        Uni<Long> reportUni = service.addValidationResource(metadata, bais, ValidationType.RDF);
        uniAsserter.assertEquals(() -> reportUni, 1L);
    }

    @Test
    @RunOnVertxContext
    public void testGetTemplate(UniAsserter uniAsserter) {
        Uni<TypedResource> resourceUni = service.getResourceContent(1L, ValidationType.RDF);
        uniAsserter.assertThat(
                () -> resourceUni,
                r -> {
                    assertNotNull(r);
                    assertNotNull(r.getContent());
                    assertEquals(ValidationType.RDF, r.getResourceType());
                });
    }

    @Test
    @RunOnVertxContext
    public void testDeleteTemplate(UniAsserter uniAsserter) {
        Uni<Void> deleteUni = service.deleteValidationResource(1L, ValidationType.RDF);
        uniAsserter.assertNull(() -> deleteUni);
    }

    @Test
    @RunOnVertxContext
    public void testSearchTemplateResources(UniAsserter uniAsserter) {
        SearchDto searchDto =
                SearchDto.builder()
                        .withDescription("desc")
                        .withLimit(10)
                        .withOffset(0)
                        .withName("name")
                        .withVersion("1.0.0")
                        .build();
        Uni<PagedResult<ResourceMetadata>> resourceUni =
                service.searchResources(searchDto, ValidationType.RDF);
        uniAsserter.assertEquals(() -> resourceUni.map(l -> l.getElements().size()), 3);
    }
}
