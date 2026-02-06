# DPP validator

A service which is meant to provide ways to validate a DPP payload. It supports validation for DPP provided either as JSON or as JSON-LD though respectively JSON schema or SHACL template validation.
It offers as well a REST api to manage JSON schemas and SHACL templates.

© CIRPASS-2 Consortium, 2024-2027

<img width="832" height="128" alt="image" src="https://github.com/user-attachments/assets/7ad1fa77-65f3-4379-909d-614f64587d40" />


The CIRPASS-2 project receives funding under the European Union's DIGITAL EUROPE PROGRAMME under the GA No 101158775.

**Important disclaimer:**
All software and artifacts produced by the CIRPASS-2 consortium are designed for exploration and are provided for information purposes only. They should not be interpreted as being either complete, exhaustive, or normative. The CIRPASS-2 consortium partners are not liable for any damage that could result from making use of this information.
Technical interpretations of the European Digital Product Passport system expressed in these artifacts are those of the author(s) only and do not necessarily reflect those of the European Union, European Commission, or the European Health and Digital Executive Agency (HADEA). Neither the European Union, the European Commission nor the granting authority can be held responsible for them. Technical interpretations of the European Digital Product Passport system expressed in these artifacts are those of the author(s) only and should not be interpreted as reflecting those of CEN-CENELEC JTC 24.

## Overview

This application provides an HTTP-based DPP validation service allowing to persist/retrieve/delete JSON schemas and SHACL templates and validate a JSON/JSON-LD payload against a schema or a template. Currently the only supported database is PostgreSQL and authentication is supported with OpenID Connect.

### Key Features

- **RESTful API** for JSON schema/SHACL template management and to trigger validation.
- **Smart JSON schema/SHACL template matching** with similarity based match for schema and type/vocabulary/context uri match for SHACL templates.
- **Database backend** (PostgreSQL)
- **OpenID Connect authentication** with role-based access control

## Table of Contents

