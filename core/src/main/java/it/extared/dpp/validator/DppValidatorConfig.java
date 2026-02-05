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

import io.quarkus.runtime.util.StringUtil;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import it.extared.dpp.validator.security.Roles;
import it.extared.dpp.validator.utils.MultiMap;
import java.util.stream.Stream;
import org.eclipse.microprofile.config.spi.Converter;

@ConfigMapping(prefix = "validator")
public interface DppValidatorConfig {

    /**
     * @return comma separated list of mappings between external IdP roles and internal roles (see
     *     {@link Roles}) as {ext_rolename}:{internal_roleName} ->
     *     my_ext_role:my_internal_role,my_ext_role2:my_internal_role2
     */
    @WithConverter(RolesMappingsConverter.class)
    @WithDefault("admin:admin,eo:eo,eu:eu")
    MultiMap<String, String> rolesMappings();

    class RolesMappingsConverter implements Converter<MultiMap<String, String>> {

        @Override
        public MultiMap<String, String> convert(String s)
                throws IllegalArgumentException, NullPointerException {
            MultiMap<String, String> map = new MultiMap<>();
            if (StringUtil.isNullOrEmpty(s)) return map;
            String[] mappings = s.split(",");
            Stream.of(mappings)
                    .map(m -> m.split(":"))
                    .filter(arr -> arr.length >= 2)
                    // validates on roles enum
                    .peek(arr -> Roles.valueOf(arr[1].toUpperCase()))
                    .forEach(arr -> map.add(arr[0], arr[1].toUpperCase()));
            return map;
        }
    }
}
