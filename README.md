# DPP Validator

A service for validating Digital Product Passport (DPP) payloads in JSON or JSON-LD format using JSON Schema or SHACL template validation. Provides REST APIs for managing JSON schemas and SHACL templates.

© CIRPASS-2 Consortium, 2024-2027

<img width="832" height="128" alt="image" src="https://raw.githubusercontent.com/CIRPASS-2/assets/main/images/cc-commons.png" />

The CIRPASS-2 project receives funding under the European Union's DIGITAL EUROPE PROGRAMME under the GA No 101158775.

**Important disclaimer:**
All software and artifacts produced by the CIRPASS-2 consortium are designed for exploration and are provided for information purposes only. They should not be interpreted as being either complete, exhaustive, or normative. The CIRPASS-2 consortium partners are not liable for any damage that could result from making use of this information. Technical interpretations of the European Digital Product Passport system expressed in these artifacts are those of the author(s) only and do not necessarily reflect those of the European Union, European Commission, or the European Health and Digital Executive Agency (HADEA). Neither the European Union, the European Commission nor the granting authority can be held responsible for them. Technical interpretations of the European Digital Product Passport system expressed in these artifacts are those of the author(s) only and should not be interpreted as reflecting those of CEN-CENELEC JTC 24.

## Overview

HTTP-based DPP validation service with support for:
- Persisting, retrieving, and deleting JSON schemas and SHACL templates
- Validating JSON/JSON-LD payloads against schemas or templates
- PostgreSQL database backend
- OpenID Connect authentication

### Key Features

- **RESTful API** for schema/template management and validation triggers
- **Smart automatic matching**: similarity-based for JSON schemas, type/vocabulary/context URI matching for SHACL templates
- **PostgreSQL database** with optimized indexing
- **OpenID Connect authentication** with role-based access control (admin/eo/eu)
- **Multi-format SHACL support** (Turtle, RDF/XML, JSON-LD, N-Triples, N-Quads, N3)

## Table of Contents

