DO
$$ DECLARE
r RECORD;
BEGIN FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = current_schema()) LOOP EXECUTE 'DROP TABLE IF EXISTS ' || quote_ident(r.tablename) || ' CASCADE';
END LOOP;
END $$;
CREATE TABLE json_schemas
(
    id                   BIGSERIAL PRIMARY KEY,
    schema_name          VARCHAR(255) NOT NULL,
    description          VARCHAR(500),
    schema_version       VARCHAR(50)  NOT NULL,
    required_paths       TEXT[] NOT NULL, -- paths comuni/base
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



INSERT INTO json_schemas (schema_name,
                          description,
                          schema_version,
                          required_paths,
                          required_paths_count,
                          has_variants,
                          schema_content)
VALUES ('battery_passport',
        'Digital Product Passport for batteries according to Battery Regulation (EU) 2023/1542',
        '1.0.0',
        ARRAY[
            'productIdentification.batteryUID',
        'productIdentification.manufacturer.name',
        'productIdentification.manufacturer.streetName',
        'productIdentification.manufacturer.postalCode',
        'productIdentification.manufacturer.cityName',
        'generalInformation.batteryCategory',
        'generalInformation.batteryWeight',
        'generalInformation.manufacturingDate',
        'performance.ratedCapacity',
        'performance.nominalVoltage',
        'sustainability.carbonFootprint.lifeCycleCarbon',
        'sustainability.carbonFootprint.carbonFootprintPerformanceClass',
        'circularity.recyclableContent.cobalt',
        'circularity.recyclableContent.lithium',
        'circularity.recyclableContent.nickel',
        'circularity.recyclableContent.lead',
        'safety.safetyInstructions',
        'dataSource.lastUpdate'
            ],
        18,
        true,
        '{"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object","required":["productIdentification","generalInformation","performance","sustainability","circularity","safety","dataSource"],"properties":{"productIdentification":{"type":"object","required":["batteryUID","manufacturer"],"properties":{"batteryUID":{"type":"string","description":"Unique identifier for the battery"},"manufacturer":{"type":"object","required":["name","streetName","postalCode","cityName"],"properties":{"name":{"type":"string"},"streetName":{"type":"string"},"postalCode":{"type":"string"},"cityName":{"type":"string"},"countryCode":{"type":"string"}}}}},"generalInformation":{"type":"object","required":["batteryCategory","batteryWeight","manufacturingDate"],"properties":{"batteryCategory":{"type":"string","enum":["LMT","EV","Industrial","SLI"],"description":"LMT=Light Means of Transport, EV=Electric Vehicle, SLI=Starting Lighting Ignition"},"batteryWeight":{"type":"number","description":"Total weight in kilograms"},"manufacturingDate":{"type":"string","format":"date"},"batteryChemistry":{"type":"string","description":"Battery chemistry type (e.g., NMC, LFP, NCA)"}}},"performance":{"type":"object","required":["ratedCapacity","nominalVoltage"],"properties":{"ratedCapacity":{"type":"number","description":"Capacity in Ah"},"nominalVoltage":{"type":"number","description":"Voltage in V"},"powerCapability":{"type":"number","description":"Power in W"},"internalResistance":{"type":"number"},"roundTripEfficiency":{"type":"number"}}},"sustainability":{"type":"object","required":["carbonFootprint"],"properties":{"carbonFootprint":{"type":"object","required":["lifeCycleCarbon","carbonFootprintPerformanceClass"],"properties":{"lifeCycleCarbon":{"type":"number","description":"Total carbon footprint in kg CO2 eq"},"carbonFootprintPerformanceClass":{"type":"string","enum":["A","B","C","D","E"]}}},"criticalRawMaterials":{"type":"object","properties":{"nickel":{"type":"number"},"cobalt":{"type":"number"},"lithium":{"type":"number"}}}}},"circularity":{"type":"object","required":["recyclableContent"],"properties":{"recyclableContent":{"type":"object","required":["cobalt","lithium","nickel","lead"],"properties":{"cobalt":{"type":"number","description":"Percentage of recycled cobalt"},"lithium":{"type":"number","description":"Percentage of recycled lithium"},"nickel":{"type":"number","description":"Percentage of recycled nickel"},"lead":{"type":"number","description":"Percentage of recycled lead"}}},"dismantlingInstructions":{"type":"string"},"spareParts":{"type":"array"}}},"safety":{"type":"object","required":["safetyInstructions"],"properties":{"safetyInstructions":{"type":"string","description":"Safety and handling instructions"},"thermalStability":{"type":"number"}}},"dataSource":{"type":"object","required":["lastUpdate"],"properties":{"lastUpdate":{"type":"string","format":"date-time"},"dataProvider":{"type":"string"}}},"composition":{"type":"object","properties":{"cathodeActiveMaterial":{"type":"object","properties":{"nickelContent":{"type":"number"},"manganeseContent":{"type":"number"},"cobaltContent":{"type":"number"},"ironContent":{"type":"number"},"phosphateContent":{"type":"number"},"aluminumContent":{"type":"number"}}}}}}}'::jsonb);


INSERT INTO json_schemas (schema_name,
                          description,
                          schema_version,
                          required_paths,
                          required_paths_count,
                          has_variants,
                          schema_content)
VALUES ('electronics_dpp',
        'Digital Product Passport for electronic devices according to ESPR',
        '1.0.0',
        ARRAY[
            'deviceIdentification.serialNumber',
        'deviceIdentification.modelName',
        'deviceIdentification.manufacturer.companyName',
        'deviceIdentification.manufacturer.headquarters',
        'deviceSpecifications.deviceCategory',
        'deviceSpecifications.weight',
        'deviceSpecifications.dimensions',
        'componentDetails.mainComponents',
        'maintenanceGuidelines.cleaning',
        'maintenanceGuidelines.storage',
        'maintenanceGuidelines.handling',
        'environmentalImpact.waterUsage',
        'environmentalImpact.powerConsumption',
        'environmentalImpact.hazardousMaterials',
        'environmentalImpact.carbonEmissions',
        'endOfLife.repairability',
        'endOfLife.reuseContent',
        'endOfLife.expectedLifespan',
        'regulations.rohs',
        'regulations.weee'
            ],
        20,
        true,
        '{"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object","required":["deviceIdentification","deviceSpecifications","componentDetails","maintenanceGuidelines","environmentalImpact","endOfLife","regulations"],"properties":{"deviceIdentification":{"type":"object","required":["serialNumber","modelName","manufacturer"],"properties":{"serialNumber":{"type":"string","pattern":"^[A-Z0-9]{10,20}$","description":"Unique device serial number"},"modelName":{"type":"string"},"manufacturer":{"type":"object","required":["companyName","headquarters"],"properties":{"companyName":{"type":"string"},"headquarters":{"type":"string","pattern":"^[A-Z]{2}$","description":"ISO 3166-1 alpha-2 country code"},"facility":{"type":"string"}}}}},"deviceSpecifications":{"type":"object","required":["deviceCategory","weight","dimensions"],"properties":{"deviceCategory":{"type":"string","enum":["consumer","industrial","professional"]},"weight":{"type":"number","description":"Weight in kilograms"},"dimensions":{"type":"string","description":"Dimensions in format WxHxD cm"},"releaseYear":{"type":"integer"}}},"componentDetails":{"type":"object","required":["mainComponents"],"properties":{"mainComponents":{"type":"array","minItems":1,"items":{"type":"object","required":["componentName","quantity"],"properties":{"componentName":{"type":"string","description":"Name of component (e.g., battery, display, processor)"},"quantity":{"type":"integer","minimum":1}}}},"primaryMaterial":{"type":"string","enum":["plastic","metal","composite"],"description":"Dominant material composition"},"plasticPercentage":{"type":"number"},"metalPercentage":{"type":"number"}}},"maintenanceGuidelines":{"type":"object","required":["cleaning","storage","handling"],"properties":{"cleaning":{"type":"string","description":"Cleaning procedures and recommendations"},"storage":{"type":"string","description":"Storage conditions and requirements"},"handling":{"type":"string","description":"Safe handling instructions"},"batteryMaintenance":{"type":"string"},"softwareUpdates":{"type":"string"}}},"environmentalImpact":{"type":"object","required":["waterUsage","powerConsumption","hazardousMaterials","carbonEmissions"],"properties":{"waterUsage":{"type":"number","description":"Water usage in liters during production"},"powerConsumption":{"type":"number","description":"Power consumption in watts during operation"},"hazardousMaterials":{"type":"array","items":{"type":"string"},"description":"List of hazardous materials present"},"carbonEmissions":{"type":"number","description":"Carbon emissions in kg CO2 eq during lifecycle"},"energyStarCertified":{"type":"boolean"},"conflictMinerals":{"type":"array"},"batteryCapacity":{"type":"number"},"standbyPower":{"type":"number"},"electronicWasteGeneration":{"type":"number"},"toxicSubstances":{"type":"array"},"manufacturingEmissions":{"type":"number"}}},"endOfLife":{"type":"object","required":["repairability","reuseContent","expectedLifespan"],"properties":{"repairability":{"type":"string","enum":["excellent","good","fair","poor"]},"reuseContent":{"type":"number","minimum":0,"maximum":100,"description":"Percentage of reused materials"},"expectedLifespan":{"type":"number","description":"Expected lifespan in years"},"disassemblyEase":{"type":"boolean"},"componentRecovery":{"type":"boolean"},"refurbishmentPotential":{"type":"string"},"upgradeability":{"type":"string"}}},"regulations":{"type":"object","required":["rohs","weee"],"properties":{"rohs":{"type":"boolean","description":"RoHS compliance (restriction of hazardous substances)"},"weee":{"type":"boolean","description":"WEEE directive compliance"}}},"supplyChain":{"type":"object","properties":{"battery":{"type":"object","properties":{"source":{"type":"string"},"certifications":{"type":"array"}}}}}},"oneOf":[{"properties":{"deviceSpecifications":{"properties":{"deviceCategory":{"const":"consumer"}}},"consumerFeatures":{"type":"object","required":["warrantyPeriod","userManualLanguages","customerSupport"],"properties":{"warrantyPeriod":{"type":"integer","description":"Warranty period in months"},"userManualLanguages":{"type":"array","items":{"type":"string"}},"customerSupport":{"type":"string"},"ecoLabel":{"type":"boolean"},"energyRating":{"type":"string"}}},"environmentalImpact":{"properties":{"batteryCapacity":{"type":"number"},"standbyPower":{"type":"number"}}}},"required":["consumerFeatures"]},{"properties":{"deviceSpecifications":{"properties":{"deviceCategory":{"const":"industrial"}}},"industrialFeatures":{"type":"object","required":["operatingTemperature","certifications","maintenanceSchedule"],"properties":{"operatingTemperature":{"type":"string","description":"Operating temperature range"},"certifications":{"type":"array","items":{"type":"string"},"description":"Industrial certifications"},"maintenanceSchedule":{"type":"string"},"dutyCycle":{"type":"number"},"meanTimeBetweenFailures":{"type":"number"}}},"environmentalImpact":{"properties":{"electronicWasteGeneration":{"type":"number"},"toxicSubstances":{"type":"array"}}}},"required":["industrialFeatures"]},{"properties":{"deviceSpecifications":{"properties":{"deviceCategory":{"const":"professional"}}},"professionalFeatures":{"type":"object","required":["technicalSupport","calibrationRequired","performanceMetrics"],"properties":{"technicalSupport":{"type":"string","description":"Technical support availability"},"calibrationRequired":{"type":"boolean"},"performanceMetrics":{"type":"object"},"softwareCompatibility":{"type":"array"},"networkCapabilities":{"type":"string"}}},"endOfLife":{"properties":{"disassemblyEase":{"type":"boolean"},"componentRecovery":{"type":"boolean"},"refurbishmentPotential":{"type":"string"}}},"environmentalImpact":{"properties":{"manufacturingEmissions":{"type":"number"}}}},"required":["professionalFeatures"]}]}'::jsonb);

INSERT INTO schema_variants (schema_metadata_id,
                             variant_type,
                             variant_index,
                             required_paths,
                             required_paths_count,
                             discriminator_path,
                             discriminator_value)
VALUES ((SELECT id FROM json_schemas WHERE schema_name = 'electronics_dpp' AND schema_version = '1.0.0'),
        'oneOf',
        0,
        ARRAY[
            'consumerFeatures.warrantyPeriod',
        'consumerFeatures.userManualLanguages',
        'consumerFeatures.customerSupport',
        'consumerFeatures.ecoLabel',
        'consumerFeatures.energyRating',
        'environmentalImpact.batteryCapacity',
        'environmentalImpact.standbyPower'
            ],
        7,
        'deviceSpecifications.deviceCategory',
        'consumer');

INSERT INTO schema_variants (schema_metadata_id,
                             variant_type,
                             variant_index,
                             required_paths,
                             required_paths_count,
                             discriminator_path,
                             discriminator_value)
VALUES ((SELECT id FROM json_schemas WHERE schema_name = 'electronics_dpp' AND schema_version = '1.0.0'),
        'oneOf',
        1,
        ARRAY[
            'industrialFeatures.operatingTemperature',
        'industrialFeatures.certifications',
        'industrialFeatures.maintenanceSchedule',
        'industrialFeatures.dutyCycle',
        'industrialFeatures.meanTimeBetweenFailures',
        'environmentalImpact.electronicWasteGeneration',
        'environmentalImpact.toxicSubstances'
            ],
        7,
        'deviceSpecifications.deviceCategory',
        'industrial');

INSERT INTO schema_variants (schema_metadata_id,
                             variant_type,
                             variant_index,
                             required_paths,
                             required_paths_count,
                             discriminator_path,
                             discriminator_value)
VALUES ((SELECT id FROM json_schemas WHERE schema_name = 'electronics_dpp' AND schema_version = '1.0.0'),
        'oneOf',
        2,
        ARRAY[
            'professionalFeatures.technicalSupport',
        'professionalFeatures.calibrationRequired',
        'professionalFeatures.performanceMetrics',
        'professionalFeatures.softwareCompatibility',
        'professionalFeatures.networkCapabilities',
        'endOfLife.disassemblyEase',
        'endOfLife.componentRecovery',
        'endOfLife.refurbishmentPotential',
        'environmentalImpact.manufacturingEmissions'
            ],
        9,
        'deviceSpecifications.deviceCategory',
        'professional');

INSERT INTO json_schemas (schema_name,
                          description,
                          schema_version,
                          required_paths,
                          required_paths_count,
                          has_variants,
                          schema_content)
VALUES ('vehicle_dpp',
        'Digital Product Passport for vehicles with dynamic sensor and metric tracking',
        '1.0.0',
        ARRAY[
            'vehicleCharacteristics',
        'lifecycleManagement.serviceability',
        'ecologicalFootprint',
        'ecologicalFootprint.fuelConsumption',
        'partsInventory.primaryParts[].count',
        'partsInventory.primaryParts[].partName',
        'vehicleIdentification.brand',
        'vehicleIdentification',
        'lifecycleManagement',
        'operatingInstructions.routine',
        'vehicleCharacteristics.mass',
        'operatingInstructions.winterCare',
        'vehicleIdentification.assembler.organization',
        'operatingInstructions.safetyChecks',
        'vehicleIdentification.vin',
        'partsInventory.primaryParts',
        'lifecycleManagement.operationalYears',
        'standardsCompliance.euro6',
        'lifecycleManagement.secondhandValue',
        'vehicleIdentification.assembler.location',
        'vehicleCharacteristics.engineType',
        'operatingInstructions',
        'standardsCompliance.safetyRating',
        'partsInventory',
        'standardsCompliance',
        'vehicleCharacteristics.vehicleType',
        'vehicleIdentification.assembler',
        'ecologicalFootprint.emissions'
            ],
        18,
        false,
        '{"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object","required":["vehicleIdentification","vehicleCharacteristics","partsInventory","operatingInstructions","ecologicalFootprint","lifecycleManagement","standardsCompliance"],"properties":{"vehicleIdentification":{"type":"object","required":["vin","brand","assembler"],"properties":{"vin":{"type":"string","pattern":"^[A-HJ-NPR-Z0-9]{17}$","description":"Vehicle Identification Number"},"brand":{"type":"string"},"assembler":{"type":"object","required":["organization","location"],"properties":{"organization":{"type":"string"},"location":{"type":"string","pattern":"^[A-Z]{2}$","description":"ISO 3166-1 alpha-2 country code"},"plantId":{"type":"string"}}}}},"vehicleCharacteristics":{"type":"object","required":["vehicleType","mass","engineType"],"properties":{"vehicleType":{"type":"string","enum":["sedan","suv","truck","van","motorcycle","bus"]},"mass":{"type":"number","description":"Vehicle mass in kilograms"},"engineType":{"type":"string","enum":["electric","hybrid","diesel","gasoline","hydrogen"]},"productionDate":{"type":"string","format":"date"}}},"partsInventory":{"type":"object","required":["primaryParts"],"properties":{"primaryParts":{"type":"array","minItems":1,"items":{"type":"object","required":["partName","count"],"properties":{"partName":{"type":"string"},"count":{"type":"integer","minimum":1}}}},"dominantMaterial":{"type":"string","enum":["steel","aluminum","carbonFiber","mixed"]}}},"operatingInstructions":{"type":"object","required":["routine","winterCare","safetyChecks"],"properties":{"routine":{"type":"string","description":"Regular maintenance instructions"},"winterCare":{"type":"string","description":"Cold weather operating guidelines"},"safetyChecks":{"type":"string","description":"Pre-operation safety inspection procedures"}}},"ecologicalFootprint":{"type":"object","required":["fuelConsumption","emissions"],"properties":{"fuelConsumption":{"type":"number","description":"Fuel consumption in liters per 100km or kWh per 100km"},"emissions":{"type":"number","description":"CO2 emissions in g/km"}}},"lifecycleManagement":{"type":"object","required":["serviceability","secondhandValue","operationalYears"],"properties":{"serviceability":{"type":"string","enum":["excellent","good","moderate","difficult"]},"secondhandValue":{"type":"number","minimum":0,"maximum":100,"description":"Percentage of residual value after use"},"operationalYears":{"type":"number","description":"Expected operational lifespan in years"}}},"standardsCompliance":{"type":"object","required":["euro6","safetyRating"],"properties":{"euro6":{"type":"boolean","description":"EURO 6 emissions standard compliance"},"safetyRating":{"type":"string","description":"Safety rating (e.g., 5-star NCAP)"}}}},"patternProperties":{"^sensor_[a-z0-9_]+$":{"type":"object","required":["reading","unit","timestamp"],"properties":{"reading":{"type":"number","description":"Sensor reading value"},"unit":{"type":"string","description":"Unit of measurement"},"timestamp":{"type":"string","format":"date-time","description":"Reading timestamp"},"accuracy":{"type":"number","description":"Measurement accuracy percentage"},"calibrationDate":{"type":"string","format":"date"}}},"^metric_[a-z0-9_]+$":{"type":"object","required":["value","threshold"],"properties":{"value":{"type":"number","description":"Performance metric value"},"threshold":{"type":"number","description":"Acceptable threshold"},"status":{"type":"string","enum":["normal","warning","critical"]},"lastUpdated":{"type":"string","format":"date-time"}}},"^certification_[a-z0-9_]+$":{"type":"object","required":["issuedBy","validUntil","status"],"properties":{"issuedBy":{"type":"string","description":"Certification authority"},"validUntil":{"type":"string","format":"date","description":"Expiration date"},"status":{"type":"string","enum":["valid","expired","revoked"]},"documentUrl":{"type":"string","format":"uri"},"verificationCode":{"type":"string"}}},"^test_[a-z0-9_]+$":{"type":"object","required":["result","date","inspector"],"properties":{"result":{"type":"string","enum":["passed","failed","conditional"]},"date":{"type":"string","format":"date"},"inspector":{"type":"string"},"notes":{"type":"string"},"nextTestDue":{"type":"string","format":"date"}}}}}'::jsonb);

INSERT INTO schema_pattern_properties (schema_metadata_id,
                                       pattern_regex,
                                       path_prefix,
                                       required_sub_paths)
VALUES ((SELECT id FROM json_schemas WHERE schema_name = 'vehicle_dpp' AND schema_version = '1.0.0'),
        '^sensor_[a-z0-9_]+$',
        'sensor_',
        ARRAY['reading', 'unit', 'timestamp']);

INSERT INTO schema_pattern_properties (schema_metadata_id,
                                       pattern_regex,
                                       path_prefix,
                                       required_sub_paths)
VALUES ((SELECT id FROM json_schemas WHERE schema_name = 'vehicle_dpp' AND schema_version = '1.0.0'),
        '^metric_[a-z0-9_]+$',
        'metric_',
        ARRAY['value', 'threshold']);

INSERT INTO schema_pattern_properties (schema_metadata_id,
                                       pattern_regex,
                                       path_prefix,
                                       required_sub_paths)
VALUES ((SELECT id FROM json_schemas WHERE schema_name = 'vehicle_dpp' AND schema_version = '1.0.0'),
        '^certification_[a-z0-9_]+$',
        'certification_',
        ARRAY['issuedBy', 'validUntil', 'status']);

INSERT INTO schema_pattern_properties (schema_metadata_id,
                                       pattern_regex,
                                       path_prefix,
                                       required_sub_paths)
VALUES ((SELECT id FROM json_schemas WHERE schema_name = 'vehicle_dpp' AND schema_version = '1.0.0'),
        '^test_[a-z0-9_]+$',
        'test_',
        ARRAY['result', 'date', 'inspector']);


-- First shacl insert
INSERT INTO shacl_templates (template_name, description, template_version, context_uri, shacl_content)
VALUES ('Vehicle-DPP-AllTargets',
        'Digital Product Passport for vehicles - All shapes have targetClass',
        '1.0.0',
        'http://example.org/vehicle-dpp',
        '@prefix sh: <http://www.w3.org/ns/shacl#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix vdpp: <http://example.org/vehicle-dpp#> .
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .

# STRATEGY 1: ALL TARGET CLASSES DEFINED
# Ogni NodeShape ha il proprio targetClass per permettere matching diretto

# Ontology declaration
<http://example.org/vehicle-dpp> a owl:Ontology ;
    owl:imports <http://example.org/vehicle-dpp> .

# Main Vehicle DPP Shape
vdpp:VehicleDPPShape
    a sh:NodeShape ;
    sh:targetClass vdpp:VehicleDPP ;
    sh:property [
        sh:path vdpp:vehicleIdentification ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:node vdpp:VehicleIdentificationShape ;
    ] ;
    sh:property [
        sh:path vdpp:vehicleCharacteristics ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:node vdpp:VehicleCharacteristicsShape ;
    ] ;
    sh:property [
        sh:path vdpp:partsInventory ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:node vdpp:PartsInventoryShape ;
    ] ;
    sh:property [
        sh:path vdpp:operatingInstructions ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:node vdpp:OperatingInstructionsShape ;
    ] ;
    sh:property [
        sh:path vdpp:ecologicalFootprint ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:node vdpp:EcologicalFootprintShape ;
    ] ;
    sh:property [
        sh:path vdpp:lifecycleManagement ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:node vdpp:LifecycleManagementShape ;
    ] ;
    sh:property [
        sh:path vdpp:standardsCompliance ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:node vdpp:StandardsComplianceShape ;
    ] .

# Vehicle Identification Shape
vdpp:VehicleIdentificationShape
    a sh:NodeShape ;
    sh:targetClass vdpp:VehicleIdentification ;
    sh:property [
        sh:path vdpp:vin ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:pattern "^[A-HJ-NPR-Z0-9]{17}$" ;
        sh:description "Vehicle Identification Number" ;
    ] ;
    sh:property [
        sh:path vdpp:brand ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
    ] ;
    sh:property [
        sh:path vdpp:assembler ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:node vdpp:AssemblerShape ;
    ] .

# Assembler Shape
vdpp:AssemblerShape
    a sh:NodeShape ;
    sh:targetClass vdpp:Assembler ;
    sh:property [
        sh:path vdpp:organization ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
    ] ;
    sh:property [
        sh:path vdpp:location ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:pattern "^[A-Z]{2}$" ;
        sh:description "ISO 3166-1 alpha-2 country code" ;
    ] ;
    sh:property [
        sh:path vdpp:plantId ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
    ] .

# Vehicle Characteristics Shape
vdpp:VehicleCharacteristicsShape
    a sh:NodeShape ;
    sh:targetClass vdpp:VehicleCharacteristics ;
    sh:property [
        sh:path vdpp:vehicleType ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:in ("sedan" "suv" "truck" "van" "motorcycle" "bus") ;
    ] ;
    sh:property [
        sh:path vdpp:mass ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
        sh:description "Vehicle mass in kilograms" ;
    ] ;
    sh:property [
        sh:path vdpp:engineType ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:in ("electric" "hybrid" "diesel" "gasoline" "hydrogen") ;
    ] ;
    sh:property [
        sh:path vdpp:productionDate ;
        sh:maxCount 1 ;
        sh:datatype xsd:date ;
    ] .

# Parts Inventory Shape
vdpp:PartsInventoryShape
    a sh:NodeShape ;
    sh:targetClass vdpp:PartsInventory ;
    sh:property [
        sh:path vdpp:primaryParts ;
        sh:minCount 1 ;
        sh:node vdpp:PartShape ;
    ] ;
    sh:property [
        sh:path vdpp:dominantMaterial ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:in ("steel" "aluminum" "carbonFiber" "mixed") ;
    ] .

# Part Shape
vdpp:PartShape
    a sh:NodeShape ;
    sh:targetClass vdpp:Part ;
    sh:property [
        sh:path vdpp:partName ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
    ] ;
    sh:property [
        sh:path vdpp:count ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:integer ;
        sh:minInclusive 1 ;
    ] .

# Operating Instructions Shape
vdpp:OperatingInstructionsShape
    a sh:NodeShape ;
    sh:targetClass vdpp:OperatingInstructions ;
    sh:property [
        sh:path vdpp:routine ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:description "Regular maintenance instructions" ;
    ] ;
    sh:property [
        sh:path vdpp:winterCare ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:description "Cold weather operating guidelines" ;
    ] ;
    sh:property [
        sh:path vdpp:safetyChecks ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:description "Pre-operation safety inspection procedures" ;
    ] .

# Ecological Footprint Shape
vdpp:EcologicalFootprintShape
    a sh:NodeShape ;
    sh:targetClass vdpp:EcologicalFootprint ;
    sh:property [
        sh:path vdpp:fuelConsumption ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
        sh:description "Fuel consumption in liters per 100km or kWh per 100km" ;
    ] ;
    sh:property [
        sh:path vdpp:emissions ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
        sh:description "CO2 emissions in g/km" ;
    ] .

# Lifecycle Management Shape
vdpp:LifecycleManagementShape
    a sh:NodeShape ;
    sh:targetClass vdpp:LifecycleManagement ;
    sh:property [
        sh:path vdpp:serviceability ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:in ("excellent" "good" "moderate" "difficult") ;
    ] ;
    sh:property [
        sh:path vdpp:secondhandValue ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
        sh:minInclusive 0 ;
        sh:maxInclusive 100 ;
        sh:description "Percentage of residual value after use" ;
    ] ;
    sh:property [
        sh:path vdpp:operationalYears ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
        sh:description "Expected operational lifespan in years" ;
    ] .

# Standards Compliance Shape
vdpp:StandardsComplianceShape
    a sh:NodeShape ;
    sh:targetClass vdpp:StandardsCompliance ;
    sh:property [
        sh:path vdpp:euro6 ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:boolean ;
        sh:description "EURO 6 emissions standard compliance" ;
    ] ;
    sh:property [
        sh:path vdpp:safetyRating ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:description "Safety rating (e.g., 5-star NCAP)" ;
    ] .

# Sensor Reading Shape (for pattern properties sensor_*)
vdpp:SensorReadingShape
    a sh:NodeShape ;
    sh:targetClass vdpp:SensorReading ;
    sh:property [
        sh:path vdpp:reading ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
        sh:description "Sensor reading value" ;
    ] ;
    sh:property [
        sh:path vdpp:unit ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:description "Unit of measurement" ;
    ] ;
    sh:property [
        sh:path vdpp:timestamp ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:dateTime ;
        sh:description "Reading timestamp" ;
    ] ;
    sh:property [
        sh:path vdpp:accuracy ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
        sh:description "Measurement accuracy percentage" ;
    ] ;
    sh:property [
        sh:path vdpp:calibrationDate ;
        sh:maxCount 1 ;
        sh:datatype xsd:date ;
    ] .

# Metric Shape (for pattern properties metric_*)
vdpp:MetricShape
    a sh:NodeShape ;
    sh:targetClass vdpp:Metric ;
    sh:property [
        sh:path vdpp:value ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
        sh:description "Performance metric value" ;
    ] ;
    sh:property [
        sh:path vdpp:threshold ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
        sh:description "Acceptable threshold" ;
    ] ;
    sh:property [
        sh:path vdpp:status ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:in ("normal" "warning" "critical") ;
    ] ;
    sh:property [
        sh:path vdpp:lastUpdated ;
        sh:maxCount 1 ;
        sh:datatype xsd:dateTime ;
    ] .

# Certification Shape (for pattern properties certification_*)
vdpp:CertificationShape
    a sh:NodeShape ;
    sh:targetClass vdpp:Certification ;
    sh:property [
        sh:path vdpp:issuedBy ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:description "Certification authority" ;
    ] ;
    sh:property [
        sh:path vdpp:validUntil ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:date ;
        sh:description "Expiration date" ;
    ] ;
    sh:property [
        sh:path vdpp:status ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:in ("valid" "expired" "revoked") ;
    ] ;
    sh:property [
        sh:path vdpp:documentUrl ;
        sh:maxCount 1 ;
        sh:nodeKind sh:IRI ;
    ] ;
    sh:property [
        sh:path vdpp:verificationCode ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
    ] .

# Test Shape (for pattern properties test_*)
vdpp:TestShape
    a sh:NodeShape ;
    sh:targetClass vdpp:Test ;
    sh:property [
        sh:path vdpp:result ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:in ("passed" "failed" "conditional") ;
    ] ;
    sh:property [
        sh:path vdpp:date ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:date ;
    ] ;
    sh:property [
        sh:path vdpp:inspector ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
    ] ;
    sh:property [
        sh:path vdpp:notes ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
    ] ;
    sh:property [
        sh:path vdpp:nextTestDue ;
        sh:maxCount 1 ;
        sh:datatype xsd:date ;
    ] .
');

INSERT INTO shacl_shapes (template_id, shape_id, target_class, vocabulary_uri, ontology_uri)
VALUES
    ((SELECT id FROM shacl_templates WHERE template_name = 'Vehicle-DPP-AllTargets' AND template_version = '1.0.0'),
     'http://example.org/vehicle-dpp#VehicleDPPShape',
     'http://example.org/vehicle-dpp#VehicleDPP',
     'http://example.org/vehicle-dpp#',
     'http://example.org/vehicle-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Vehicle-DPP-AllTargets' AND template_version = '1.0.0'),
     'http://example.org/vehicle-dpp#VehicleIdentificationShape',
     'http://example.org/vehicle-dpp#VehicleIdentification',
     'http://example.org/vehicle-dpp#',
     'http://example.org/vehicle-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Vehicle-DPP-AllTargets' AND template_version = '1.0.0'),
     'http://example.org/vehicle-dpp#AssemblerShape',
     'http://example.org/vehicle-dpp#Assembler',
     'http://example.org/vehicle-dpp#',
     'http://example.org/vehicle-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Vehicle-DPP-AllTargets' AND template_version = '1.0.0'),
     'http://example.org/vehicle-dpp#VehicleCharacteristicsShape',
     'http://example.org/vehicle-dpp#VehicleCharacteristics',
     'http://example.org/vehicle-dpp#',
     'http://example.org/vehicle-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Vehicle-DPP-AllTargets' AND template_version = '1.0.0'),
     'http://example.org/vehicle-dpp#PartsInventoryShape',
     'http://example.org/vehicle-dpp#PartsInventory',
     'http://example.org/vehicle-dpp#',
     'http://example.org/vehicle-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Vehicle-DPP-AllTargets' AND template_version = '1.0.0'),
     'http://example.org/vehicle-dpp#PartShape',
     'http://example.org/vehicle-dpp#Part',
     'http://example.org/vehicle-dpp#',
     'http://example.org/vehicle-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Vehicle-DPP-AllTargets' AND template_version = '1.0.0'),
     'http://example.org/vehicle-dpp#OperatingInstructionsShape',
     'http://example.org/vehicle-dpp#OperatingInstructions',
     'http://example.org/vehicle-dpp#',
     'http://example.org/vehicle-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Vehicle-DPP-AllTargets' AND template_version = '1.0.0'),
     'http://example.org/vehicle-dpp#EcologicalFootprintShape',
     'http://example.org/vehicle-dpp#EcologicalFootprint',
     'http://example.org/vehicle-dpp#',
     'http://example.org/vehicle-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Vehicle-DPP-AllTargets' AND template_version = '1.0.0'),
     'http://example.org/vehicle-dpp#LifecycleManagementShape',
     'http://example.org/vehicle-dpp#LifecycleManagement',
     'http://example.org/vehicle-dpp#',
     'http://example.org/vehicle-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Vehicle-DPP-AllTargets' AND template_version = '1.0.0'),
     'http://example.org/vehicle-dpp#StandardsComplianceShape',
     'http://example.org/vehicle-dpp#StandardsCompliance',
     'http://example.org/vehicle-dpp#',
     'http://example.org/vehicle-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Vehicle-DPP-AllTargets' AND template_version = '1.0.0'),
     'http://example.org/vehicle-dpp#SensorReadingShape',
     'http://example.org/vehicle-dpp#SensorReading',
     'http://example.org/vehicle-dpp#',
     'http://example.org/vehicle-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Vehicle-DPP-AllTargets' AND template_version = '1.0.0'),
     'http://example.org/vehicle-dpp#MetricShape',
     'http://example.org/vehicle-dpp#Metric',
     'http://example.org/vehicle-dpp#',
     'http://example.org/vehicle-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Vehicle-DPP-AllTargets' AND template_version = '1.0.0'),
     'http://example.org/vehicle-dpp#CertificationShape',
     'http://example.org/vehicle-dpp#Certification',
     'http://example.org/vehicle-dpp#',
     'http://example.org/vehicle-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Vehicle-DPP-AllTargets' AND template_version = '1.0.0'),
     'http://example.org/vehicle-dpp#TestShape',
     'http://example.org/vehicle-dpp#Test',
     'http://example.org/vehicle-dpp#',
     'http://example.org/vehicle-dpp');

