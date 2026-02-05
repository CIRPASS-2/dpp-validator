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

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import it.extared.dpp.validator.ValidationType;
import it.extared.dpp.validator.dto.ValidationReport;
import it.extared.dpp.validator.utils.CommonUtils;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ValidationAPITest {

    @Test
    public void testJSONSchemaValidation() {
        ValidationReport report =
                given().when()
                        .contentType(ContentType.JSON)
                        .body(CommonUtils.readJsonBytes("valid-vehicle.json"))
                        .post("/validate/v1")
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .as(ValidationReport.class);
        assertTrue(report.isValid());
        assertEquals(ValidationType.PLAIN_JSON, report.getValidationType());
        assertEquals("vehicle_dpp - 1.0.0", report.getValidatedWith());
        assertEquals(
                "Validation performed using template found by SIMILARITY_MATCH",
                report.getMessage());
    }

    @Test
    public void testJsonSchemaValidation2() {
        ValidationReport report =
                given().when()
                        .contentType(ContentType.JSON)
                        .body(CommonUtils.readJsonBytes("invalid-vehicle.json"))
                        .post("/validate/v1")
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .as(ValidationReport.class);
        assertFalse(report.isValid());
        assertEquals(ValidationType.PLAIN_JSON, report.getValidationType());
        assertEquals("vehicle_dpp - 1.0.0", report.getValidatedWith());
        assertEquals(
                "Validation performed using template found by SIMILARITY_MATCH",
                report.getMessage());
        assertEquals(3, report.getInvalidProperties().size());
    }

    @Test
    public void testJSONLDTemplateValidation() {
        ValidationReport report =
                given().when()
                        .contentType("application/ld+json")
                        .body(CommonUtils.readJsonLdString("valid-type-match-ld.json").getBytes())
                        .post("/validate/v1")
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .as(ValidationReport.class);
        assertTrue(report.isValid());
        assertEquals(ValidationType.RDF, report.getValidationType());
        assertEquals("Vehicle-DPP-AllTargets - 1.0.0", report.getValidatedWith());
        assertEquals(
                "Validation performed using template found by EXACT_TYPE_MATCH",
                report.getMessage());
    }

    @Test
    public void testJSONLDTemplateValidation2() {
        ValidationReport report =
                given().when()
                        .contentType("application/ld+json")
                        .body(CommonUtils.readJsonLdString("invalid-type-match-ld.json").getBytes())
                        .post("/validate/v1")
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .as(ValidationReport.class);
        assertFalse(report.isValid());
        assertEquals(ValidationType.RDF, report.getValidationType());
        assertEquals("Vehicle-DPP-AllTargets - 1.0.0", report.getValidatedWith());
        assertEquals(
                "Validation performed using template found by EXACT_TYPE_MATCH",
                report.getMessage());
        assertEquals(8, report.getInvalidProperties().size());
    }

    @Test
    public void testJSONSchemaValidationByNameAndVersion() {
        ValidationReport report =
                given().when()
                        .contentType(ContentType.JSON)
                        .body(CommonUtils.readJsonBytes("valid-vehicle.json"))
                        .post("/validate/v1/vehicle_dpp/1.0.0")
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .as(ValidationReport.class);
        assertTrue(report.isValid());
        assertEquals(ValidationType.PLAIN_JSON, report.getValidationType());
        assertEquals("vehicle_dpp - 1.0.0", report.getValidatedWith());
        assertEquals(
                "Validation performed using template found by NAME_AND_VERSION",
                report.getMessage());
    }

    @Test
    public void testJSONLDTemplateValidationByNameAndVersion() {
        ValidationReport report =
                given().when()
                        .contentType("application/ld+json")
                        .body(CommonUtils.readJsonLdString("valid-type-match-ld.json").getBytes())
                        .post("/validate/v1/Vehicle-DPP-AllTargets/1.0.0")
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .as(ValidationReport.class);
        assertTrue(report.isValid());
        assertEquals(ValidationType.RDF, report.getValidationType());
        assertEquals("Vehicle-DPP-AllTargets - 1.0.0", report.getValidatedWith());
        assertEquals(
                "Validation performed using template found by NAME_AND_VERSION",
                report.getMessage());
    }
}
