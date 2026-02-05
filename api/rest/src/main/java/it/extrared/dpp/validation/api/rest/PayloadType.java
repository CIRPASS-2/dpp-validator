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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.extared.dpp.validator.ValidationType;
import it.extared.dpp.validator.exceptions.InvalidOpException;
import it.extared.dpp.validator.utils.JsonLdUtils;
import jakarta.enterprise.inject.spi.CDI;
import java.io.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

public enum PayloadType {
    json(null, List.of(APPLICATION_JSON, TEXT_JSON)),
    json_ld(Lang.JSONLD, List.of(APPLICATION_LD_JSON)),
    rdf_json(Lang.RDFJSON, List.of("application/rdf+json")),
    rdf_xml(Lang.RDFXML, List.of("application/rdf+xml", "application/xml", "text/xml")),
    turtle(Lang.TURTLE, List.of("application/x+turtle", "text/turtle")),
    n_triples(Lang.NTRIPLES, List.of("application/n-triples", "text/plain")),
    n_quads(Lang.NQUADS, List.of("application/n-quads")),
    n3(Lang.N3, List.of("text/n3", "text/rdf+n3"));

    private final Lang lang;
    private final List<String> supportedMimeTypes;

    PayloadType(Lang lang, List<String> supportedMimeTypes) {
        this.lang = lang;
        this.supportedMimeTypes = supportedMimeTypes;
    }

    public InputStream convert(InputStream input) {
        if (lang == null || Objects.equals(Lang.TURTLE, lang)) {
            return input;
        } else {
            Model model = ModelFactory.createDefaultModel();
            RDFDataMgr.read(model, input, lang);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            RDFDataMgr.write(new BufferedOutputStream(baos), model, Lang.TURTLE);
            return new ByteArrayInputStream(baos.toByteArray());
        }
    }

    public ValidationType asValidationType() {
        if (lang == null) return ValidationType.PLAIN_JSON;
        else return ValidationType.RDF;
    }

    public static PayloadType fromContentType(byte[] content, String contentType) {
        // someone might send a JSON-LD using a JSON mime type.
        // Double check if this is the case
        String actualContentType = actualContentType(content, contentType);
        Optional<PayloadType> payloadType =
                Stream.of(PayloadType.values())
                        .filter(
                                p ->
                                        p.supportedMimeTypes.stream()
                                                .anyMatch(actualContentType::contains))
                        .findFirst();
        return payloadType.orElseThrow(
                () ->
                        new InvalidOpException(
                                "Content type %s is not supported".formatted(contentType)));
    }

    private static String actualContentType(byte[] dpp, String contentType) {
        try {
            if (!contentType.contains(APPLICATION_JSON) && !contentType.contains(TEXT_JSON))
                return contentType;
            ObjectMapper objectMapper = CDI.current().select(ObjectMapper.class).get();
            JsonNode payload = objectMapper.readTree(dpp);
            if (JsonLdUtils.isJsonLd(payload)) return APPLICATION_LD_JSON;
            else return APPLICATION_JSON;
        } catch (IOException e) {
            throw new RuntimeException(
                    "Error while trying to deserialize dpp to check if it is JSON LD or plain JSON");
        }
    }
}
