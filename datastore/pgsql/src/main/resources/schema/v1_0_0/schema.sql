CREATE TABLE json_schemas
(
    id                   BIGSERIAL PRIMARY KEY,
    schema_name          VARCHAR(255) NOT NULL,
    description          VARCHAR(500),
    schema_version       VARCHAR(50)  NOT NULL,
    required_paths       TEXT[] NOT NULL,
    required_paths_count INT          NOT NULL,
    has_variants         BOOLEAN   DEFAULT FALSE,
    schema_content       JSONB        NOT NULL,
    created_at           TIMESTAMP DEFAULT NOW(),
    UNIQUE (schema_name, schema_version)
);

CREATE TABLE schema_variants
(
    id                   BIGSERIAL PRIMARY KEY,
    schema_metadata_id   BIGINT      NOT NULL REFERENCES json_schemas (id) ON DELETE CASCADE,
    variant_type         VARCHAR(20) NOT NULL,
    variant_index        INT         NOT NULL,
    required_paths       TEXT[] NOT NULL,
    required_paths_count INT         NOT NULL,
    discriminator_path   VARCHAR(255),
    discriminator_value  VARCHAR(255),
    UNIQUE (schema_metadata_id, variant_type, variant_index)
);

CREATE TABLE schema_pattern_properties
(
    id                 BIGSERIAL PRIMARY KEY,
    schema_metadata_id BIGINT       NOT NULL REFERENCES json_schemas (id) ON DELETE CASCADE,
    pattern_regex      VARCHAR(500) NOT NULL,
    path_prefix        VARCHAR(255) NOT NULL,
    required_sub_paths TEXT[],
    UNIQUE (schema_metadata_id, path_prefix, pattern_regex)
);

CREATE INDEX idx_variants_schema ON schema_variants (schema_metadata_id);
CREATE INDEX idx_variants_discriminator ON schema_variants (discriminator_path, discriminator_value);
CREATE INDEX idx_pattern_schema ON schema_pattern_properties (schema_metadata_id);
CREATE INDEX idx_required_paths_gin ON json_schemas USING GIN(required_paths);
CREATE INDEX idx_variant_paths_gin ON schema_variants USING GIN(required_paths);

CREATE TABLE shacl_templates
(
    id               BIGSERIAL PRIMARY KEY,
    template_name    VARCHAR(255) NOT NULL,
    description      VARCHAR(500),
    template_version VARCHAR(50),
    context_uri      VARCHAR(500),
    uploaded_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    shacl_content    TEXT         NOT NULL,
    UNIQUE (template_name, template_version)
);

CREATE INDEX idx_template_name ON shacl_templates (template_name);

CREATE INDEX idx_context_uri ON shacl_templates (context_uri);


CREATE TABLE shacl_shapes
(
    id             BIGSERIAL PRIMARY KEY,
    template_id    BIGINT       NOT NULL REFERENCES shacl_templates (id) ON DELETE CASCADE,
    shape_id       VARCHAR(500) NOT NULL,
    target_class   VARCHAR(500),
    vocabulary_uri VARCHAR(500),
    ontology_uri   VARCHAR(500),
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_target_class ON shacl_shapes (target_class);

CREATE INDEX idx_vocabulary ON shacl_shapes (vocabulary_uri);

CREATE INDEX idx_template_id ON shacl_shapes (template_id);