-- second shacl insert
-- second shacl insert
INSERT INTO shacl_templates (template_name, description, template_version, context_uri, shacl_content)
VALUES ('Battery-Passport-Vocabulary',
        'Digital Product Passport for batteries - Vocabulary optimized',
        '1.0.0',
        'http://example.org/battery-passport',
        '@prefix sh: <http://www.w3.org/ns/shacl#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix bp: <http://example.org/battery-passport#> .
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .

# STRATEGY 2: OPTIMIZED FOR VOCABULARY_URI EXTRACTION
# Tutte le proprietà usano lo stesso namespace per facilitare l''estrazione
# Alcune shape hanno targetClass, altre no (approccio misto)

# Ontology declaration
<http://example.org/battery-passport> a owl:Ontology ;
    owl:imports <http://example.org/battery-passport> .

# Main Battery Passport Shape
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
        sh:path bp:generalInformation ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:node bp:GeneralInformationShape ;
    ] ;
    sh:property [
        sh:path bp:performance ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:node bp:PerformanceShape ;
    ] ;
    sh:property [
        sh:path bp:sustainability ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:node bp:SustainabilityShape ;
    ] ;
    sh:property [
        sh:path bp:circularity ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:node bp:CircularityShape ;
    ] ;
    sh:property [
        sh:path bp:safety ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:node bp:SafetyShape ;
    ] ;
    sh:property [
        sh:path bp:dataSource ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:node bp:DataSourceShape ;
    ] ;
    sh:property [
        sh:path bp:composition ;
        sh:maxCount 1 ;
        sh:node bp:CompositionShape ;
    ] .

