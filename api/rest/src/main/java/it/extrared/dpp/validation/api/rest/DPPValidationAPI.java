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

import static it.extared.dpp.validator.utils.JsonLdUtils.APPLICATION_LD_JSON;
import static it.extared.dpp.validator.utils.JsonUtils.APPLICATION_JSON;
import static it.extared.dpp.validator.utils.JsonUtils.TEXT_JSON;

import io.smallrye.mutiny.Uni;
import it.extared.dpp.validator.dto.ValidationReport;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import java.io.IOException;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.jboss.resteasy.reactive.RestHeader;
import org.jboss.resteasy.reactive.RestPath;

/** REST controller for methods allowing to validate an input DPP. */
@Path("/validate/v1")
public interface DPPValidationAPI {

    @Operation(
            summary =
                    "Validates a DPP autodetecting the appropriate validation resource to apply to it.",
            description =
                    "Validates a DPP automatically finding the best matching validation resource in the service repository to validate it.")
    @POST
    @Consumes(value = {APPLICATION_JSON, TEXT_JSON, APPLICATION_LD_JSON})
    Uni<ValidationReport> validate(byte[] dpp, @RestHeader("Content-Type") String contentType)
            throws IOException;

    @Operation(
            summary =
                    "Validates a DPP retrieving the appropriate validation resource to apply to it, by name and version.",
            description =
                    "Validate a DPP using the name and the version to find the validation resource in the service repository to validate it.")
    @Parameter(
            name = "resourceVersion",
            in = ParameterIn.PATH,
            description =
                    "The name of the validation resource to be retrieved to validate the input")
    @Parameter(
            name = "resourceVersion",
            in = ParameterIn.PATH,
            description =
                    "The version of the validation resource to be retrieved to validate the input")
    @POST
    @Path("/{resourceName}/{resourceVersion}")
    @Consumes(value = {APPLICATION_JSON, TEXT_JSON, APPLICATION_LD_JSON})
    Uni<ValidationReport> validateByNameAndVersion(
            @RestPath String resourceName,
            @RestPath String resourceVersion,
            byte[] dpp,
            @RestHeader("Content-Type") String contentType)
            throws IOException;
}
