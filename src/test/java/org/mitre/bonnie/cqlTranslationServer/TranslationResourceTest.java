package org.mitre.bonnie.cqlTranslationServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class TranslationResourceTest {

  private HttpServer server;
  private WebTarget target;

  @Before
  public void setUp() throws Exception {
    // start the server
    server = Main.startServer();

    // create the client
    Client c = ClientBuilder.newBuilder().register(MultiPartFeature.class).build();

    // uncomment the following line if you want to enable
    // support for JSON in the client (you also have to uncomment
    // dependency on jersey-media-json module in pom.xml and Main.startServer())
    // --
    // c.configuration().enable(new org.glassfish.jersey.media.json.JsonJaxbFeature());
    target = c.target(Main.BASE_URI.replace("0.0.0.0", "localhost"));
  }

  @After
  public void tearDown() throws Exception {
    server.shutdownNow();
  }

  private static DocumentBuilder newDocumentBuilder() throws ParserConfigurationException, SAXException {
    DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
    domFactory.setNamespaceAware(true);
    return domFactory.newDocumentBuilder();
  }

  private static Document parseXml(File f) throws ParserConfigurationException, SAXException, IOException {
    return newDocumentBuilder().parse(f);
  }

  private static Document parseXml(String s) throws ParserConfigurationException, SAXException, IOException {
    InputSource source = new InputSource(new StringReader(s));
    return newDocumentBuilder().parse(source);
  }

  private static String applyXPath(Document doc, String xPathStr) throws XPathExpressionException {
    XPath xpath = XPathFactory.newInstance().newXPath();
    xpath.setNamespaceContext(new ElmNamespaceContext());
    XPathExpression expr = xpath.compile(xPathStr);
    Object result = expr.evaluate(doc, XPathConstants.STRING);
    return (String) result;
  }

  private void validateListPromotionDisabled(String cqlTitle, int startLine, int startChar, String errorMessage) {
    File file = new File(TranslationResourceTest.class.getResource(cqlTitle).getFile());
    FormDataMultiPart pkg = new FormDataMultiPart();
    pkg.field("test", file, new MediaType("application", "cql"));
    Response resp = target.path("translator").queryParam("disable-list-promotion", "true").request(MediaType.MULTIPART_FORM_DATA).post(Entity.entity(pkg, MediaType.MULTIPART_FORM_DATA));
    assertTrue(resp.hasEntity());
    assertEquals(MediaType.MULTIPART_FORM_DATA_TYPE.getType(), resp.getMediaType().getType());
    assertEquals(MediaType.MULTIPART_FORM_DATA_TYPE.getSubtype(), resp.getMediaType().getSubtype());
    FormDataMultiPart translatedPkg = resp.readEntity(FormDataMultiPart.class);
    assertEquals(1, translatedPkg.getBodyParts().size());
    assertEquals(1, translatedPkg.getFields("test").size());
    JsonReader reader = Json.createReader(new StringReader(translatedPkg.getBodyParts().get(0).getEntityAs(String.class)));
    JsonObject obj = reader.readObject();
    JsonObject library = obj.getJsonObject("library");
    JsonArray annotations = library.getJsonArray("annotation");
    assertNotNull(annotations);
    JsonObject errorAnnotation = annotations.getJsonObject(1);
    assertEquals("CqlToElmError", errorAnnotation.getString("type"));
    assertEquals("semantic", errorAnnotation.getString("errorType"));
    assertEquals(startLine, errorAnnotation.getInt("startLine"));
    assertEquals(startChar, errorAnnotation.getInt("startChar"));
    assertEquals(errorMessage, errorAnnotation.getString("message"));
  }

  private void validateListPromotionEnabled(String cqlTitle) {
    File file = new File(TranslationResourceTest.class.getResource(cqlTitle).getFile());
    FormDataMultiPart pkg = new FormDataMultiPart();
    pkg.field("test", file, new MediaType("application", "cql"));
    Response resp = target.path("translator").queryParam("disable-list-promotion", "false").request(MediaType.MULTIPART_FORM_DATA).post(Entity.entity(pkg, MediaType.MULTIPART_FORM_DATA));
    assertTrue(resp.hasEntity());
    assertEquals(MediaType.MULTIPART_FORM_DATA_TYPE.getType(), resp.getMediaType().getType());
    assertEquals(MediaType.MULTIPART_FORM_DATA_TYPE.getSubtype(), resp.getMediaType().getSubtype());
    FormDataMultiPart translatedPkg = resp.readEntity(FormDataMultiPart.class);
    assertEquals(1, translatedPkg.getBodyParts().size());
    assertEquals(1, translatedPkg.getFields("test").size());
    JsonReader reader = Json.createReader(new StringReader(translatedPkg.getBodyParts().get(0).getEntityAs(String.class)));
    JsonObject obj = reader.readObject();
    JsonObject library = obj.getJsonObject("library");
    JsonArray annotations = library.getJsonArray("annotation");
    assertEquals(1, annotations.size());
    assertEquals(1, library.getJsonObject("statements").size());
  }

  @Test
  public void testInvalidCqlAsXml() {
    File file = new File(TranslationResourceTest.class.getResource("invalid.cql").getFile());
    Response resp = target.path("translator").request(TranslationResource.ELM_XML_TYPE).post(Entity.entity(file, TranslationResource.CQL_TEXT_TYPE));
    assertEquals(Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
    assertEquals(TranslationResource.ELM_XML_TYPE, resp.getMediaType().toString());
    assertTrue(resp.hasEntity());
    try {
      Document doc = parseXml(resp.readEntity(File.class));
      String errorCount = applyXPath(doc, "count(/elm:library/elm:annotation[@errorType='syntax'])");
      assertEquals(1, Integer.parseInt(errorCount));
      String errorLine = applyXPath(doc, "/elm:library/elm:annotation[@errorType='syntax'][1]/@startLine");
      assertEquals(1, Integer.parseInt(errorLine));
      String errorChar = applyXPath(doc, "/elm:library/elm:annotation[@errorType='syntax'][1]/@startChar");
      assertEquals(0, Integer.parseInt(errorChar));
    } catch (Exception ex) {
      fail("Error parsing returned XML");
    }
  }

  @Test
  public void testInvalidCqlAsJson() {
    File file = new File(TranslationResourceTest.class.getResource("invalid.cql").getFile());
    Response resp = target.path("translator").request(TranslationResource.ELM_JSON_TYPE).post(Entity.entity(file, TranslationResource.CQL_TEXT_TYPE));
    assertEquals(Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
    assertEquals(TranslationResource.ELM_JSON_TYPE, resp.getMediaType().toString());
    assertTrue(resp.hasEntity());
    JsonReader reader = Json.createReader(new StringReader(resp.readEntity(String.class)));
    JsonObject obj = reader.readObject();
    JsonObject library = obj.getJsonObject("library");
    JsonArray annotations = library.getJsonArray("annotation");
    assertEquals(2, annotations.size());
    JsonObject errorAnnotation = annotations.getJsonObject(1);
    assertEquals("CqlToElmError", errorAnnotation.getString("type"));
    assertEquals("syntax", errorAnnotation.getString("errorType"));
    assertEquals(1, errorAnnotation.getInt("startLine"));
    assertEquals(0, errorAnnotation.getInt("startChar"));
  }

  @Test
  public void testMissingLibraryAsJson() {
    File file = new File(TranslationResourceTest.class.getResource("missingLibrary.cql").getFile());
    Response resp = target.path("translator").request(TranslationResource.ELM_JSON_TYPE).post(Entity.entity(file, TranslationResource.CQL_TEXT_TYPE));
    assertEquals(Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
    assertEquals(TranslationResource.ELM_JSON_TYPE, resp.getMediaType().toString());
    assertTrue(resp.hasEntity());
    JsonReader reader = Json.createReader(new StringReader(resp.readEntity(String.class)));
    JsonObject obj = reader.readObject();
    JsonObject library = obj.getJsonObject("library");
    JsonArray annotations = library.getJsonArray("annotation");
    assertEquals(2, annotations.size());
    JsonObject errorAnnotation = annotations.getJsonObject(1);
    assertEquals("CqlToElmError", errorAnnotation.getString("type"));
    assertEquals("semantic", errorAnnotation.getString("errorType"));
    assertEquals(5, errorAnnotation.getInt("startLine"));
    assertEquals(1, errorAnnotation.getInt("startChar"));
    assertEquals("Could not load source for library CMSAll, version 1.", errorAnnotation.getString("message"));
  }

  @Test
  public void testValidLibraryAsJson() {
    File file = new File(TranslationResourceTest.class.getResource("valid.cql").getFile());
    Response resp = target.path("translator").request(TranslationResource.ELM_JSON_TYPE).post(Entity.entity(file, TranslationResource.CQL_TEXT_TYPE));
    assertEquals(Status.OK.getStatusCode(), resp.getStatus());
    assertEquals(TranslationResource.ELM_JSON_TYPE, resp.getMediaType().toString());
    assertTrue(resp.hasEntity());
    JsonReader reader = Json.createReader(new StringReader(resp.readEntity(String.class)));
    JsonObject obj = reader.readObject();
    JsonObject library = obj.getJsonObject("library");
    JsonArray annotations = library.getJsonArray("annotation");
    assertEquals(1, annotations.size());
    JsonObject identifier = library.getJsonObject("identifier");
    assertEquals("CMS146", identifier.getString("id"));
    assertEquals("2", identifier.getString("version"));
    // By default, should not have annotations or result types
    JsonArray defs = library.getJsonObject("statements").getJsonArray("def");
    assertEquals(2, defs.size());
    assertFalse(defs.getJsonObject(1).containsKey("resultTypeName"));
    assertFalse(defs.getJsonObject(1).containsKey("annotation"));
  }

  @Test
  public void testValidLibraryAsJsonWithAnnotationsAndResultTypes() {
    File file = new File(TranslationResourceTest.class.getResource("valid.cql").getFile());
    Response resp = target.path("translator").queryParam("annotations", "true").queryParam("result-types", "true").request(TranslationResource.ELM_JSON_TYPE).post(Entity.entity(file, TranslationResource.CQL_TEXT_TYPE));
    assertEquals(Status.OK.getStatusCode(), resp.getStatus());
    assertEquals(TranslationResource.ELM_JSON_TYPE, resp.getMediaType().toString());
    assertTrue(resp.hasEntity());
    JsonReader reader = Json.createReader(new StringReader(resp.readEntity(String.class)));
    JsonObject obj = reader.readObject();
    JsonObject library = obj.getJsonObject("library");
    JsonArray annotations = library.getJsonArray("annotation");
    assertEquals(2, annotations.size());
    JsonObject identifier = library.getJsonObject("identifier");
    assertEquals("CMS146", identifier.getString("id"));
    assertEquals("2", identifier.getString("version"));
    // Validate it has annotations and result types
    JsonArray defs = library.getJsonObject("statements").getJsonArray("def");
    assertEquals(2, defs.size());
    String resultTypeName = defs.getJsonObject(1).getString("resultTypeName");
    assertEquals(resultTypeName, "{urn:hl7-org:elm-types:r1}Boolean");
    JsonArray annotation = defs.getJsonObject(1).getJsonArray("annotation");
    assertNotNull(annotation);
    assertEquals(1, annotation.size());
  }

  @Test
  public void testInvalidListPromotionExistsAsJson() {
    validateListPromotionDisabled("ListPromotionExists.cql", 8, 13, "Could not resolve call to operator Exists with signature (System.Integer).");
  }

  @Test
  public void testInvalidListPromotionInAsJson() {
    validateListPromotionDisabled("ListPromotionIn.cql", 7, 16, "Could not resolve call to operator In with signature (System.Integer,System.Integer).");
  }

  @Test
  public void testValidListPromotionInAsJson() {
    validateListPromotionEnabled("ListPromotionIn.cql");
  }

  @Test
  public void testValidListPromotionExistsAsJson() {
    validateListPromotionEnabled("ListPromotionExists.cql");
  }

  @Test
  public void testSingleLibraryAsMultipart() {
    File file = new File(TranslationResourceTest.class.getResource("valid.cql").getFile());
    FormDataMultiPart pkg = new FormDataMultiPart();
    pkg.field("foo", file, new MediaType("application", "cql"));
    Response resp = target.path("translator").request(MediaType.MULTIPART_FORM_DATA).post(Entity.entity(pkg, MediaType.MULTIPART_FORM_DATA));
    assertEquals(Status.OK.getStatusCode(), resp.getStatus());
    assertEquals(MediaType.MULTIPART_FORM_DATA_TYPE.getType(), resp.getMediaType().getType());
    assertEquals(MediaType.MULTIPART_FORM_DATA_TYPE.getSubtype(), resp.getMediaType().getSubtype());
    assertTrue(resp.hasEntity());
    FormDataMultiPart translatedPkg = resp.readEntity(FormDataMultiPart.class);
    assertEquals(1, translatedPkg.getBodyParts().size());
    assertEquals(1, translatedPkg.getFields("foo").size());
    parseAndValidateJson( translatedPkg.getBodyParts().get(0), "CMS146", "2", 0 );
  }

  @Test
  public void testCrossLibraryResolution() {
    String filenames[] = {"ProvidesDependency.cql", "HasDependency.cql"};
    FormDataMultiPart pkg = new FormDataMultiPart();
    for (String filename: filenames) {
      File file = new File(TranslationResourceTest.class.getResource(filename).getFile());
      pkg.field(filename, file, new MediaType("application", "cql"));
    }
    Response resp = target.path("translator").request(MediaType.MULTIPART_FORM_DATA).post(Entity.entity(pkg, MediaType.MULTIPART_FORM_DATA));
    assertEquals(Status.OK.getStatusCode(), resp.getStatus());
    assertEquals(MediaType.MULTIPART_FORM_DATA_TYPE.getType(), resp.getMediaType().getType());
    assertEquals(MediaType.MULTIPART_FORM_DATA_TYPE.getSubtype(), resp.getMediaType().getSubtype());
    assertTrue(resp.hasEntity());
    FormDataMultiPart translatedPkg = resp.readEntity(FormDataMultiPart.class);
    assertEquals(2, translatedPkg.getBodyParts().size());
    for (String filename: filenames) {
      assertEquals(1, translatedPkg.getFields(filename).size());
      JsonReader reader = Json.createReader(new StringReader(translatedPkg.getFields(filename).get(0).getEntityAs(String.class)));
      JsonObject obj = reader.readObject();
      JsonObject library = obj.getJsonObject("library");
      JsonArray annotations = library.getJsonArray("annotation");
      assertEquals(1, annotations.size()); // should be no errors, dependency should be resolved
      JsonObject identifier = library.getJsonObject("identifier");
      assertNotNull(identifier.getString("id"));
      assertNotNull(identifier.getString("version"));
    }
  }

  @Test
  public void testMultipartRequestAsXml() throws Exception {
    String filenames[] = {"valid.cql"};
    FormDataMultiPart pkg = new FormDataMultiPart();
    for (String filename: filenames) {
      File file = new File(TranslationResourceTest.class.getResource(filename).getFile());
      pkg.field(filename, file, new MediaType("application", "cql"));
    }

    Response resp = target.path("translator").request(MediaType.MULTIPART_FORM_DATA)
        .header(TranslationResource.TARGET_FORMAT, TranslationResource.ELM_XML_TYPE)
        .post(Entity.entity(pkg, MediaType.MULTIPART_FORM_DATA));
    assertEquals(Status.OK.getStatusCode(), resp.getStatus());
    assertEquals(MediaType.MULTIPART_FORM_DATA_TYPE.getType(), resp.getMediaType().getType());
    assertEquals(MediaType.MULTIPART_FORM_DATA_TYPE.getSubtype(), resp.getMediaType().getSubtype());
    assertTrue(resp.hasEntity());
    FormDataMultiPart translatedPkg = resp.readEntity(FormDataMultiPart.class);
    assertEquals(1, translatedPkg.getBodyParts().size());
    for (String filename: filenames) {
      assertEquals(1, translatedPkg.getFields(filename).size());
      parseAndValidateXml( translatedPkg.getFields(filename).get(0), "CMS146", "2", 0 );
    }
  }

  @Test
  public void testMultipartRequestAsJsonAndXml() throws Exception {
    String filenames[] = {"valid.cql"};
    FormDataMultiPart pkg = new FormDataMultiPart();
    for (String filename: filenames) {
      File file = new File(TranslationResourceTest.class.getResource(filename).getFile());
      pkg.field(filename, file, new MediaType("application", "cql"));
    }

    Response resp = target.path("translator").request(MediaType.MULTIPART_FORM_DATA)
        .header(TranslationResource.TARGET_FORMAT, TranslationResource.ELM_XML_TYPE)
        .header(TranslationResource.TARGET_FORMAT, TranslationResource.ELM_JSON_TYPE)
        .post(Entity.entity(pkg, MediaType.MULTIPART_FORM_DATA));
    assertEquals(Status.OK.getStatusCode(), resp.getStatus());
    assertEquals(MediaType.MULTIPART_FORM_DATA_TYPE.getType(), resp.getMediaType().getType());
    assertEquals(MediaType.MULTIPART_FORM_DATA_TYPE.getSubtype(), resp.getMediaType().getSubtype());
    assertTrue(resp.hasEntity());
    FormDataMultiPart translatedPkg = resp.readEntity(FormDataMultiPart.class);
    assertEquals(2, translatedPkg.getBodyParts().size());
    for (String filename: filenames) {
      assertEquals(2, translatedPkg.getFields(filename).size());
      for( FormDataBodyPart part : translatedPkg.getFields(filename) ) {
        if( part.getMediaType().equals( MediaType.valueOf( TranslationResource.ELM_XML_TYPE ) ) ) {
        	parseAndValidateXml( part, "CMS146", "2", 0 );
        } else if( part.getMediaType().equals( MediaType.valueOf( TranslationResource.ELM_JSON_TYPE ) ) ) {
        	parseAndValidateJson( part, "CMS146", "2", 0 );
        } else {
          fail( "Unsupported media type" );
        }
      }
    }
  }

  private Document parseAndValidateXml( BodyPart input, String expectedId, String expectedVersion, int expectedErrors ) throws Exception {
      Document doc = parseXml(input.getEntityAs(String.class));
      String errorCount = applyXPath(doc, "count(/elm:library/elm:annotation[@errorType='syntax'])");
      assertEquals(0, Integer.parseInt(errorCount));
      assertEquals("CMS146", applyXPath(doc, "/elm:library/elm:identifier/@id") );
      assertEquals("2", applyXPath(doc, "/elm:library/elm:identifier/@version") );
      return doc;
  }

  private JsonObject parseAndValidateJson( BodyPart input, String expectedId, String expectedVersion, int expectedErrors ) {
      JsonReader reader = Json.createReader( new StringReader(input.getEntityAs(String.class)) );
      JsonObject obj = reader.readObject();
      JsonObject library = obj.getJsonObject("library");
      JsonArray annotations = library.getJsonArray("annotation");
      if( expectedErrors == 0 ) {
    	  assertEquals(1, annotations.size() );
      } else {
    	  assertEquals( expectedErrors + 1, annotations.size() );
      }
      JsonObject identifier = library.getJsonObject("identifier");
      assertEquals( expectedId, identifier.getString("id"));
      assertEquals( expectedVersion, identifier.getString("version"));

      return library;
  }

  private static class ElmNamespaceContext implements NamespaceContext {

    private final Map<String, String> ns = new HashMap<>();

    public ElmNamespaceContext() {
      ns.put("elm", "urn:hl7-org:elm:r1");
      ns.put("t", "urn:hl7-org:elm-types:r1");
      ns.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
      ns.put("xsd", "http://www.w3.org/2001/XMLSchema");
      ns.put("quick", "http://hl7.org/fhir");
      ns.put("a", "urn:hl7-org:cql-annotations:r1");
    }

    @Override
    public String getNamespaceURI(String prefix) {
      return ns.get(prefix);
    }

    @Override
    public String getPrefix(String uri) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<String> getPrefixes(String uri) {
      throw new UnsupportedOperationException();
    }
  }
}