# Product Identification Shape - NO targetClass (nidificata)
bp:ProductIdentificationShape
    a sh:NodeShape ;
    sh:property [
        sh:path bp:batteryUID ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:description "Unique identifier for the battery" ;
    ] ;
    sh:property [
        sh:path bp:manufacturer ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:node bp:ManufacturerShape ;
    ] .

# Manufacturer Shape - NO targetClass (nidificata)
bp:ManufacturerShape
    a sh:NodeShape ;
    sh:property [
        sh:path bp:name ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
    ] ;
    sh:property [
        sh:path bp:streetName ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
    ] ;
    sh:property [
        sh:path bp:postalCode ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
    ] ;
    sh:property [
        sh:path bp:cityName ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
    ] ;
    sh:property [
        sh:path bp:countryCode ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
    ] .

# General Information Shape - CON targetClass
bp:GeneralInformationShape
    a sh:NodeShape ;
    sh:targetClass bp:GeneralInformation ;
    sh:property [
        sh:path bp:batteryCategory ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:in ("LMT" "EV" "Industrial" "SLI") ;
        sh:description "LMT=Light Means of Transport, EV=Electric Vehicle, SLI=Starting Lighting Ignition" ;
    ] ;
    sh:property [
        sh:path bp:batteryWeight ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
        sh:description "Total weight in kilograms" ;
    ] ;
    sh:property [
        sh:path bp:manufacturingDate ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:date ;
    ] ;
    sh:property [
        sh:path bp:batteryChemistry ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:description "Battery chemistry type (e.g., NMC, LFP, NCA)" ;
    ] .

