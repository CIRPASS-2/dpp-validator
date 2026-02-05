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

import io.smallrye.mutiny.Uni;
import it.extared.dpp.validator.ValidatorService;
import it.extared.dpp.validator.dto.ValidationReport;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;

@ApplicationScoped
public class DPPValidationAPIImpl implements DPPValidationAPI {

    @Inject ValidatorService validatorService;

    @Override
    public Uni<ValidationReport> validate(byte[] dpp, String contentType) throws IOException {
        return validatorService.validate(
                dpp, PayloadType.fromContentType(dpp, contentType).asValidationType());
    }

    @Override
    public Uni<ValidationReport> validateByNameAndVersion(
            String resourceName, String resourceVersion, byte[] dpp, String contentType)
            throws IOException {
        return validatorService.validate(
                resourceName,
                resourceVersion,
                dpp,
                PayloadType.fromContentType(dpp, contentType).asValidationType());
    }
}
