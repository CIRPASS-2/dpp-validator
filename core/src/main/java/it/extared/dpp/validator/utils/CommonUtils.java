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
package it.extared.dpp.validator.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.inject.spi.CDI;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;
import org.jboss.logging.Logger;

public class CommonUtils {

    public static JsonNode readJsonSchemaNode(String resourceName) {
        return readJsonFromClassPath("/json-schemas", resourceName);
    }

    public static byte[] readJsonSchemaBytes(String resourceName) {
        return readBytesFromClassPath("/json-schemas", resourceName);
    }

    public static JsonNode readJsonNode(String resourceName) {
        return readJsonFromClassPath("/json", resourceName);
    }

    public static byte[] readJsonBytes(String resourceName) {
        return readBytesFromClassPath("/json", resourceName);
    }

    public static String readShaclString(String resourceName) {
        return readStringFromClassPath("/shacl-templates", resourceName);
    }

    public static String readJsonLdString(String resName) {
        return readStringFromClassPath("/json-ld", resName);
    }

    public static byte[] readBytesFromClassPath(String resourceDir, String resourceName) {
        try (InputStream is =
                CommonUtils.class.getResourceAsStream(resourceDir + "/" + resourceName)) {
            if (is == null)
                throw new RuntimeException(
                        "No resource with name %s in class path dir %s"
                                .formatted(resourceName, resourceDir));
            return is.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readStringFromClassPath(String resourceDir, String resourceName) {
        return new String(readBytesFromClassPath(resourceDir, resourceName));
    }

    public static JsonNode readJsonFromClassPath(String resourceDir, String resourceName) {
        try (InputStream is =
                CommonUtils.class.getResourceAsStream(resourceDir + "/" + resourceName)) {
            if (is == null)
                throw new RuntimeException(
                        "No resource with name %s in class path dir %s"
                                .formatted(resourceName, resourceDir));
            return CDI.current().select(ObjectMapper.class).get().readTree(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void debug(Logger logger, Supplier<String> message) {
        if (logger.isDebugEnabled()) {
            logger.debug(message.get());
        }
    }
}