# Performance Shape - CON targetClass
bp:PerformanceShape
    a sh:NodeShape ;
    sh:targetClass bp:Performance ;
    sh:property [
        sh:path bp:ratedCapacity ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
        sh:description "Capacity in Ah" ;
    ] ;
    sh:property [
        sh:path bp:nominalVoltage ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
        sh:description "Voltage in V" ;
    ] ;
    sh:property [
        sh:path bp:powerCapability ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
        sh:description "Power in W" ;
    ] ;
    sh:property [
        sh:path bp:internalResistance ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
    ] ;
    sh:property [
        sh:path bp:roundTripEfficiency ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
    ] .

# Sustainability Shape - NO targetClass
bp:SustainabilityShape
    a sh:NodeShape ;
    sh:property [
        sh:path bp:carbonFootprint ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:node bp:CarbonFootprintShape ;
    ] ;
    sh:property [
        sh:path bp:criticalRawMaterials ;
        sh:maxCount 1 ;
        sh:node bp:CriticalRawMaterialsShape ;
    ] .

# Carbon Footprint Shape - NO targetClass
bp:CarbonFootprintShape
    a sh:NodeShape ;
    sh:property [
        sh:path bp:lifeCycleCarbon ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
        sh:description "Total carbon footprint in kg CO2 eq" ;
    ] ;
    sh:property [
        sh:path bp:carbonFootprintPerformanceClass ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:in ("A" "B" "C" "D" "E") ;
    ] .