- [Quick Start](#quick-start)
- [Configuration](#configuration)
 - [Configuration Variables Reference](#configuration-variables-reference)
 - [Configuration Examples](#configuration-examples)
- [Metadata Schema](#metadata-schema)
 - [Default Schema](#default-schema)
 - [Schema Customization](#schema-customization)
 - [Schema Resolution Order](#schema-resolution-order)
- [REST API](#rest-api)
 - [Metadata Endpoints](#metadata-endpoints)
 - [Schema Management Endpoints](#schema-management-endpoints)
- [Authentication & Authorization](#authentication--authorization)

## Quick Start

The application currently provides one maven profile which is also the default one:
- `pgsql-oidc` profile builds an application using postgresql as a database and oidc as the authentication method.

Artifacts and docker images are available [here](https://github.com/cirpass-2/dpp-validator/releases)
### Build the Application
```bash
mvn clean install
```

### Run the Application

After building, you can run the application using the Quarkus runner.

Create an `application.properties` with your configuration parameters and specify its location.

***Run with PGSQL***
```bash
java -Dquarkus.config.locations=file://path/to/application.properties -Dquarkus.profile=pgql,oidc -jar target/quarkus-app/quarkus-run.jar
```

Instead of an `application.properties`, environment variables can be used:
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

**PostgreSQL Schema Script:**
```sql
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
```

#### OpenID Connect Configuration

| Variable                             | Environment Variable                 | Description                                         | Default |
|--------------------------------------|--------------------------------------|-----------------------------------------------------|---------|
| `quarkus.oidc.auth-server-url`       | `QUARKUS_OIDC_AUTH_SERVER_URL`       | OIDC server URL (realm URL for Keycloak)            | -       |
| `quarkus.oidc.client-id`             | `QUARKUS_OIDC_CLIENT_ID`             | OIDC client ID                                      | -       |
| `quarkus.oidc.credentials.secret`    | `QUARKUS_OIDC_CREDENTIALS_SECRET`    | OIDC client secret                                  | -       |
| `quarkus.oidc.roles.role-claim-path` | `QUARKUS_OIDC_ROLES_ROLE_CLAIM_PATH` | Comma-separated JWT claim paths for role extraction | `group` |

#### Application Configuration

| Variable                  | Environment Variable      | Description                                                           | Default |
|---------------------------|---------------------------|-----------------------------------------------------------------------|---------|
| `validator.role-mappings` | `VALIDATOR_ROLE_MAPPINGS` | Comma-separated mappings between external and internal roles          | -       |

#### HTTP Configuration


| Variable            | Environment Variable | Description              | Default |
|---------------------|----------------------|--------------------------|---------|
| `quarkus.http.port` | `QUARKUS_HTTP_PORT`  | HTTP port of the service | 8080    |


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
  registry:
    image: ghcr.io/cirpass-2/mock-eu-registry-pgsql-oidc:latest
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
  name: registry-config
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
  name: registry-secrets
type: Opaque
stringData:
  QUARKUS_DATASOURCE_PASSWORD: "dbpass"
  QUARKUS_OIDC_CREDENTIALS_SECRET: "my-secret"

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: registry-deployment
spec:
  replicas: 2
  selector:
    matchLabels:
      app: registry
  template:
    metadata:
      labels:
        app: registry
    spec:
      containers:
      - name: registry
        image: ghcr.io/cirpass-2/mock-eu-registry-pgsql-oidc:latest
        ports:
        - containerPort: 8080
        envFrom:
        - configMapRef:
            name: registry-config
        - secretRef:
            name: registry-secrets
        volumeMounts:
        - name: schema-volume
          mountPath: /etc/registry
          readOnly: true
      volumes:
      - name: schema-volume
        configMap:
          name: json-schema-config
```

## REST API

The application exposes two main API groups:

1. **Resource API**: For creating and managing validation resources i.e. JSON schemas and SHACL templates.
2. **Validation API**: For validating a JSON/JSON-LD payload.

To obtain the OpenAPI document start the application and issue a `GET` request targeting the path `/q/openapi`. Use the `Accept`
header to negotiate the media type (either `JSON` or `YAML`).

### Validation API Endpoints

The API comprises two endpoint for validation. Both validate the payload in the request body and returns a validation report as a JSON,
containing the following properties:
- `valid (boolean)` if the payload is valid or not.
- `message (string)` a message telling which type of match was performed to retrieve the schema/template from the payload.
- `validatedWith (string)` the name of the resource `-` the version of the resource.
- `invalidProperties` if not valid the list of properties invalid or missing in the payload with the related error message.


#### Group /validate/v1

Validate the request body payload automatically retrieving the best matching schema or template among the one present in the database.
When receiving a JSON:
- it extract all the JSON property names.
- it computes a JAGGARD index weightned for non mandatory properties using the JSON properties in the input and the mandatory properties extracted from the schema at addition time.
- it uses for validation the schema with the highest score.

When receiving a JSON-LD:
- it extract the `@context` uri if present
- it expand the JSON-LD and extract the root `@type` and the vocabulary uri (either from the `@vocab` or by counting the most frequent namespace)
- it tries to retrieve a SHACL template by type match (`@type` on shape `owl:targetClass`).
- if no SHACL is found it tries to search by `@context` uri.
- if no SHACL is found it tries to search by vocabulary uri.

Once the schema or template is retrieved the validation is performed and report is returned as a JSON.

**Example Request:**
```http
POST /validate/v1/
Content-Type: application/json | text/json | application/ld+json
```
**Example Response:**

```json
{
 "valid": false,
 "message": "Validation performed using template found by VOCABULARY_MATCH",
 "validatedWith": "vehicle-dpp-template - 1.0.0",
 "invalidProperties": [
  {
   "property": "[http://example.org/data/vehicle-001-ecology]http://example.org/vehicle-dpp#fuelConsumption",
   "reason": "DatatypeConstraint[xsd:double]: Expected xsd:double: Actual xsd:decimal"
  }
 ]
}
```

#### Group /validate/v1/{resourceName}/{resourceVersion}


**Path Parameters:**

| Parameter         | Type   | Description                                                                                               |
|-------------------|--------|-----------------------------------------------------------------------------------------------------------|
| `resourceName`    | string | The name of the resource to retrieve.                                                                     |
| `resourceVersion` | string | The version of the resource to retrieve.                                                                  |

##### POST

Validates the request body payload with the schema or the template matching the path parameters `resourceName` and `resourceVersion`.

**Example Request:**
```http
POST /validate/v1/battery-dpp/1.0.0
Content-Type: application/json | text/json | application/ld+json
```
**Example Response:**

```json
{
 "valid": false,
 "message": "Validation performed using template found by NAME_AND_VERSION",
 "validatedWith": "vehicle-dpp-schema - 1.0.0",
 "invalidProperties": [
  {
   "property": "$.vehicleIdentification.assembler.location",
   "reason": "$.vehicleIdentification.assembler: proprietà richiesta 'location' non trovata"
  }
 ]
}
```

### Resource API Endpoints
#### Group /resource/v1/{payloadType}

**Path Parameters:**

| Parameter     | Type   | Description                                                                      |
|---------------|--------|----------------------------------------------------------------------------------|
| `payloadType` | string | One of `json`,`json_ld`,`turtle`,`rdf_xml`,`rdf_json`,`n3`,`n_triples`,`n_quads` |

##### POST
Add a validation resource to the service repository. The resource must be provided through a multipart body with two parts:
- meta as a JSON: resource metadata such as:
  * a name, a version and an optional description to be associated to the schema/template.
  * a context uri only for SHACL template resources. The context uri refers to the uri at which a JSON-LD `@context` is available that mark a content supported by the SHACL template being uploaded.
- file as binary: the actual schema/template.

The `payloadType` path variable is needed to tell the application the type of the file part being uploaded and can be of one of the following types:
- `json` (when uploading JSON schemas or JSON-LD formatted SHACL)
- `json_ld` (when uploading JSON-LD formatted SHACL)
- `turtle` (when uploading turtle formatted SHACL)
- `rdf_xml` (when uploading RDF XML formatted SHACL)
- `rdf_json` (when uploading RDF JSON formatted SHACL)
- `n_triples` (when uploading N-TRIPLES formatted SHACL)
- `n_quads` (when uploading N-QUADS formatted SHACL)
- `n3` (when uploading n3 formatted SHACL)

**Behavior:**
- The service use the payload type to determine the resource type. If the `payloadType` equals `json` the resource is double checked to see if it contains a  `@context`. If yes it is taken to be a JSON-LD SHACL otherwise is taken to be a JSON schema.
- If the resource is a JSON schema, properties paths listed as mandatory are extracted with the variant informations (anyOf,allOf,oneOf) and possible pattern properties. These informations with the schema itself and associated resource metadata are persisted into the database.
- If the resource is a SHACL template the service tries to extract all the shapes and relative vocabulary uri and type name (owl:targetClass attribute). These informations with the template itself and associated resource metadata are persisted into the database.
- The service return the unique numeric identifier of the resource.

**Example Request:**

*JSON schema example*
```http
POST /resource/v1/json
Content-Type: multipart/form-data
Part: meta
Content-Type: application/json
{
  "metadataType":"base",
  "name":"textile schema",
  "description":"a JSON schema to validate textile JSON DPP",
  "version":"1.0.0",
}

Part: file
Content-Type: application/octet-stream
[bytes]
```

*SHACL template example*
```http
POST /resource/v1/turtle
Content-Type: multipart/form-data
Part: meta
Content-Type: application/json
{
  "metadataType":"template",
  "name":"textile schema",
  "description":"a JSON schema to validate textile JSON DPP",
  "version":"1.0.0",
  "contextUri:"http://url/to/content.jsonld"
}

Part: file
Content-Type: application/octet-stream
[bytes]
```

**Example Response:**

```json
1
```

#### Group /resource/v1/{resourceType}

**Path Parameters:**

| Parameter      | Type   | Description                                                                                               |
|----------------|--------|-----------------------------------------------------------------------------------------------------------|
| `resourceType` | string | Either `template` for operations over SHACL templates, either `schema`  for operations over JSON schemas. |

##### GET

Search for templates or schemas based on search criteria. It returns just the resource metadata associated to the templates and their unique numeric identifier.

**QueryString Parameters:**

| Parameter     | Type    | Description                                                                  |
|---------------|---------|------------------------------------------------------------------------------|
| `name`        | string  | a string to perform a like condition over resource names.                    |
| `description` | string  | a string to perform a like condition over resource description.              |
| `version`     | string  | a string to perform a like condition over resource version.                  |
| `offset`      | integer | a 0 based index to provide the start record to be retrieved from the search. |
| `limit`       | integer | number of record to be returned.                                             |


**Example Request:**

***Example schema request***
```http
GET /resource/v1/schema?name=bat&version=1.0&limit=3&offset=0
```
**Example Response:**

```json
{
 "totalElements": 10,
 "pageSize": 3,
 "elements": [
  {
   "metadataType": "base",
   "id": 1,
   "name": "battery_dpp_schema_1",
   "description": "a schema for digital battery passport",
   "version": "1.0.0"
  },
  {
   "metadataType": "base",
   "id": 2,
   "name": "ev_battery_dpp_schema_2",
   "description": "a schema for ev digital battery passport",
   "version": "1.0.0"
  },
  {
   "metadataType": "base",
   "id": 3,
   "name": "prismatic_cell_battery_dpp_schema_3",
   "description": "a schema for digital battery passport with prismatic cells",
   "version": "1.0.0"
  }
 ]
}
```

***Example template request***
```http
GET /resource/v1/template?name=bat&version=1.0&limit=3&offset=0
```
**Example Response:**

```json
{
 "totalElements": 10,
 "pageSize": 3,
 "elements": [
  {
   "metadataType": "template",
   "id": 1,
   "name": "battery_dpp_shacl_1",
   "description": "a shacl for digital battery passport",
   "version": "1.0.0",
   "contextUri": null
  },
  {
   "metadataType": "template",
   "id": 2,
   "name": "ev_battery_dpp_shacl_2",
   "description": "a shacl for ev digital battery passport",
   "version": "1.0.0",
   "contextUri": null
  },
  {
   "metadataType": "template",
   "id": 3,
   "name": "prismatic_cell_battery_dpp_shacl_3",
   "description": "a shacl for digital battery passport with prismatic cells",
   "version": "1.0.0",
   "contextUri": "https://battery_passports/prova.json"
  }
 ]
}
```

#### Group /resource/v1/{resourceType}/{id}

**Path Parameters:**

| Parameter      | Type    | Description                                                                                               |
|----------------|---------|-----------------------------------------------------------------------------------------------------------|
| `resourceType` | string  | Either `template` for operations over SHACL templates, either `schema`  for operations over JSON schemas. |
| `id`           | integer | Unique numeric identifier of a template or a schema.                                                      |

##### GET

Retrieve the schema or the template associated to the unique numeric identifier.

**Example Requests:**

***Example schema request***
```http
GET /resource/v1/schema/1
```
**Example Response:**

```json
{
 "$schema": "https://json-schema.org/draft/2020-12/schema",
 "type": "object",
 "required": [
  "productIdentification",
  "generalInformation"
 ],
 "properties": {
  "productIdentification": {
   "type": "object",
   "required": [
    "batteryUID",
    "manufacturer"
   ],
   "properties": {
    "batteryUID": {
     "type": "string",
     "description": "Unique identifier for the battery"
    },
    "manufacturer": {
     "type": "object",
     "required": [
      "name",
      "streetName",
      "postalCode",
      "cityName"
     ],
     "properties": {
      "name": {
       "type": "string"
      },
      "streetName": {
       "type": "string"
      },
      "postalCode": {
       "type": "string"
      },
      "cityName": {
       "type": "string"
      },
      "countryCode": {
       "type": "string"
      }
     }
    }
   }
  },
  "generalInformation": {
   "type": "object",
   "required": [
    "batteryCategory",
    "batteryWeight",
    "manufacturingDate"
   ],
   "properties": {
    "batteryCategory": {
     "type": "string",
     "enum": [
      "LMT",
      "EV",
      "Industrial",
      "SLI"
     ],
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

***Example template request***
```http
GET /resource/v1/template/1
```
**Example Response:**

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
##### DELETE

Delete the schema/template identified by the unique numeric identifier.

**Example Requests:**

***Example schema request***
```http
DELETE /resource/v1/schema/1
```

***Example template request***
```http
DELETE /resource/v1/template/1
```

***Example Response***

204 No content

#### Group /resource/v1/{resourceType}/{name}/{version}

**Path Parameters:**

| Parameter      | Type   | Description                                                                                               |
|----------------|--------|-----------------------------------------------------------------------------------------------------------|
| `resourceType` | string | Either `template` for operations over SHACL templates, either `schema`  for operations over JSON schemas. |
| `name`         | string | The name of the resource to retrieve.                                                                     |
| `version`      | string | The version of the resource to retrieve.                                                                  |

##### GET

Retrieve the schema or the template associated with the specified name and version.

**Example Requests:**

***Example schema request***
```http
GET /resource/v1/schema/battery-dpp/1.0.0
```
**Example Response:**

```json
{
 "$schema": "https://json-schema.org/draft/2020-12/schema",
 "type": "object",
 "required": [
  "productIdentification",
  "generalInformation"
 ],
 "properties": {
  "productIdentification": {
   "type": "object",
   "required": [
    "batteryUID",
    "manufacturer"
   ],
   "properties": {
    "batteryUID": {
     "type": "string",
     "description": "Unique identifier for the battery"
    },
    "manufacturer": {
     "type": "object",
     "required": [
      "name",
      "streetName",
      "postalCode",
      "cityName"
     ],
     "properties": {
      "name": {
       "type": "string"
      },
      "streetName": {
       "type": "string"
      },
      "postalCode": {
       "type": "string"
      },
      "cityName": {
       "type": "string"
      },
      "countryCode": {
       "type": "string"
      }
     }
    }
   }
  },
  "generalInformation": {
   "type": "object",
   "required": [
    "batteryCategory",
    "batteryWeight",
    "manufacturingDate"
   ],
   "properties": {
    "batteryCategory": {
     "type": "string",
     "enum": [
      "LMT",
      "EV",
      "Industrial",
      "SLI"
     ],
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

***Example template request***
```http
GET /resource/v1/template/battery-dpp/1.0.0
```
**Example Response:**

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

## Authentication & Authorization

The application uses **OpenID Connect (OIDC)** for authentication and implements role-based access control.

### Supported Roles

- **`admin`**: Full administrative access: all api endpoints.
- **`eo`** (Economic Operator): Operator-level access: validation endpoints.
- **`eu`** (End User): End-user level access: all api endpoints.

### Role Mapping

External Identity Provider roles can be mapped to internal roles using the `registry.role-mappings` configuration:

```properties
validator.role-mappings=keycloak_admin:admin,idp_operator:eo,idp_user:eu
```

### JWT Role Claims

The application extracts roles from JWT tokens using the paths specified in `quarkus.oidc.roles.role-claim-path`. Multiple paths can be specified:

```properties
quarkus.oidc.roles.role-claim-path=group,realm_access.roles,resource_access.my-client.roles
```

The system searches each path in order until roles are found.

---

## License

This project is licensed under the Apache License 2.0 - see below for details.

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