- [Quick Start](#quick-start)
- [Configuration](#configuration)
  - [Configuration Variables Reference](#configuration-variables-reference)
  - [Configuration Examples](#configuration-examples)
- [REST API](#rest-api)
  - [Validation API](#validation-api)
  - [Resource Management API](#resource-management-api)
  - [Response Codes Summary](#response-codes-summary)
- [Authentication & Authorization](#authentication--authorization)
- [License](#license)
- [Contributing](#contributing)
- [Support](#support)

## Quick Start

### Build the Application

```bash
mvn clean install
```

Artifacts and Docker images are available at [GitHub Releases](https://github.com/cirpass-2/dpp-validator/releases)

### Run the Application

**With configuration file:**
```bash
java -Dquarkus.config.locations=file://path/to/application.properties \
     -Dquarkus.profile=pgsql,oidc \
     -jar target/quarkus-app/quarkus-run.jar
```

**With environment variables:**
```bash
QUARKUS_DATASOURCE_REACTIVE_URL=vertx-reactive:postgresql://localhost:5432/registry_db \
QUARKUS_DATASOURCE_USERNAME=db_user \
QUARKUS_DATASOURCE_PASSWORD=db_password \
QUARKUS_OIDC_AUTH_SERVER_URL=https://your-idp.com/realms/your-realm \
QUARKUS_OIDC_CLIENT_ID=your-client-id \
QUARKUS_OIDC_CREDENTIALS_SECRET=your-secret \
java -jar target/quarkus-app/quarkus-run.jar
```

### Using Docker

See the [Docker Compose](#docker-compose) examples in the configuration section.

## Configuration

### Configuration Variables Reference

#### Database Configuration

| Variable                                   | Environment Variable                          | Description                    | Default |
|--------------------------------------------|-----------------------------------------------|--------------------------------|---------|
| `quarkus.datasource.reactive.url`          | `QUARKUS_DATASOURCE_REACTIVE_URL`             | Reactive datasource URL        | -       |
| `quarkus.datasource.username`              | `QUARKUS_DATASOURCE_USERNAME`                 | Database username              | -       |
| `quarkus.datasource.password`              | `QUARKUS_DATASOURCE_PASSWORD`                 | Database password              | -       |
| `quarkus.datasource.reactive.max-size`     | `QUARKUS_DATASOURCE_REACTIVE_MAX_SIZE`        | Maximum connection pool size   | `16`    |

**PostgreSQL Reactive URL format:**
```
vertx-reactive:postgresql://hostname:port/database_name
```
Example: `vertx-reactive:postgresql://localhost:5432/registry_db`

<details>
<summary><b>PostgreSQL Database Schema (click to expand)</b></summary>

```sql
CREATE TABLE json_schemas (
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

CREATE TABLE schema_variants (
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

CREATE TABLE schema_pattern_properties (
    id                 BIGSERIAL PRIMARY KEY,
    schema_metadata_id BIGINT       NOT NULL REFERENCES json_schemas (id) ON DELETE CASCADE,
    pattern_regex      VARCHAR(500) NOT NULL,
    path_prefix        VARCHAR(255) NOT NULL,
    required_sub_paths TEXT[],
    UNIQUE (schema_metadata_id, path_prefix, pattern_regex)
);

CREATE TABLE shacl_templates (
    id               BIGSERIAL PRIMARY KEY,
    template_name    VARCHAR(255) NOT NULL,
    description      VARCHAR(500),
    template_version VARCHAR(50),
    context_uri      VARCHAR(500),
    uploaded_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    shacl_content    TEXT         NOT NULL,
    UNIQUE (template_name, template_version)
);

CREATE TABLE shacl_shapes (
    id             BIGSERIAL PRIMARY KEY,
    template_id    BIGINT       NOT NULL REFERENCES shacl_templates (id) ON DELETE CASCADE,
    shape_id       VARCHAR(500) NOT NULL,
    target_class   VARCHAR(500),
    vocabulary_uri VARCHAR(500),
    ontology_uri   VARCHAR(500),
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance optimization
CREATE INDEX idx_variants_schema ON schema_variants (schema_metadata_id);
CREATE INDEX idx_variants_discriminator ON schema_variants (discriminator_path, discriminator_value);
CREATE INDEX idx_pattern_schema ON schema_pattern_properties (schema_metadata_id);
CREATE INDEX idx_required_paths_gin ON json_schemas USING GIN(required_paths);
CREATE INDEX idx_variant_paths_gin ON schema_variants USING GIN(required_paths);
CREATE INDEX idx_template_name ON shacl_templates (template_name);
CREATE INDEX idx_context_uri ON shacl_templates (context_uri);
CREATE INDEX idx_target_class ON shacl_shapes (target_class);
CREATE INDEX idx_vocabulary ON shacl_shapes (vocabulary_uri);
CREATE INDEX idx_template_id ON shacl_shapes (template_id);
```
</details>

#### OpenID Connect Configuration

| Variable                             | Environment Variable                 | Description                                         | Default |
|--------------------------------------|--------------------------------------|-----------------------------------------------------|---------|
| `quarkus.oidc.auth-server-url`       | `QUARKUS_OIDC_AUTH_SERVER_URL`       | OIDC server URL (realm URL for Keycloak)            | -       |
| `quarkus.oidc.client-id`             | `QUARKUS_OIDC_CLIENT_ID`             | OIDC client ID                                      | -       |
| `quarkus.oidc.credentials.secret`    | `QUARKUS_OIDC_CREDENTIALS_SECRET`    | OIDC client secret                                  | -       |
| `quarkus.oidc.roles.role-claim-path` | `QUARKUS_OIDC_ROLES_ROLE_CLAIM_PATH` | Comma-separated JWT claim paths for role extraction | `group` |

#### Application Configuration

| Variable                  | Environment Variable      | Description                                                  | Default |
|---------------------------|---------------------------|--------------------------------------------------------------|---------|
| `validator.role-mappings` | `VALIDATOR_ROLE_MAPPINGS` | Comma-separated mappings between external and internal roles | -       |
| `quarkus.http.port`       | `QUARKUS_HTTP_PORT`       | HTTP port of the service                                     | `8080`  |

#### Configuration Notes

**Role Mappings**
- Maps external Identity Provider roles to internal application roles
- Internal roles: `admin`, `eo` (Economic Operator), `eu` (End User)
- Format: `external_role:internal_role,another_external:another_internal`
- Example: `keycloak_admin:admin,keycloak_operator:eo`

**OIDC Role Claim Path**
- Supports multiple paths separated by commas
- The system searches for roles in the JWT token at each specified path in order
- Example: `group,realm_access.roles`

### Configuration Examples

#### Application Properties (PostgreSQL)
```properties
# Database Configuration
quarkus.datasource.reactive.url=vertx-reactive:postgresql://localhost:5432/registry_db
quarkus.datasource.username=dbuser
quarkus.datasource.password=dbpass
quarkus.datasource.reactive.max-size=20

# OIDC Configuration
quarkus.oidc.auth-server-url=https://keycloak.example.com/realms/myrealm
quarkus.oidc.client-id=my-client
quarkus.oidc.credentials.secret=my-secret
quarkus.oidc.roles.role-claim-path=group,realm_access.roles

# Application Configuration
validator.role-mappings=keycloak_admin:admin,keycloak_eu:eu,keycloak_operator:eo
```

#### Docker Compose (PostgreSQL)
```yaml
version: '3.8'

services:
  validator:
    image: ghcr.io/cirpass-2/dpp-validator-pgsql-oidc:latest
    ports:
      - "8080:8080"
    environment:
      # Database Configuration
      QUARKUS_DATASOURCE_REACTIVE_URL: vertx-reactive:postgresql://postgres:5432/registry_db
      QUARKUS_DATASOURCE_USERNAME: dbuser
      QUARKUS_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      QUARKUS_DATASOURCE_REACTIVE_MAX_SIZE: 20
      
      # OIDC Configuration
      QUARKUS_OIDC_AUTH_SERVER_URL: https://keycloak:8443/realms/myrealm
      QUARKUS_OIDC_CLIENT_ID: my-client
      QUARKUS_OIDC_CREDENTIALS_SECRET: ${OIDC_SECRET}
      QUARKUS_OIDC_ROLES_ROLE_CLAIM_PATH: group,realm_access.roles
      
      # Application Configuration
      VALIDATOR_ROLE_MAPPINGS: keycloak_admin:admin,keycloak_eu:eu,keycloak_operator:eo
    depends_on:
      - postgres

  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: registry_db
      POSTGRES_USER: dbuser
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres-data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

volumes:
  postgres-data:
```

#### Kubernetes Deployment
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: validator-config
data:
  QUARKUS_DATASOURCE_REACTIVE_URL: "vertx-reactive:postgresql://postgres-service:5432/registry_db"
  QUARKUS_DATASOURCE_USERNAME: "dbuser"
  QUARKUS_DATASOURCE_REACTIVE_MAX_SIZE: "20"
  QUARKUS_OIDC_AUTH_SERVER_URL: "https://keycloak.example.com/realms/myrealm"
  QUARKUS_OIDC_CLIENT_ID: "my-client"
  QUARKUS_OIDC_ROLES_ROLE_CLAIM_PATH: "group,realm_access.roles"
  VALIDATOR_ROLE_MAPPINGS: "keycloak_admin:admin,keycloak_user:eu,keycloak_operator:eo"

---
apiVersion: v1
kind: Secret
metadata:
  name: validator-secrets
type: Opaque
stringData:
  QUARKUS_DATASOURCE_PASSWORD: "dbpass"
  QUARKUS_OIDC_CREDENTIALS_SECRET: "my-secret"

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: validator-deployment
spec:
  replicas: 2
  selector:
    matchLabels:
      app: validator
  template:
    metadata:
      labels:
        app: validator
    spec:
      containers:
      - name: validator
        image: ghcr.io/cirpass-2/dpp-validator-pgsql-oidc:latest
        ports:
        - containerPort: 8080
        envFrom:
        - configMapRef:
            name: validator-config
        - secretRef:
            name: validator-secrets
```

## REST API

### API Overview

The application exposes two main API groups:

1. **Validation API**: For validating JSON/JSON-LD payloads
2. **Resource Management API**: For creating and managing validation resources (JSON schemas and SHACL templates)

**OpenAPI Specification:** Send a `GET` request to `/q/openapi` with the `Accept` header set to `application/json` or `application/yaml`

| Group           | Path                                           | Description                                                       |
|-----------------|------------------------------------------------|-------------------------------------------------------------------|
| **Validation**  | `/validate/v1`                                 | Auto-matching validation                                          |
| **Validation**  | `/validate/v1/{name}/{version}`                | Validation with specific resource                                 |
| **Resources**   | `/resource/v1/{payloadType}`                   | Upload schema/template                                            |
| **Resources**   | `/resource/v1/{resourceType}`                  | Search resources                                                  |
| **Resources**   | `/resource/v1/{resourceType}/{id}`             | Get/Delete resource by ID                                         |
| **Resources**   | `/resource/v1/{resourceType}/{name}/{version}` | Get resource by name/version                                      |

---

### Validation API

All validation endpoints return a JSON validation report with the following structure:

```json
{
  "valid": boolean,
  "message": string,
  "validatedWith": string,
  "invalidProperties": [
    {
      "property": string,
      "reason": string
    }
  ]
}
```

#### `POST /validate/v1`

Validates the request body payload by automatically retrieving the best matching schema or template from the database.

**Matching Algorithm:**

**For JSON payloads:**
1. Extracts all JSON property names from the payload
2. Computes a weighted Jaccard index using payload properties and mandatory properties extracted from schemas at upload time
3. Uses the schema with the highest similarity score for validation

**For JSON-LD payloads:**
1. Extracts the `@context` URI (if present)
2. Expands the JSON-LD and extracts the root `@type` and vocabulary URI (from `@vocab` or by counting most frequent namespace)
3. Attempts to retrieve a SHACL template with the following priority:
  - Match by `@type` (against shape `owl:targetClass`)
  - Match by `@context` URI
  - Match by vocabulary URI

**Request Headers:**

| Header          | Required | Values                                                 |
|-----------------|----------|--------------------------------------------------------|
| `Content-Type`  | ✓        | `application/json`, `text/json`, `application/ld+json` |
| `Authorization` | ✓        | `Bearer <token>`                                       |

**Request Body Example (JSON):**
```json
{
  "productIdentification": {
    "batteryUID": "BAT-2024-001",
    "manufacturer": {
      "name": "ACME Batteries",
      "streetName": "Via Roma 1"
    }
  },
  "generalInformation": {
    "batteryCategory": "EV",
    "batteryWeight": 450.5
  }
}
```

**Response Example (JSON Schema validation):**
```json
{
  "valid": false,
  "message": "Validation performed using schema found by SIMILARITY_MATCH",
  "validatedWith": "battery-dpp-schema - 1.0.0",
  "invalidProperties": [
    {
      "property": "$.productIdentification.manufacturer.postalCode",
      "reason": "required property 'postalCode' not found"
    },
    {
      "property": "$.productIdentification.manufacturer.cityName",
      "reason": "required property 'cityName' not found"
    },
    {
      "property": "$.generalInformation.manufacturingDate",
      "reason": "required property 'manufacturingDate' not found"
    }
  ]
}
```

**Request Body Example (JSON-LD):**
```json
{
  "@context": "http://example.org/vehicle-dpp/context.jsonld",
  "@type": "VehiclePassport",
  "@id": "http://example.org/data/vehicle-001",
  "vehicleIdentification": {
    "vin": "WBA12345678901234"
  },
  "ecology": {
    "fuelConsumption": 5.8
  }
}
```

**Response Example (SHACL validation):**
```json
{
  "valid": false,
  "message": "Validation performed using template found by TYPE_MATCH",
  "validatedWith": "vehicle-dpp-template - 1.0.0",
  "invalidProperties": [
    {
      "property": "[http://example.org/data/vehicle-001]http://example.org/vehicle-dpp#fuelConsumption",
      "reason": "DatatypeConstraint[xsd:double]: Expected xsd:double, got xsd:decimal"
    }
  ]
}
```

---

#### `POST /validate/v1/{resourceName}/{resourceVersion}`

Validates the request body payload using the schema or template matching the specified name and version.

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `resourceName` | string | ✓ | Name of the resource to use for validation |
| `resourceVersion` | string | ✓ | Version of the resource to use for validation |

**Request Headers:**

| Header          | Required | Values                                                 |
|-----------------|----------|--------------------------------------------------------|
| `Content-Type`  | ✓        | `application/json`, `text/json`, `application/ld+json` |
| `Authorization` | ✓        | `Bearer <token>`                                       |

**Example Request:**
```http
POST /validate/v1/battery-dpp-schema/1.0.0
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...

{
  "productIdentification": {
    "batteryUID": "BAT-2024-001"
  }
}
```

**Response Example:**
```json
{
  "valid": false,
  "message": "Validation performed using schema found by NAME_AND_VERSION",
  "validatedWith": "battery-dpp-schema - 1.0.0",
  "invalidProperties": [
    {
      "property": "$.productIdentification.manufacturer",
      "reason": "required property 'manufacturer' not found"
    },
    {
      "property": "$.generalInformation",
      "reason": "required property 'generalInformation' not found"
    }
  ]
}
```

---

### Resource Management API

#### `POST /resource/v1/{payloadType}`

Uploads a new validation resource (JSON schema or SHACL template) to the service repository.

**Path Parameters:**

| Parameter     | Type   | Required  | Values                                                                           |
|---------------|--------|-----------|----------------------------------------------------------------------------------|
| `payloadType` | string | ✓         | `json`, `json_ld`, `turtle`, `rdf_xml`, `rdf_json`, `n3`, `n_triples`, `n_quads` |

**Payload Type Logic:**
- `json`: If the file contains `@context` → treated as SHACL (JSON-LD), otherwise → treated as JSON Schema
- All other formats → treated as SHACL templates

**Request Type:** `multipart/form-data` with two parts:

**Part 1 - `meta` (application/json):**

For JSON Schema:
```json
{
  "metadataType": "base",
  "name": "battery-dpp-schema",
  "description": "JSON schema for validating battery digital passports",
  "version": "1.0.0"
}
```

For SHACL Template:
```json
{
  "metadataType": "template",
  "name": "vehicle-dpp-template",
  "description": "SHACL template for validating vehicle digital passports",
  "version": "1.0.0",
  "contextUri": "http://example.org/vehicle-dpp/context.jsonld"
}
```

**Part 2 - `file` (application/octet-stream):**
- The actual schema or template file content

**Processing Behavior:**

**For JSON Schemas:**
- Extracts all required property paths
- Identifies variants (oneOf/anyOf/allOf)
- Extracts pattern properties
- Stores metadata, schema content, and extracted information in the database

**For SHACL Templates:**
- Extracts all shapes from the template
- Identifies vocabulary URIs and target classes (`owl:targetClass`)
- Stores metadata, template content, and extracted shape information in the database

**Response:**
Returns the unique numeric identifier of the created resource.

**Example Response:**
```json
1
```

**cURL Example (JSON Schema):**
```bash
curl -X POST "http://localhost:8080/resource/v1/json" \
  -H "Authorization: Bearer $TOKEN" \
  -F "meta=@metadata.json;type=application/json" \
  -F "file=@battery-schema.json;type=application/octet-stream"
```

**cURL Example (SHACL Template - Turtle):**
```bash
curl -X POST "http://localhost:8080/resource/v1/turtle" \
  -H "Authorization: Bearer $TOKEN" \
  -F "meta=@template-metadata.json;type=application/json" \
  -F "file=@vehicle-dpp.ttl;type=application/octet-stream"
```

---

#### `GET /resource/v1/{resourceType}`

Searches for templates or schemas based on search criteria. Returns resource metadata and unique identifiers with pagination support.

**Path Parameters:**

| Parameter      | Type   | Required | Values               |
|----------------|--------|----------|----------------------|
| `resourceType` | string | ✓        | `schema`, `template` |

**Query Parameters:**

| Parameter     | Type     | Required   | Description                         |
|---------------|----------|------------|-------------------------------------|
| `name`        | string   | ✗          | LIKE search on resource name        |
| `description` | string   | ✗          | LIKE search on resource description |
| `version`     | string   | ✗          | LIKE search on resource version     |
| `offset`      | integer  | ✗          | 0-based start index for pagination  |
| `limit`       | integer  | ✗          | Number of records to return         |

**Example Request (Schemas):**
```http
GET /resource/v1/schema?name=battery&version=1.0&limit=10&offset=0
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
```

**Response Example (Schemas):**
```json
{
  "totalElements": 25,
  "pageSize": 10,
  "elements": [
    {
      "metadataType": "base",
      "id": 1,
      "name": "battery-dpp-schema",
      "description": "JSON schema for battery digital passport validation",
      "version": "1.0.0"
    },
    {
      "metadataType": "base",
      "id": 2,
      "name": "ev-battery-dpp-schema",
      "description": "JSON schema for EV battery digital passport validation",
      "version": "1.0.2"
    },
    {
      "metadataType": "base",
      "id": 3,
      "name": "industrial-battery-dpp-schema",
      "description": "JSON schema for industrial battery digital passport",
      "version": "1.0.0"
    }
  ]
}
```

**Example Request (Templates):**
```http
GET /resource/v1/template?name=vehicle&limit=5&offset=0
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
```

**Response Example (Templates):**
```json
{
  "totalElements": 12,
  "pageSize": 5,
  "elements": [
    {
      "metadataType": "template",
      "id": 10,
      "name": "vehicle-dpp-template",
      "description": "SHACL template for vehicle digital passport validation",
      "version": "1.0.0",
      "contextUri": "http://example.org/vehicle-dpp/context.jsonld"
    },
    {
      "metadataType": "template",
      "id": 11,
      "name": "ev-vehicle-dpp-template",
      "description": "SHACL template for electric vehicle digital passport",
      "version": "2.1.0",
      "contextUri": null
    }
  ]
}
```

---

#### `GET /resource/v1/{resourceType}/{id}`

Retrieves the complete schema or template associated with the specified unique identifier.

**Path Parameters:**

| Parameter      | Type    | Required   | Description                               |
|----------------|---------|------------|-------------------------------------------|
| `resourceType` | string  | ✓          | `schema`, `template`                      |
| `id`           | integer | ✓          | Unique numeric identifier of the resource |

**Example Request (Schema):**
```http
GET /resource/v1/schema/1
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
```

**Response Example (JSON Schema):**
```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "required": ["productIdentification", "generalInformation"],
  "properties": {
    "productIdentification": {
      "type": "object",
      "required": ["batteryUID", "manufacturer"],
      "properties": {
        "batteryUID": {
          "type": "string",
          "description": "Unique identifier for the battery"
        },
        "manufacturer": {
          "type": "object",
          "required": ["name", "streetName", "postalCode", "cityName"],
          "properties": {
            "name": { "type": "string" },
            "streetName": { "type": "string" },
            "postalCode": { "type": "string" },
            "cityName": { "type": "string" },
            "countryCode": { "type": "string" }
          }
        }
      }
    },
    "generalInformation": {
      "type": "object",
      "required": ["batteryCategory", "batteryWeight", "manufacturingDate"],
      "properties": {
        "batteryCategory": {
          "type": "string",
          "enum": ["LMT", "EV", "Industrial", "SLI"],
          "description": "LMT=Light Means of Transport, EV=Electric Vehicle, SLI=Starting Lighting Ignition"
        },
        "batteryWeight": {
          "type": "number",
          "description": "Total weight in kilograms"
        },
        "manufacturingDate": {
          "type": "string",
          "format": "date"
        },
        "batteryChemistry": {
          "type": "string",
          "description": "Battery chemistry type (e.g., NMC, LFP, NCA)"
        }
      }
    }
  }
}
```

**Example Request (Template):**
```http
GET /resource/v1/template/10
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
```

**Response Example (SHACL Template - Turtle format):**
```turtle
@prefix sh: <http://www.w3.org/ns/shacl#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix bp: <http://example.org/battery-passport#> .
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .

<http://example.org/battery-passport> a owl:Ontology ;
    owl:imports <http://example.org/battery-passport> .

bp:BatteryPassportShape
    a sh:NodeShape ;
    sh:targetClass bp:BatteryPassport ;
    sh:property [
        sh:path bp:productIdentification ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:node bp:ProductIdentificationShape ;
    ] ;
    sh:property [
        sh:path bp:composition ;
        sh:maxCount 1 ;
        sh:node bp:CompositionShape ;
    ] .

bp:ProductIdentificationShape
    a sh:NodeShape ;
    sh:property [
        sh:path bp:manufacturer ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:node bp:ManufacturerShape ;
    ] .

bp:ManufacturerShape
    a sh:NodeShape ;
    sh:property [
        sh:path bp:countryCode ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
    ] .

bp:GeneralInformationShape
    a sh:NodeShape ;
    sh:targetClass bp:GeneralInformation ;
    sh:property [
        sh:path bp:batteryChemistry ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:description "Battery chemistry type (e.g., NMC, LFP, NCA)" ;
    ] .
```

---

#### `GET /resource/v1/{resourceType}/{name}/{version}`

Retrieves the schema or template by its name and version.

**Path Parameters:**

| Parameter      | Type   | Required  | Description             |
|----------------|--------|-----------|-------------------------|
| `resourceType` | string | ✓         | `schema`, `template`    |
| `name`         | string | ✓         | Name of the resource    |
| `version`      | string | ✓         | Version of the resource |

**Example Request:**
```http
GET /resource/v1/schema/battery-dpp-schema/1.0.0
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
```

**Response:** Same format as `GET /resource/v1/{resourceType}/{id}`

---

#### `DELETE /resource/v1/{resourceType}/{id}`

Deletes the schema or template identified by the unique numeric identifier.

**Path Parameters:**

| Parameter      | Type    | Required  | Description                               |
|----------------|---------|-----------|-------------------------------------------|
| `resourceType` | string  | ✓         | `schema`, `template`                      |
| `id`           | integer | ✓         | Unique numeric identifier of the resource |

**Example Request:**
```http
DELETE /resource/v1/schema/1
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
```

**Response:**
- **204 No Content** - Resource successfully deleted
- **404 Not Found** - Resource does not exist
- **403 Forbidden** - Insufficient permissions

**cURL Example:**
```bash
curl -X DELETE "http://localhost:8080/resource/v1/template/10" \
  -H "Authorization: Bearer $TOKEN"
```

---

### Response Codes Summary

| Code   | Description                                            |
|--------|--------------------------------------------------------|
| `200`  | Success with response body                             |
| `204`  | Success without response body (delete operations)      |
| `400`  | Bad request (malformed payload, validation error)      |
| `401`  | Unauthorized (missing or invalid authentication token) |
| `403`  | Forbidden (authenticated but insufficient permissions) |
| `404`  | Resource not found                                     |
| `409`  | Conflict (duplicate name/version combination)          |
| `500`  | Internal server error                                  |

---

## Authentication & Authorization

The application uses **OpenID Connect (OIDC)** for authentication and implements role-based access control.

### Role-Based Access Control

| Role    | Description                | Permissions               |
|---------|----------------------------|---------------------------|
| `admin` | Full administrative access | All API endpoints         |
| `eo`    | Economic Operator          | Validation endpoints only |
| `eu`    | End User                   | All API endpoints         |

### Role Mapping

External Identity Provider roles can be mapped to internal application roles using the `validator.role-mappings` configuration:

```properties
validator.role-mappings=keycloak_admin:admin,idp_operator:eo,idp_user:eu
```

**Format:** `external_role:internal_role,another_external:another_internal`

### JWT Role Claims

The application extracts roles from JWT tokens using the paths specified in `quarkus.oidc.roles.role-claim-path`. Multiple paths can be specified, and the system searches each path in order until roles are found.

**Example configuration:**
```properties
quarkus.oidc.roles.role-claim-path=group,realm_access.roles,resource_access.my-client.roles
```

### Keycloak Setup Example

<details>
<summary><b>Keycloak Configuration Steps (click to expand)</b></summary>

1. **Create Realm:** `dpp-validator`

2. **Create Client:**
  - Client ID: `dpp-validator-api`
  - Client Protocol: `openid-connect`
  - Access Type: `confidential`
  - Valid Redirect URIs: `http://localhost:8080/*`
  - Get the client secret from the Credentials tab

3. **Create Realm Roles:**
  - `validator_admin`
  - `validator_operator`
  - `validator_user`

4. **Create Users and Assign Roles:**
  - Create users in the Users section
  - Assign appropriate roles from Role Mappings tab

5. **Configure Application:**
```properties
quarkus.oidc.auth-server-url=http://keycloak:8080/realms/dpp-validator
quarkus.oidc.client-id=dpp-validator-api
quarkus.oidc.credentials.secret=<client-secret-from-keycloak>
quarkus.oidc.roles.role-claim-path=realm_access.roles
validator.role-mappings=validator_admin:admin,validator_operator:eo,validator_user:eu
```

</details>

---

## License

This project is licensed under the Apache License 2.0.

```
Copyright 2024-2027 CIRPASS-2

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Contributing

We welcome contributions to this project! To contribute:

1. **Open a Pull Request** on GitHub with your changes
2. **Include tests** for all modifications:
  - Bug fixes must include tests that verify the fix
  - New functionalities must include comprehensive test coverage
  - Improvements should include tests where applicable
3. **Request a review** from the maintainers
4. Ensure all existing tests pass and code follows the project's coding standards

All contributions will be reviewed before being merged.

## Support

For questions, issues, or support requests, please contact:

**marco.volpini@extrared.it**