# Critical Raw Materials Shape - NO targetClass
bp:CriticalRawMaterialsShape
    a sh:NodeShape ;
    sh:property [
        sh:path bp:nickel ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
    ] ;
    sh:property [
        sh:path bp:cobalt ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
    ] ;
    sh:property [
        sh:path bp:lithium ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
    ] .

# Circularity Shape - CON targetClass
bp:CircularityShape
    a sh:NodeShape ;
    sh:targetClass bp:Circularity ;
    sh:property [
        sh:path bp:recyclableContent ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:node bp:RecyclableContentShape ;
    ] ;
    sh:property [
        sh:path bp:dismantlingInstructions ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
    ] ;
    sh:property [
        sh:path bp:spareParts ;
        sh:maxCount 1 ;
    ] .

# Recyclable Content Shape - NO targetClass
bp:RecyclableContentShape
    a sh:NodeShape ;
    sh:property [
        sh:path bp:cobalt ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
        sh:description "Percentage of recycled cobalt" ;
    ] ;
    sh:property [
        sh:path bp:lithium ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
        sh:description "Percentage of recycled lithium" ;
    ] ;
    sh:property [
        sh:path bp:nickel ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
        sh:description "Percentage of recycled nickel" ;
    ] ;
    sh:property [
        sh:path bp:lead ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
        sh:description "Percentage of recycled lead" ;
    ] .

