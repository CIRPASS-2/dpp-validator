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
package it.extared.dpp.validator.jsonld;

import static it.extared.dpp.validator.utils.CommonUtils.debug;
import static it.extared.dpp.validator.utils.JsonLdUtils.extractNamespace;

import it.extared.dpp.validator.jsonld.dto.ShaclShapeMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.StringReader;
import java.util.*;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.engine.Target;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.vocabulary.OWL;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ShaclMetadataExtractor {

    private static final String SHACL_NS = "http://www.w3.org/ns/shacl#";

    private static final Logger LOGGER = Logger.getLogger(ShaclMetadataExtractor.class);

    public List<ShaclShapeMetadata> extractAllShapes(String shacl) {
        Model model = ModelFactory.createDefaultModel();
        model.read(new StringReader(shacl), null, "TURTLE");
        Graph graph = model.getGraph();

        Shapes shapes = Shapes.parse(graph);

        List<ShaclShapeMetadata> allMetadata = new ArrayList<>();

        for (Shape shape : shapes.getShapeMap().values()) {
            if (shape.getShapeNode().isURI()) {
                ShaclShapeMetadata metadata = extractShapeMetadata(shape, model);
                allMetadata.add(metadata);
            }
        }

        return allMetadata;
    }

    private ShaclShapeMetadata extractShapeMetadata(Shape shape, Model model) {
        ShaclShapeMetadata metadata = new ShaclShapeMetadata();

        metadata.setShapeId(shape.getShapeNode().getURI());

        Collection<Target> targets = shape.getTargets();
        debug(LOGGER, () -> "trying retrieving target class from the shapes...");
        if (!targets.isEmpty()) {
            Target target = targets.iterator().next();
            if (target.getObject().isURI()) {
                String targetClass = target.getObject().getURI();
                debug(LOGGER, () -> "retrieved target class %s".formatted(targetClass));
                metadata.setTargetClass(targetClass);
            }
        }

        metadata.setVocabularyUri(extractVocabularyUri(shape, model));
        metadata.setOntologyUri(extractOntologyUri(model, shape));
        return metadata;
    }

    private String extractVocabularyUri(Shape shape, Model model) {
        Map<String, Integer> namespaceCounts = new HashMap<>();

        Property shProperty = model.getProperty(SHACL_NS + "property");
        Property shPath = model.getProperty(SHACL_NS + "path");

        Resource shapeResource = model.getResource(shape.getShapeNode().getURI());

        StmtIterator propIter = shapeResource.listProperties(shProperty);
        debug(LOGGER, () -> "extracting vocabulary uri by ns count");
        while (propIter.hasNext()) {
            Statement propStmt = propIter.next();
            if (propStmt.getObject().isResource()) {
                Resource propShape = propStmt.getResource();

                Statement pathStmt = propShape.getProperty(shPath);
                if (pathStmt != null && pathStmt.getObject().isURIResource()) {
                    String pathUri = pathStmt.getResource().getURI();
                    String namespace = extractNamespace(pathUri);
                    namespaceCounts.merge(namespace, 1, Integer::sum);
                }
            }
        }

        String vocabulary =
                namespaceCounts.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(null);
        debug(LOGGER, () -> "extracted vocabulary is %s".formatted(vocabulary));
        return vocabulary;
    }

    private String extractOntologyUri(Model model, Shape shape) {
        StmtIterator imports = model.listStatements(null, OWL.imports, (RDFNode) null);
        if (imports.hasNext()) {
            Statement stmt = imports.next();
            if (stmt.getObject().isURIResource()) {
                String ontologyUri = stmt.getResource().getURI();
                debug(LOGGER, () -> "extracted ontology uri is %s".formatted(ontologyUri));
                return ontologyUri;
            }
        }

        String shapeUri = shape.getShapeNode().getURI();
        if (shapeUri != null) {
            int hashIndex = shapeUri.lastIndexOf('#');
            int slashIndex = shapeUri.lastIndexOf('/');
            int splitIndex = Math.max(hashIndex, slashIndex);

            if (splitIndex > 0) {
                String ontologyUri = shapeUri.substring(0, splitIndex);
                debug(LOGGER, () -> "extracted ontology uri is %s".formatted(ontologyUri));
                return ontologyUri;
            }
        }
        debug(LOGGER, () -> "ontology uri not found");
        return null;
    }
}
