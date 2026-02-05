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
package it.extared.dpp.validator.dto;

import io.quarkus.runtime.util.StringUtil;
import it.extared.dpp.validator.ValidationType;
import java.util.List;

/** Represents the result of a validation process. */
public class ValidationReport {

    private boolean valid;

    private String message;

    private String validatedWith;

    private ValidationType validationType;

    private List<InvalidProperty> invalidProperties;

    public boolean isValid() {
        return valid;
    }

    public String getMessage() {
        return message;
    }

    public List<InvalidProperty> getInvalidProperties() {
        return invalidProperties;
    }

    public String getValidatedWith() {
        return validatedWith;
    }

    public ValidationType getValidationType() {
        return validationType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ValidationReport report;
        private String resourceName;
        private String resourceVersion;

        private Builder() {
            this.report = new ValidationReport();
        }

        public ValidationReport build() {
            String fullName = null;
            if (!StringUtil.isNullOrEmpty(resourceName)) {
                fullName = resourceName;
                if (!StringUtil.isNullOrEmpty(resourceVersion)) fullName += " - " + resourceVersion;
                report.validatedWith = fullName;
            }
            return report;
        }

        public Builder withValid(boolean valid) {
            report.valid = valid;
            return this;
        }

        public Builder withMessage(String message) {
            report.message = message;
            return this;
        }

        public Builder withInvalidProperties(List<InvalidProperty> invalidProperties) {
            report.invalidProperties = invalidProperties;
            return this;
        }

        public Builder withResourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public Builder withResourceVersion(String resourceVersion) {
            this.resourceVersion = resourceVersion;
            return this;
        }

        public Builder withValidationType(ValidationType validationType) {
            report.validationType = validationType;
            return this;
        }
    }
}