# Safety Shape - NO targetClass
bp:SafetyShape
    a sh:NodeShape ;
    sh:property [
        sh:path bp:safetyInstructions ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:description "Safety and handling instructions" ;
    ] ;
    sh:property [
        sh:path bp:thermalStability ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
    ] .

# Data Source Shape - NO targetClass
bp:DataSourceShape
    a sh:NodeShape ;
    sh:property [
        sh:path bp:lastUpdate ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:dateTime ;
    ] ;
    sh:property [
        sh:path bp:dataProvider ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
    ] .

# Composition Shape - NO targetClass
bp:CompositionShape
    a sh:NodeShape ;
    sh:property [
        sh:path bp:cathodeActiveMaterial ;
        sh:maxCount 1 ;
        sh:node bp:CathodeActiveMaterialShape ;
    ] .

# Cathode Active Material Shape - NO targetClass
bp:CathodeActiveMaterialShape
    a sh:NodeShape ;
    sh:property [
        sh:path bp:nickelContent ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
    ] ;
    sh:property [
        sh:path bp:manganeseContent ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
    ] ;
    sh:property [
        sh:path bp:cobaltContent ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
    ] ;
    sh:property [
        sh:path bp:ironContent ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
    ] ;
    sh:property [
        sh:path bp:phosphateContent ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
    ] ;
    sh:property [
        sh:path bp:aluminumContent ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
    ] .
');

INSERT INTO shacl_shapes (template_id, shape_id, target_class, vocabulary_uri, ontology_uri)
VALUES
    ((SELECT id FROM shacl_templates WHERE template_name = 'Battery-Passport-Vocabulary' AND template_version = '1.0.0'),
     'http://example.org/battery-passport#BatteryPassportShape',
     'http://example.org/battery-passport#BatteryPassport',
     'http://example.org/battery-passport#',
     'http://example.org/battery-passport'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Battery-Passport-Vocabulary' AND template_version = '1.0.0'),
     'http://example.org/battery-passport#GeneralInformationShape',
     'http://example.org/battery-passport#GeneralInformation',
     'http://example.org/battery-passport#',
     'http://example.org/battery-passport'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Battery-Passport-Vocabulary' AND template_version = '1.0.0'),
     'http://example.org/battery-passport#PerformanceShape',
     'http://example.org/battery-passport#Performance',
     'http://example.org/battery-passport#',
     'http://example.org/battery-passport'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Battery-Passport-Vocabulary' AND template_version = '1.0.0'),
     'http://example.org/battery-passport#CircularityShape',
     'http://example.org/battery-passport#Circularity',
     'http://example.org/battery-passport#',
     'http://example.org/battery-passport'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Battery-Passport-Vocabulary' AND template_version = '1.0.0'),
     'http://example.org/battery-passport#ProductIdentificationShape',
     NULL,
     'http://example.org/battery-passport#',
     'http://example.org/battery-passport'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Battery-Passport-Vocabulary' AND template_version = '1.0.0'),
     'http://example.org/battery-passport#ManufacturerShape',
     NULL,
     'http://example.org/battery-passport#',
     'http://example.org/battery-passport'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Battery-Passport-Vocabulary' AND template_version = '1.0.0'),
     'http://example.org/battery-passport#SustainabilityShape',
     NULL,
     'http://example.org/battery-passport#',
     'http://example.org/battery-passport'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Battery-Passport-Vocabulary' AND template_version = '1.0.0'),
     'http://example.org/battery-passport#CarbonFootprintShape',
     NULL,
     'http://example.org/battery-passport#',
     'http://example.org/battery-passport'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Battery-Passport-Vocabulary' AND template_version = '1.0.0'),
     'http://example.org/battery-passport#CriticalRawMaterialsShape',
     NULL,
     'http://example.org/battery-passport#',
     'http://example.org/battery-passport'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Battery-Passport-Vocabulary' AND template_version = '1.0.0'),
     'http://example.org/battery-passport#RecyclableContentShape',
     NULL,
     'http://example.org/battery-passport#',
     'http://example.org/battery-passport'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Battery-Passport-Vocabulary' AND template_version = '1.0.0'),
     'http://example.org/battery-passport#SafetyShape',
     NULL,
     'http://example.org/battery-passport#',
     'http://example.org/battery-passport'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Battery-Passport-Vocabulary' AND template_version = '1.0.0'),
     'http://example.org/battery-passport#DataSourceShape',
     NULL,
     'http://example.org/battery-passport#',
     'http://example.org/battery-passport'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Battery-Passport-Vocabulary' AND template_version = '1.0.0'),
     'http://example.org/battery-passport#CompositionShape',
     NULL,
     'http://example.org/battery-passport#',
     'http://example.org/battery-passport'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Battery-Passport-Vocabulary' AND template_version = '1.0.0'),
     'http://example.org/battery-passport#CathodeActiveMaterialShape',
     NULL,
     'http://example.org/battery-passport#',
     'http://example.org/battery-passport');

