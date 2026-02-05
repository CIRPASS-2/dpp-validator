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
package it.extrared.dpp.validation.api.rest;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import com.cronutils.utils.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import it.extared.dpp.validator.dto.PagedResult;
import it.extared.dpp.validator.dto.ResourceMetadata;
import it.extared.dpp.validator.dto.TemplateResourceMetadata;
import it.extared.dpp.validator.utils.CommonUtils;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ValidationResourceAPITest {

    @Inject ObjectMapper objectMapper;

    @Test
    public void searchShaclResources() {
        ValidationResourceAPI.SearchParams params = new ValidationResourceAPI.SearchParams();
        params.setName("dpp");
        params.setVersion("1.0");
        params.setLimit(20);
        params.setOffset(0);
        PagedResult<ResourceMetadata> metadata =
                given().when()
                        .params(params.toMap())
                        .get("/resource/v1/%s".formatted(ResourceType.template.name()))
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .as(new TypeRef<>() {});
        assertEquals(2, metadata.getTotalElements());
        assertEquals(2, metadata.getElements().size());
    }

    @Test
    public void searchJsonResources() {
        ValidationResourceAPI.SearchParams params = new ValidationResourceAPI.SearchParams();
        params.setName("dpp");
        params.setVersion("1.0");
        params.setLimit(20);
        params.setOffset(0);
        PagedResult<ResourceMetadata> metadata =
                given().when()
                        .params(params.toMap())
                        .get("/resource/v1/%s".formatted(ResourceType.schema.name()))
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .as(new TypeRef<>() {});
        assertEquals(2, metadata.getTotalElements());
        assertEquals(2, metadata.getElements().size());
    }

    @Test
    public void searchShaclWithPagination() {
        ValidationResourceAPI.SearchParams params = new ValidationResourceAPI.SearchParams();
        params.setName("dpp");
        params.setVersion("1.0");
        params.setLimit(1);
        params.setOffset(0);
        PagedResult<ResourceMetadata> metadata =
                given().when()
                        .params(params.toMap())
                        .get("/resource/v1/%s".formatted(ResourceType.template.name()))
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .as(new TypeRef<>() {});
        assertEquals(2, metadata.getTotalElements());
        assertEquals(1, metadata.getElements().size());
        params.setOffset(1);
        metadata =
                given().when()
                        .params(params.toMap())
                        .get("/resource/v1/%s".formatted(ResourceType.template.name()))
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .as(new TypeRef<>() {});
        assertEquals(2, metadata.getTotalElements());
        assertEquals(1, metadata.getElements().size());
    }

    @Test
    public void searchJsonWithPagination() {
        ValidationResourceAPI.SearchParams params = new ValidationResourceAPI.SearchParams();
        params.setName("dpp");
        params.setVersion("1.0");
        params.setLimit(1);
        params.setOffset(0);
        PagedResult<ResourceMetadata> metadata =
                given().when()
                        .params(params.toMap())
                        .get("/resource/v1/%s".formatted(ResourceType.schema.name()))
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .as(new TypeRef<>() {});
        assertEquals(2, metadata.getTotalElements());
        assertEquals(1, metadata.getElements().size());

        params.setOffset(1);
        metadata =
                given().when()
                        .params(params.toMap())
                        .get("/resource/v1/%s".formatted(ResourceType.schema.name()))
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .as(new TypeRef<>() {});
        assertEquals(2, metadata.getTotalElements());
        assertEquals(1, metadata.getElements().size());
    }

    @Test
    public void addGetDeleteJsonSchema() throws JsonProcessingException {
        ResourceMetadata metadata =
                new ResourceMetadata(
                        "Test add get delete",
                        "This schema will be added, got and deleted",
                        "1.0.0");
        Long id =
                given().contentType(ContentType.MULTIPART)
                        .multiPart(
                                "meta",
                                objectMapper.writeValueAsString(metadata),
                                ContentType.JSON.toString())
                        .multiPart(
                                "file",
                                "test-addition.json",
                                CommonUtils.readJsonSchemaBytes("test-addition.json"),
                                ContentType.BINARY.toString())
                        .when()
                        .post("/resource/v1/%s".formatted(PayloadType.json))
                        .then()
                        .statusCode(201)
                        .extract()
                        .body()
                        .as(Long.class);
        String schema =
                given().when()
                        .get("/resource/v1/%s/%s".formatted(ResourceType.schema, id))
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .asString();
        assertFalse(StringUtils.isEmpty(schema));

        given().when()
                .delete("/resource/v1/%s/%s".formatted(ResourceType.schema, id))
                .then()
                .statusCode(204);
        given().when()
                .get("/resource/v1/%s/%s".formatted(ResourceType.schema, id))
                .then()
                .statusCode(404);
    }

    @Test
    public void addGetDeleteTemplate() throws JsonProcessingException {
        TemplateResourceMetadata metadata =
                new TemplateResourceMetadata(
                        "Test add get delete",
                        "This template will be added, got and deleted",
                        "1.0.0",
                        "http://context.ld");
        Long id =
                given().contentType(ContentType.MULTIPART)
                        .multiPart(
                                "meta",
                                objectMapper.writeValueAsString(metadata),
                                ContentType.JSON.toString())
                        .multiPart(
                                "file",
                                "test-addition.ttl",
                                CommonUtils.readShaclString("test-addition.ttl").getBytes(),
                                ContentType.BINARY.toString())
                        .when()
                        .post("/resource/v1/%s".formatted(PayloadType.turtle))
                        .then()
                        .statusCode(201)
                        .extract()
                        .body()
                        .as(Long.class);
        String schema =
                given().when()
                        .get("/resource/v1/%s/%s".formatted(ResourceType.template, id))
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .asString();
        assertFalse(StringUtils.isEmpty(schema));

        given().when()
                .delete("/resource/v1/%s/%s".formatted(ResourceType.template, id))
                .then()
                .statusCode(204);
        given().when()
                .get("/resource/v1/%s/%s".formatted(ResourceType.template, id))
                .then()
                .statusCode(404);
    }

    @Test
    public void testGetResourceByNameAndVersion() {
        String schema =
                given().when()
                        .get(
                                "/resource/v1/%s/%s/1.0.0"
                                        .formatted(ResourceType.template, "Vehicle-DPP-AllTargets"))
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .asString();
        assertFalse(StringUtils.isEmpty(schema));
    }

    @Test
    public void testGetSchemaByNameAndVersion() {
        String schema =
                given().when()
                        .get(
                                "/resource/v1/%s/%s/1.0.0"
                                        .formatted(ResourceType.schema, "vehicle_dpp"))
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .asString();
        assertFalse(StringUtils.isEmpty(schema));
    }
}
