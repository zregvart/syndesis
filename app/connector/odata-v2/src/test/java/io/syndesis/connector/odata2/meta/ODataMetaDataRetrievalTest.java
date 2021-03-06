/*
 * Copyright (C) 2016 Red Hat, Inc.
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
package io.syndesis.connector.odata2.meta;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatTypes;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ArraySchema;
import com.fasterxml.jackson.module.jsonSchema.types.ContainerTypeSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;
import io.syndesis.common.model.DataShape;
import io.syndesis.common.model.DataShapeKinds;
import io.syndesis.common.util.json.JsonUtils;
import io.syndesis.connector.odata2.AbstractODataTest;
import io.syndesis.connector.odata2.server.Certificates;
import io.syndesis.connector.support.verifier.api.MetadataRetrieval;
import io.syndesis.connector.support.verifier.api.PropertyPair;
import io.syndesis.connector.support.verifier.api.SyndesisMetadata;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.FactoryFinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ODataMetaDataRetrievalTest extends AbstractODataTest {

    @BeforeEach
    public void setup() throws Exception {
        context = new DefaultCamelContext();
        context.disableJMX();
        context.start();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (context != null) {
            context.stop();
            context = null;
        }
    }

    private static Map<String, JsonSchema> checkShape(DataShape dataShape, Class<? extends ContainerTypeSchema> expectedShapeClass) throws IOException, JsonParseException, JsonMappingException {
        assertNotNull(dataShape);

        assertEquals(DataShapeKinds.JSON_SCHEMA, dataShape.getKind());
        assertNotNull(dataShape.getSpecification());

        ContainerTypeSchema schema = JsonUtils.copyObjectMapperConfiguration().readValue(
                                            dataShape.getSpecification(), expectedShapeClass);

        Map<String, JsonSchema> propSchemaMap = null;
        if (schema instanceof ArraySchema) {
            propSchemaMap = ((ArraySchema) schema).getItems().asSingleItems().getSchema().asObjectSchema().getProperties();
        } else if (schema instanceof ObjectSchema) {
            propSchemaMap = ((ObjectSchema) schema).getProperties();
        }

        assertNotNull(propSchemaMap);
        return propSchemaMap;
    }

    private static void checkTestServerSchemaMap(Map<String, JsonSchema> schemaMap) {
        JsonSchema idSchema = schemaMap.get("Id");
        JsonSchema nameSchema = schemaMap.get("Name");
        JsonSchema foundedSchema = schemaMap.get("Founded");
        JsonSchema addressSchema = schemaMap.get("Address");

        assertNotNull(idSchema);
        assertNotNull(nameSchema);
        assertNotNull(foundedSchema);
        assertNotNull(addressSchema);

        JsonFormatTypes idType = idSchema.getType();
        assertNotNull(idType);
        assertEquals(JsonFormatTypes.STRING, idType);
        assertEquals(false, idSchema.getRequired());

        JsonFormatTypes addressType = addressSchema.getType();
        assertNotNull(addressType);
        assertEquals(JsonFormatTypes.OBJECT, addressType);
        assertEquals(false, addressSchema.getRequired());
        assertThat(addressSchema).isInstanceOf(ObjectSchema.class);
        ObjectSchema specObjSchema = addressSchema.asObjectSchema();
        assertEquals(4, specObjSchema.getProperties().size());
    }

    @Test
    public void testFindingAdapter() throws Exception {
        String resourcePath = "META-INF/syndesis/connector/meta/";
        String connectorId = "odata-v2";
        CamelContext context = new DefaultCamelContext();

        FactoryFinder finder = context.getFactoryFinder(resourcePath);
        assertThat(finder).isNotNull();

        Class<?> type = finder.findClass(connectorId);
        assertThat(type).isEqualTo(ODataMetaDataRetrieval.class);

        MetadataRetrieval adapter = (MetadataRetrieval) context.getInjector().newInstance(type);
        assertThat(adapter).isNotNull();
    }

    @Test
    public void testReadFromMetaDataRetrieval() throws Exception {
        CamelContext context = new DefaultCamelContext();
        ODataMetaDataRetrieval retrieval = new ODataMetaDataRetrieval();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(SERVICE_URI, odataTestServer.getServiceUri());
        parameters.put(RESOURCE_PATH, MANUFACTURERS);

        String componentId = "odata-v2";
        String actionId = "io.syndesis:" + Methods.READ.actionIdentifierRoot() + HYPHEN + FROM;

        SyndesisMetadata metadata = retrieval.fetch(context, componentId, actionId, parameters);
        assertNotNull(metadata);

        Map<String, List<PropertyPair>> properties = metadata.getProperties();
        assertFalse(properties.isEmpty());

        //
        // The method names are important for collecting prior
        // to the filling in of the integration step (values such as resource etc...)
        //
        List<PropertyPair> resourcePaths = properties.get(RESOURCE_PATH);
        assertNotNull(resourcePaths);
        assertFalse(resourcePaths.isEmpty());

        assertThat(resourcePaths).hasSize(3);
        assertThat(resourcePaths).anyMatch(p -> MANUFACTURERS.equals(p.getValue()));
        assertThat(resourcePaths).anyMatch(p -> CARS.equals(p.getValue()));
        assertThat(resourcePaths).anyMatch(p -> DRIVERS.equals(p.getValue()));

        //
        // The out data shape is defined after the integration step has
        // been populated and should be a dynamic json-schema based
        // on the contents of the OData Edm metadata object.
        //
        checkTestServerSchemaMap(checkShape(metadata.outputShape, ArraySchema.class));
    }

    @Test
    public void testReadFromMetaDataRetrievalWithSplit() throws Exception {
        CamelContext context = new DefaultCamelContext();
        ODataMetaDataRetrieval retrieval = new ODataMetaDataRetrieval();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(SERVICE_URI, odataTestServer.getServiceUri());
        parameters.put(RESOURCE_PATH, MANUFACTURERS);
        parameters.put(SPLIT_RESULT, true);

        String componentId = "odata-v2";
        String actionId = "io.syndesis:" + Methods.READ.actionIdentifierRoot() + HYPHEN + FROM;

        SyndesisMetadata metadata = retrieval.fetch(context, componentId, actionId, parameters);
        assertNotNull(metadata);

        Map<String, List<PropertyPair>> properties = metadata.getProperties();
        assertFalse(properties.isEmpty());

        //
        // The method names are important for collecting prior
        // to the filling in of the integration step (values such as resource etc...)
        //
        List<PropertyPair> resourcePaths = properties.get(RESOURCE_PATH);
        assertNotNull(resourcePaths);
        assertFalse(resourcePaths.isEmpty());

        assertThat(resourcePaths).hasSize(3);
        assertThat(resourcePaths).anyMatch(p -> MANUFACTURERS.equals(p.getValue()));
        assertThat(resourcePaths).anyMatch(p -> CARS.equals(p.getValue()));
        assertThat(resourcePaths).anyMatch(p -> DRIVERS.equals(p.getValue()));

        //
        // The out data shape is defined after the integration step has
        // been populated and should be a dynamic json-schema based
        // on the contents of the OData Edm metadata object.
        //
        // Split causes it to be an ObjectSchema rather than an ArraySchema
        //
        checkTestServerSchemaMap(checkShape(metadata.outputShape, ObjectSchema.class));
    }

    @Test
    public void testReadFromMetaDataRetrievalWithKeyPredicate() throws Exception {
        CamelContext context = new DefaultCamelContext();
        ODataMetaDataRetrieval retrieval = new ODataMetaDataRetrieval();

        String keyPredicate = "'1'";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(SERVICE_URI, odataTestServer.getServiceUri());
        parameters.put(RESOURCE_PATH, MANUFACTURERS);
        parameters.put(SPLIT_RESULT, false);
        parameters.put(KEY_PREDICATE, keyPredicate);

        String componentId = "odata-v2";
        String actionId = "io.syndesis:" + Methods.READ.actionIdentifierRoot() + HYPHEN + FROM;

        SyndesisMetadata metadata = retrieval.fetch(context, componentId, actionId, parameters);
        assertNotNull(metadata);

        Map<String, List<PropertyPair>> properties = metadata.getProperties();
        assertFalse(properties.isEmpty());

        //
        // The method names are important for collecting prior
        // to the filling in of the integration step (values such as resource etc...)
        //
        List<PropertyPair> resourcePaths = properties.get(RESOURCE_PATH);
        assertNotNull(resourcePaths);
        assertFalse(resourcePaths.isEmpty());

        assertThat(resourcePaths).hasSize(3);
        assertThat(resourcePaths).anyMatch(p -> MANUFACTURERS.equals(p.getValue()));
        assertThat(resourcePaths).anyMatch(p -> CARS.equals(p.getValue()));
        assertThat(resourcePaths).anyMatch(p -> DRIVERS.equals(p.getValue()));

        //
        // The out data shape is defined after the integration step has
        // been populated and should be a dynamic json-schema based
        // on the contents of the OData Edm metadata object.
        //
        checkTestServerSchemaMap(checkShape(metadata.outputShape, ObjectSchema.class));
    }

    @Test
    public void testCreateMetaDataRetrieval() throws Exception {
        CamelContext context = new DefaultCamelContext();
        ODataMetaDataRetrieval retrieval = new ODataMetaDataRetrieval();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(SERVICE_URI, odataTestServer.getServiceUri());
        parameters.put(RESOURCE_PATH, MANUFACTURERS);

        String componentId = "odata-v2";
        String actionId = "io.syndesis:" + Methods.CREATE.actionIdentifierRoot();

        SyndesisMetadata metadata = retrieval.fetch(context, componentId, actionId, parameters);
        assertNotNull(metadata);

        Map<String, List<PropertyPair>> properties = metadata.getProperties();
        assertFalse(properties.isEmpty());

        //
        // The method names are important for collecting prior
        // to the filling in of the integration step (values such as resource etc...)
        //
        List<PropertyPair> resourcePaths = properties.get(RESOURCE_PATH);
        assertNotNull(resourcePaths);
        assertFalse(resourcePaths.isEmpty());

        assertThat(resourcePaths).hasSize(3);
        assertThat(resourcePaths).anyMatch(p -> MANUFACTURERS.equals(p.getValue()));
        assertThat(resourcePaths).anyMatch(p -> CARS.equals(p.getValue()));
        assertThat(resourcePaths).anyMatch(p -> DRIVERS.equals(p.getValue()));

        //
        // Both data shapes are defined after the integration step has
        // been populated and should be dynamic json-schema based
        // on the contents of the OData Edm metadata object.
        //
        checkTestServerSchemaMap(checkShape(metadata.inputShape, ObjectSchema.class));
        checkTestServerSchemaMap(checkShape(metadata.outputShape, ObjectSchema.class));
    }

    @Test
    public void testDeleteMetaDataRetrieval() throws Exception {
        CamelContext context = new DefaultCamelContext();
        ODataMetaDataRetrieval retrieval = new ODataMetaDataRetrieval();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(SERVICE_URI, odataTestServer.getServiceUri());
        parameters.put(RESOURCE_PATH, MANUFACTURERS);

        String componentId = "odata-v2";
        String actionId = "io.syndesis:" + Methods.DELETE.actionIdentifierRoot();

        SyndesisMetadata metadata = retrieval.fetch(context, componentId, actionId, parameters);
        assertNotNull(metadata);

        Map<String, List<PropertyPair>> properties = metadata.getProperties();
        assertFalse(properties.isEmpty());

        //
        // The method names are important for collecting prior
        // to the filling in of the integration step (values such as resource etc...)
        //
        List<PropertyPair> resourcePaths = properties.get(RESOURCE_PATH);
        assertNotNull(resourcePaths);
        assertFalse(resourcePaths.isEmpty());

        assertThat(resourcePaths).hasSize(3);
        assertThat(resourcePaths).anyMatch(p -> MANUFACTURERS.equals(p.getValue()));
        assertThat(resourcePaths).anyMatch(p -> CARS.equals(p.getValue()));
        assertThat(resourcePaths).anyMatch(p -> DRIVERS.equals(p.getValue()));

        DataShape inputShape = metadata.inputShape;
        Map<String, JsonSchema> schemaMap = checkShape(inputShape, ObjectSchema.class);
        assertNotNull(schemaMap.get(KEY_PREDICATE));

        DataShape outputShape = metadata.outputShape;
        assertEquals(DataShapeKinds.JSON_INSTANCE, outputShape.getKind());
    }

    @Test
    public void testUpdateMetaDataRetrieval() throws Exception {
        CamelContext context = new DefaultCamelContext();
        ODataMetaDataRetrieval retrieval = new ODataMetaDataRetrieval();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(SERVICE_URI, odataTestServer.getServiceUri());
        parameters.put(RESOURCE_PATH, MANUFACTURERS);

        String componentId = "odata-v2";
        String actionId = "io.syndesis:" + Methods.MERGE.actionIdentifierRoot();

        SyndesisMetadata metadata = retrieval.fetch(context, componentId, actionId, parameters);
        assertNotNull(metadata);

        Map<String, List<PropertyPair>> properties = metadata.getProperties();
        assertFalse(properties.isEmpty());

        //
        // The method names are important for collecting prior
        // to the filling in of the integration step (values such as resource etc...)
        //
        List<PropertyPair> resourcePaths = properties.get(RESOURCE_PATH);
        assertNotNull(resourcePaths);
        assertFalse(resourcePaths.isEmpty());

        assertThat(resourcePaths).hasSize(3);
        assertThat(resourcePaths).anyMatch(p -> MANUFACTURERS.equals(p.getValue()));
        assertThat(resourcePaths).anyMatch(p -> CARS.equals(p.getValue()));
        assertThat(resourcePaths).anyMatch(p -> DRIVERS.equals(p.getValue()));

        DataShape inputShape = metadata.inputShape;
        Map<String, JsonSchema> schemaMap = checkShape(inputShape, ObjectSchema.class);
        checkTestServerSchemaMap(schemaMap);
        assertNotNull(schemaMap.get(KEY_PREDICATE));

        DataShape outputShape = metadata.outputShape;
        assertEquals(DataShapeKinds.JSON_INSTANCE, outputShape.getKind());
    }

    /**
     * Needs to supply server certificate since the server is unknown to the default
     * certificate authorities that is loaded into the keystore by default
     */
    @Test
    public void testReadFromMetaDataRetrievalSSL() throws Exception {
        CamelContext context = new DefaultCamelContext();
        ODataMetaDataRetrieval retrieval = new ODataMetaDataRetrieval();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(SERVICE_URI, sslTestServer.getSecuredServiceUri());
        parameters.put(RESOURCE_PATH, MANUFACTURERS);
        // Provide the server's SSL certificate to allow client handshake
        parameters.put(SERVER_CERTIFICATE, Certificates.TEST_SERVICE.get());

        String componentId = "odata-v2";
        String actionId = "io.syndesis:" + Methods.READ.actionIdentifierRoot() + HYPHEN + FROM;

        SyndesisMetadata metadata = retrieval.fetch(context, componentId, actionId, parameters);
        assertNotNull(metadata);

        Map<String, List<PropertyPair>> properties = metadata.getProperties();
        assertFalse(properties.isEmpty());

        //
        // The method names are important for collecting prior
        // to the filling in of the integration step (values such as resource etc...)
        //
        List<PropertyPair> resourcePaths = properties.get(RESOURCE_PATH);
        assertNotNull(resourcePaths);
        assertFalse(resourcePaths.isEmpty());

        assertThat(resourcePaths).hasSize(3);
        assertThat(resourcePaths).anyMatch(p -> MANUFACTURERS.equals(p.getValue()));
        assertThat(resourcePaths).anyMatch(p -> CARS.equals(p.getValue()));
        assertThat(resourcePaths).anyMatch(p -> DRIVERS.equals(p.getValue()));

        //
        // The out data shape is defined after the integration step has
        // been populated and should be a dynamic json-schema based
        // on the contents of the OData Edm metadata object.
        //
        Map<String, JsonSchema> schemaMap = checkShape(metadata.outputShape, ArraySchema.class);
        checkTestServerSchemaMap(schemaMap);
    }

    @Test
    public void testReadFromMetaDataRetrievalReferenceServerSSL() throws Exception {
        CamelContext context = new DefaultCamelContext();
        ODataMetaDataRetrieval retrieval = new ODataMetaDataRetrieval();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(SERVICE_URI, REF_SERVICE_URI);
        parameters.put(RESOURCE_PATH, SUPPLIERS);

        String componentId = "odata-v2";
        String actionId = "io.syndesis:" + Methods.READ.actionIdentifierRoot() + HYPHEN + FROM;

        SyndesisMetadata metadata = retrieval.fetch(context, componentId, actionId, parameters);
        assertNotNull(metadata);

        Map<String, List<PropertyPair>> properties = metadata.getProperties();
        assertFalse(properties.isEmpty());

        //
        // The method names are important for collecting prior
        // to the filling in of the integration step (values such as resource etc...)
        //
        List<PropertyPair> resourcePaths = properties.get(RESOURCE_PATH);
        assertNotNull(resourcePaths);
        assertFalse(resourcePaths.isEmpty());

        assertThat(resourcePaths).hasSize(3);
        assertThat(resourcePaths).anyMatch(p -> SUPPLIERS.equals(p.getValue()));
        assertThat(resourcePaths).anyMatch(p -> PRODUCTS.equals(p.getValue()));
        assertThat(resourcePaths).anyMatch(p -> CATEGORIES.equals(p.getValue()));

        //
        // The out data shape is defined after the integration step has
        // been populated and should be a dynamic json-schema based
        // on the contents of the OData Edm metadata object.
        //
        Map<String, JsonSchema> schemaMap = checkShape(metadata.outputShape, ArraySchema.class);
        assertNotNull(schemaMap.get("Name"));
        assertNotNull(schemaMap.get("Address"));
    }

    @Test
    public void testReadToMetaDataRetrieval() throws Exception {
        CamelContext context = new DefaultCamelContext();
        ODataMetaDataRetrieval retrieval = new ODataMetaDataRetrieval();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(SERVICE_URI, odataTestServer.getServiceUri());
        parameters.put(RESOURCE_PATH, MANUFACTURERS);

        String componentId = "odata-v2";
        String actionId = "io.syndesis:" + Methods.READ.actionIdentifierRoot() + HYPHEN + TO;

        SyndesisMetadata metadata = retrieval.fetch(context, componentId, actionId, parameters);
        assertNotNull(metadata);

        Map<String, List<PropertyPair>> properties = metadata.getProperties();
        assertFalse(properties.isEmpty());

        //
        // The method names are important for collecting prior
        // to the filling in of the integration step (values such as resource etc...)
        //
        List<PropertyPair> resourcePaths = properties.get(RESOURCE_PATH);
        assertNotNull(resourcePaths);
        assertFalse(resourcePaths.isEmpty());

        assertThat(resourcePaths).hasSize(3);
        assertThat(resourcePaths).anyMatch(p -> MANUFACTURERS.equals(p.getValue()));
        assertThat(resourcePaths).anyMatch(p -> CARS.equals(p.getValue()));
        assertThat(resourcePaths).anyMatch(p -> DRIVERS.equals(p.getValue()));

        DataShape inputShape = metadata.inputShape;
        Map<String, JsonSchema> schemaMap = checkShape(inputShape, ObjectSchema.class);
        assertNotNull(schemaMap.get(KEY_PREDICATE));

        DataShape outputShape = metadata.outputShape;
        assertEquals(DataShapeKinds.JSON_SCHEMA, outputShape.getKind());
    }
}