-- third shacl insert
-- third shacl insert
INSERT INTO shacl_templates (template_name, description, template_version, context_uri, shacl_content)
VALUES ('Electronics-DPP-MinimalTargets',
        'Digital Product Passport for electronics - Minimal targetClass strategy',
        '1.0.0',
        'http://example.org/electronics-dpp',
        '@prefix sh: <http://www.w3.org/ns/shacl#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix edpp: <http://example.org/electronics-dpp#> .
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .

# STRATEGY 3: MINIMAL TARGET CLASSES
# Solo la shape principale ha targetClass
# Tutte le altre shape dipendono da matching basato su properties (SIMILARITY_MATCH)
# Questo forza il sistema a usare vocabulary_uri e required_paths

# Ontology declaration
<http://example.org/electronics-dpp> a owl:Ontology ;
    owl:imports <http://example.org/electronics-dpp> .

# Main Electronics DPP Shape - UNICA con targetClass
edpp:ElectronicsDPPShape
    a sh:NodeShape ;
    sh:targetClass edpp:ElectronicsDPP ;
    sh:property [
        sh:path edpp:deviceIdentification ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:node edpp:DeviceIdentificationShape ;
    ] ;
    sh:property [
        sh:path edpp:deviceSpecifications ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:node edpp:DeviceSpecificationsShape ;
    ] ;
    sh:property [
        sh:path edpp:componentDetails ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:node edpp:ComponentDetailsShape ;
    ] ;
    sh:property [
        sh:path edpp:maintenanceGuidelines ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:node edpp:MaintenanceGuidelinesShape ;
    ] ;
    sh:property [
        sh:path edpp:environmentalImpact ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:node edpp:EnvironmentalImpactShape ;
    ] ;
    sh:property [
        sh:path edpp:endOfLife ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:node edpp:EndOfLifeShape ;
    ] ;
    sh:property [
        sh:path edpp:regulations ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:node edpp:RegulationsShape ;
    ] ;
    sh:property [
        sh:path edpp:supplyChain ;
        sh:maxCount 1 ;
        sh:node edpp:SupplyChainShape ;
    ] ;
    # oneOf constraint: exactly one variant must be present
    sh:xone (
        edpp:ConsumerVariantShape
        edpp:IndustrialVariantShape
        edpp:ProfessionalVariantShape
    ) .

# Device Identification Shape - NO targetClass
edpp:DeviceIdentificationShape
    a sh:NodeShape ;
    sh:property [
        sh:path edpp:serialNumber ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:pattern "^[A-Z0-9]{10,20}$" ;
        sh:description "Unique device serial number" ;
    ] ;
    sh:property [
        sh:path edpp:modelName ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
    ] ;
    sh:property [
        sh:path edpp:manufacturer ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:node edpp:ManufacturerShape ;
    ] .

# Manufacturer Shape - NO targetClass
edpp:ManufacturerShape
    a sh:NodeShape ;
    sh:property [
        sh:path edpp:companyName ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
    ] ;
    sh:property [
        sh:path edpp:headquarters ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:pattern "^[A-Z]{2}$" ;
        sh:description "ISO 3166-1 alpha-2 country code" ;
    ] ;
    sh:property [
        sh:path edpp:facility ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
    ] .

# Device Specifications Shape - NO targetClass
edpp:DeviceSpecificationsShape
    a sh:NodeShape ;
    sh:property [
        sh:path edpp:deviceCategory ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:in ("consumer" "industrial" "professional") ;
    ] ;
    sh:property [
        sh:path edpp:weight ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
        sh:description "Weight in kilograms" ;
    ] ;
    sh:property [
        sh:path edpp:dimensions ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:description "Dimensions in format WxHxD cm" ;
    ] ;
    sh:property [
        sh:path edpp:releaseYear ;
        sh:maxCount 1 ;
        sh:datatype xsd:integer ;
    ] .

# Component Details Shape - NO targetClass
edpp:ComponentDetailsShape
    a sh:NodeShape ;
    sh:property [
        sh:path edpp:mainComponents ;
        sh:minCount 1 ;
        sh:node edpp:ComponentShape ;
    ] ;
    sh:property [
        sh:path edpp:primaryMaterial ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:in ("plastic" "metal" "composite") ;
        sh:description "Dominant material composition" ;
    ] ;
    sh:property [
        sh:path edpp:plasticPercentage ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
    ] ;
    sh:property [
        sh:path edpp:metalPercentage ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
    ] .

# Component Shape - NO targetClass
edpp:ComponentShape
    a sh:NodeShape ;
    sh:property [
        sh:path edpp:componentName ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:description "Name of component (e.g., battery, display, processor)" ;
    ] ;
    sh:property [
        sh:path edpp:quantity ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:integer ;
        sh:minInclusive 1 ;
    ] .

# Maintenance Guidelines Shape - NO targetClass
edpp:MaintenanceGuidelinesShape
    a sh:NodeShape ;
    sh:property [
        sh:path edpp:cleaning ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:description "Cleaning procedures and recommendations" ;
    ] ;
    sh:property [
        sh:path edpp:storage ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:description "Storage conditions and requirements" ;
    ] ;
    sh:property [
        sh:path edpp:handling ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:description "Safe handling instructions" ;
    ] ;
    sh:property [
        sh:path edpp:batteryMaintenance ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
    ] ;
    sh:property [
        sh:path edpp:softwareUpdates ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
    ] .

# Environmental Impact Shape - NO targetClass
edpp:EnvironmentalImpactShape
    a sh:NodeShape ;
    sh:property [
        sh:path edpp:waterUsage ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
        sh:description "Water usage in liters during production" ;
    ] ;
    sh:property [
        sh:path edpp:powerConsumption ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
        sh:description "Power consumption in watts during operation" ;
    ] ;
    sh:property [
        sh:path edpp:hazardousMaterials ;
        sh:minCount 1 ;
        sh:description "List of hazardous materials present" ;
    ] ;
    sh:property [
        sh:path edpp:carbonEmissions ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
        sh:description "Carbon emissions in kg CO2 eq during lifecycle" ;
    ] ;
    sh:property [
        sh:path edpp:energyStarCertified ;
        sh:maxCount 1 ;
        sh:datatype xsd:boolean ;
    ] ;
    sh:property [
        sh:path edpp:conflictMinerals ;
        sh:maxCount 1 ;
    ] .

# End of Life Shape - NO targetClass
edpp:EndOfLifeShape
    a sh:NodeShape ;
    sh:property [
        sh:path edpp:repairability ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:in ("excellent" "good" "fair" "poor") ;
    ] ;
    sh:property [
        sh:path edpp:reuseContent ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
        sh:minInclusive 0 ;
        sh:maxInclusive 100 ;
        sh:description "Percentage of reused materials" ;
    ] ;
    sh:property [
        sh:path edpp:expectedLifespan ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
        sh:description "Expected lifespan in years" ;
    ] ;
    sh:property [
        sh:path edpp:upgradeability ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
    ] .

# Regulations Shape - NO targetClass
edpp:RegulationsShape
    a sh:NodeShape ;
    sh:property [
        sh:path edpp:rohs ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:boolean ;
        sh:description "RoHS compliance (restriction of hazardous substances)" ;
    ] ;
    sh:property [
        sh:path edpp:weee ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:boolean ;
        sh:description "WEEE directive compliance" ;
    ] .

# Supply Chain Shape - NO targetClass
edpp:SupplyChainShape
    a sh:NodeShape ;
    sh:property [
        sh:path edpp:battery ;
        sh:maxCount 1 ;
        sh:node edpp:BatterySupplyChainShape ;
    ] .

# Battery Supply Chain Shape - NO targetClass
edpp:BatterySupplyChainShape
    a sh:NodeShape ;
    sh:property [
        sh:path edpp:source ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
    ] ;
    sh:property [
        sh:path edpp:certifications ;
        sh:maxCount 1 ;
    ] .

# Consumer Variant Shape - NO targetClass
edpp:ConsumerVariantShape
    a sh:NodeShape ;
    sh:property [
        sh:path edpp:deviceSpecifications ;
        sh:node [
            sh:property [
                sh:path edpp:deviceCategory ;
                sh:hasValue "consumer" ;
            ]
        ]
    ] ;
    sh:property [
        sh:path edpp:consumerFeatures ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:node edpp:ConsumerFeaturesShape ;
    ] ;
    sh:property [
        sh:path edpp:environmentalImpact ;
        sh:node [
            sh:property [
                sh:path edpp:batteryCapacity ;
                sh:maxCount 1 ;
                sh:datatype xsd:double ;
            ] ;
            sh:property [
                sh:path edpp:standbyPower ;
                sh:maxCount 1 ;
                sh:datatype xsd:double ;
            ]
        ]
    ] .

# Consumer Features Shape - NO targetClass
edpp:ConsumerFeaturesShape
    a sh:NodeShape ;
    sh:property [
        sh:path edpp:warrantyPeriod ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:integer ;
        sh:description "Warranty period in months" ;
    ] ;
    sh:property [
        sh:path edpp:userManualLanguages ;
        sh:minCount 1 ;
    ] ;
    sh:property [
        sh:path edpp:customerSupport ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
    ] ;
    sh:property [
        sh:path edpp:ecoLabel ;
        sh:maxCount 1 ;
        sh:datatype xsd:boolean ;
    ] ;
    sh:property [
        sh:path edpp:energyRating ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
    ] .

# Industrial Variant Shape - NO targetClass
edpp:IndustrialVariantShape
    a sh:NodeShape ;
    sh:property [
        sh:path edpp:deviceSpecifications ;
        sh:node [
            sh:property [
                sh:path edpp:deviceCategory ;
                sh:hasValue "industrial" ;
            ]
        ]
    ] ;
    sh:property [
        sh:path edpp:industrialFeatures ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:node edpp:IndustrialFeaturesShape ;
    ] ;
    sh:property [
        sh:path edpp:environmentalImpact ;
        sh:node [
            sh:property [
                sh:path edpp:electronicWasteGeneration ;
                sh:maxCount 1 ;
                sh:datatype xsd:double ;
            ] ;
            sh:property [
                sh:path edpp:toxicSubstances ;
                sh:maxCount 1 ;
            ]
        ]
    ] .

# Industrial Features Shape - NO targetClass
edpp:IndustrialFeaturesShape
    a sh:NodeShape ;
    sh:property [
        sh:path edpp:operatingTemperature ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:description "Operating temperature range" ;
    ] ;
    sh:property [
        sh:path edpp:certifications ;
        sh:minCount 1 ;
        sh:description "Industrial certifications" ;
    ] ;
    sh:property [
        sh:path edpp:maintenanceSchedule ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
    ] ;
    sh:property [
        sh:path edpp:dutyCycle ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
    ] ;
    sh:property [
        sh:path edpp:meanTimeBetweenFailures ;
        sh:maxCount 1 ;
        sh:datatype xsd:double ;
    ] .

# Professional Variant Shape - NO targetClass
edpp:ProfessionalVariantShape
    a sh:NodeShape ;
    sh:property [
        sh:path edpp:deviceSpecifications ;
        sh:node [
            sh:property [
                sh:path edpp:deviceCategory ;
                sh:hasValue "professional" ;
            ]
        ]
    ] ;
    sh:property [
        sh:path edpp:professionalFeatures ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:node edpp:ProfessionalFeaturesShape ;
    ] ;
    sh:property [
        sh:path edpp:endOfLife ;
        sh:node [
            sh:property [
                sh:path edpp:disassemblyEase ;
                sh:maxCount 1 ;
                sh:datatype xsd:boolean ;
            ] ;
            sh:property [
                sh:path edpp:componentRecovery ;
                sh:maxCount 1 ;
                sh:datatype xsd:boolean ;
            ] ;
            sh:property [
                sh:path edpp:refurbishmentPotential ;
                sh:maxCount 1 ;
                sh:datatype xsd:string ;
            ]
        ]
    ] ;
    sh:property [
        sh:path edpp:environmentalImpact ;
        sh:node [
            sh:property [
                sh:path edpp:manufacturingEmissions ;
                sh:maxCount 1 ;
                sh:datatype xsd:double ;
            ]
        ]
    ] .

# Professional Features Shape - NO targetClass
edpp:ProfessionalFeaturesShape
    a sh:NodeShape ;
    sh:property [
        sh:path edpp:technicalSupport ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
        sh:description "Technical support availability" ;
    ] ;
    sh:property [
        sh:path edpp:calibrationRequired ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
        sh:datatype xsd:boolean ;
    ] ;
    sh:property [
        sh:path edpp:performanceMetrics ;
        sh:minCount 1 ;
        sh:maxCount 1 ;
    ] ;
    sh:property [
        sh:path edpp:softwareCompatibility ;
        sh:maxCount 1 ;
    ] ;
    sh:property [
        sh:path edpp:networkCapabilities ;
        sh:maxCount 1 ;
        sh:datatype xsd:string ;
    ] .
');

INSERT INTO shacl_shapes (template_id, shape_id, target_class, vocabulary_uri, ontology_uri)
VALUES
    ((SELECT id FROM shacl_templates WHERE template_name = 'Electronics-DPP-MinimalTargets' AND template_version = '1.0.0'),
     'http://example.org/electronics-dpp#ElectronicsDPPShape',
     'http://example.org/electronics-dpp#ElectronicsDPP',
     'http://example.org/electronics-dpp#',
     'http://example.org/electronics-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Electronics-DPP-MinimalTargets' AND template_version = '1.0.0'),
     'http://example.org/electronics-dpp#DeviceIdentificationShape',
     NULL,
     'http://example.org/electronics-dpp#',
     'http://example.org/electronics-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Electronics-DPP-MinimalTargets' AND template_version = '1.0.0'),
     'http://example.org/electronics-dpp#ManufacturerShape',
     NULL,
     'http://example.org/electronics-dpp#',
     'http://example.org/electronics-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Electronics-DPP-MinimalTargets' AND template_version = '1.0.0'),
     'http://example.org/electronics-dpp#DeviceSpecificationsShape',
     NULL,
     'http://example.org/electronics-dpp#',
     'http://example.org/electronics-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Electronics-DPP-MinimalTargets' AND template_version = '1.0.0'),
     'http://example.org/electronics-dpp#ComponentDetailsShape',
     NULL,
     'http://example.org/electronics-dpp#',
     'http://example.org/electronics-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Electronics-DPP-MinimalTargets' AND template_version = '1.0.0'),
     'http://example.org/electronics-dpp#MainComponentShape',
     NULL,
     'http://example.org/electronics-dpp#',
     'http://example.org/electronics-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Electronics-DPP-MinimalTargets' AND template_version = '1.0.0'),
     'http://example.org/electronics-dpp#MaintenanceGuidelinesShape',
     NULL,
     'http://example.org/electronics-dpp#',
     'http://example.org/electronics-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Electronics-DPP-MinimalTargets' AND template_version = '1.0.0'),
     'http://example.org/electronics-dpp#EnvironmentalImpactShape',
     NULL,
     'http://example.org/electronics-dpp#',
     'http://example.org/electronics-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Electronics-DPP-MinimalTargets' AND template_version = '1.0.0'),
     'http://example.org/electronics-dpp#EndOfLifeShape',
     NULL,
     'http://example.org/electronics-dpp#',
     'http://example.org/electronics-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Electronics-DPP-MinimalTargets' AND template_version = '1.0.0'),
     'http://example.org/electronics-dpp#RegulationsShape',
     NULL,
     'http://example.org/electronics-dpp#',
     'http://example.org/electronics-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Electronics-DPP-MinimalTargets' AND template_version = '1.0.0'),
     'http://example.org/electronics-dpp#SupplyChainShape',
     NULL,
     'http://example.org/electronics-dpp#',
     'http://example.org/electronics-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Electronics-DPP-MinimalTargets' AND template_version = '1.0.0'),
     'http://example.org/electronics-dpp#BatteryShape',
     NULL,
     'http://example.org/electronics-dpp#',
     'http://example.org/electronics-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Electronics-DPP-MinimalTargets' AND template_version = '1.0.0'),
     'http://example.org/electronics-dpp#ConsumerFeaturesShape',
     NULL,
     'http://example.org/electronics-dpp#',
     'http://example.org/electronics-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Electronics-DPP-MinimalTargets' AND template_version = '1.0.0'),
     'http://example.org/electronics-dpp#IndustrialFeaturesShape',
     NULL,
     'http://example.org/electronics-dpp#',
     'http://example.org/electronics-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Electronics-DPP-MinimalTargets' AND template_version = '1.0.0'),
     'http://example.org/electronics-dpp#ProfessionalFeaturesShape',
     NULL,
     'http://example.org/electronics-dpp#',
     'http://example.org/electronics-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Electronics-DPP-MinimalTargets' AND template_version = '1.0.0'),
     'http://example.org/electronics-dpp#ConsumerEnvironmentalImpactShape',
     NULL,
     'http://example.org/electronics-dpp#',
     'http://example.org/electronics-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Electronics-DPP-MinimalTargets' AND template_version = '1.0.0'),
     'http://example.org/electronics-dpp#IndustrialEnvironmentalImpactShape',
     NULL,
     'http://example.org/electronics-dpp#',
     'http://example.org/electronics-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Electronics-DPP-MinimalTargets' AND template_version = '1.0.0'),
     'http://example.org/electronics-dpp#ProfessionalEnvironmentalImpactShape',
     NULL,
     'http://example.org/electronics-dpp#',
     'http://example.org/electronics-dpp'),
    ((SELECT id FROM shacl_templates WHERE template_name = 'Electronics-DPP-MinimalTargets' AND template_version = '1.0.0'),
     'http://example.org/electronics-dpp#ProfessionalEndOfLifeShape',
     NULL,
     'http://example.org/electronics-dpp#',
     'http://example.org/electronics-dpp');