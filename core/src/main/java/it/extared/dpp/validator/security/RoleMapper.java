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
package it.extared.dpp.validator.security;

import static it.extared.dpp.validator.utils.CommonUtils.debug;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import it.extared.dpp.validator.DppValidatorConfig;
import it.extared.dpp.validator.utils.MultiMap;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

/** Map roles retrieved from the security identity to the api internal roles, namely one of */
@ApplicationScoped
public class RoleMapper implements SecurityIdentityAugmentor {

    @Inject DppValidatorConfig config;

    private static final Logger LOG = Logger.getLogger(RoleMapper.class);

    @Override
    public int priority() {
        return SecurityIdentityAugmentor.super.priority();
    }

    @Override
    public Uni<SecurityIdentity> augment(
            SecurityIdentity securityIdentity,
            AuthenticationRequestContext authenticationRequestContext) {
        return Uni.createFrom().item(copyAndAugment(securityIdentity));
    }

    @Override
    public Uni<SecurityIdentity> augment(
            SecurityIdentity identity,
            AuthenticationRequestContext context,
            Map<String, Object> attributes) {
        return SecurityIdentityAugmentor.super.augment(identity, context, attributes);
    }

    protected Supplier<SecurityIdentity> copyAndAugment(SecurityIdentity identity) {
        if (identity.isAnonymous()) {
            debug(LOG, () -> "User is anonymous no augmentation to be performed");
            return () -> identity;
        } else {
            QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);
            addInternalRoles(identity, builder);
            return builder::build;
        }
    }

    private void addInternalRoles(
            SecurityIdentity identity, QuarkusSecurityIdentity.Builder builder) {
        Set<String> roles = identity.getRoles();
        MultiMap<String, String> mappings = config.rolesMappings();
        debug(LOG, () -> "Applying role mapping if needed...");
        if (roles != null && !roles.isEmpty()) {
            for (String r : roles) {
                List<String> internalRoles = mappings.get(r);
                debug(
                        LOG,
                        () ->
                                "Retrieved %s internal roles using incoming role %s"
                                        .formatted(internalRoles, r));
                if (internalRoles != null)
                    builder.addRoles(
                            internalRoles.stream()
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toSet()));
            }
        }
    }
}
