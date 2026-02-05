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
package it.extared.dpp.validator;

import io.smallrye.mutiny.Uni;
import it.extared.dpp.validator.dto.ValidationReport;
import java.io.IOException;

/** Validator strategy interface. */
public interface Validator {

    /**
     * Given an input as byte[] it should validate it and produce an {@link ValidationReport}
     * result.
     *
     * @param input what needs to be validated.
     * @return a report with validation details.
     * @throws IOException when something goes wrong reading the input or a validation resource.
     */
    Uni<ValidationReport> validate(byte[] input) throws IOException;

    /**
     * Given the name and the version of a validation resource and an input as byte[], it should
     * validate the latter using the validation resource identified by the name and the version and
     * produce an {@link ValidationReport} result.
     *
     * @param resourceName the validation resource name.
     * @param version the validation resource version.
     * @param input what needs to be validated
     * @return a report with validation details.
     * @throws IOException when something goes wrong reading the input or a validation resource.
     */
    Uni<ValidationReport> validate(String resourceName, String version, byte[] input)
            throws IOException;

    /**
     * @param type the type of validation required.
     * @return true if its supports it, false otherwise.
     */
    boolean canHandle(ValidationType type);

    /**
     * Priority of the Validator, where 0 is highest priority.
     *
     * @return the priority number.
     */
    Integer priority();
}
