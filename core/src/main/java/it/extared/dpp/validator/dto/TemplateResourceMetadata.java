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

public class TemplateResourceMetadata extends ResourceMetadata {

    private String contextUri;

    public TemplateResourceMetadata() {
        super(TEMPLATE);
    }

    public TemplateResourceMetadata(
            String name, String description, String version, String contextUri) {
        super(name, description, version, TEMPLATE);
        this.contextUri = contextUri;
    }

    public TemplateResourceMetadata(
            Long id, String name, String description, String version, String contextUri) {
        super(id, name, description, version, TEMPLATE);
        this.contextUri = contextUri;
    }

    public String getContextUri() {
        return contextUri;
    }

    public void setContextUri(String contextUri) {
        this.contextUri = contextUri;
    }
}
