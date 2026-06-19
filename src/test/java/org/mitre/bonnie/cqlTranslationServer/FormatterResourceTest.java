package org.mitre.bonnie.cqlTranslationServer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FormatterResourceTest {

  private HttpServer server;
  private WebTarget target;

  // The CQL formatter is hard-coded to use \r\n for line breaks
  private final String newLine = "\r\n";

  @BeforeEach
  public void setUp() throws Exception {
    // start the server
    server = Main.startServer();

    // create the client
    Client c = ClientBuilder.newBuilder().register(MultiPartFeature.class).build();
    target = c.target(Main.BASE_URI.replace("0.0.0.0", "localhost"));
  }

  @AfterEach
  public void tearDown() throws Exception {
    server.shutdownNow();
  }

  @Test
  void testUnformattedLibrary() {
    String input = "library HelloWorld using QDM define Hello: 'World'";
    Response resp = target.path("formatter").request(FormatterResource.CQL_TEXT_TYPE).post(Entity.entity(input, FormatterResource.CQL_TEXT_TYPE));
    assertEquals(Status.OK.getStatusCode(), resp.getStatus());
    assertEquals(FormatterResource.CQL_TEXT_TYPE, resp.getMediaType().toString());
    assertTrue(resp.hasEntity());
    String actual = resp.readEntity(String.class);
    String expected = String.join(
      newLine,
      "library HelloWorld",
      "",
      "using QDM",
      "",
      "define Hello:",
      "  'World'"
    );
    assertEquals(expected, actual);
  }

  @Test
  void testInvalidCql() {
    String input = "lib HelloWorld using QDM define Hello: 'World'";
    Response resp = target.path("formatter").request(FormatterResource.CQL_TEXT_TYPE).post(Entity.entity(input, FormatterResource.CQL_TEXT_TYPE));
    assertEquals(Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
    assertEquals("text/plain", resp.getMediaType().toString());
    assertTrue(resp.hasEntity());
    String actual = resp.readEntity(String.class);
    assertTrue(actual.startsWith("CQL formatting failed due to errors:[1:0]: mismatched input 'lib'"));
  }

  @Test
  void testSingleUnformattedLibraryAsMultipart() {
    String input = "library HelloWorld2 using QDM define Hello: 'World2'";
    FormDataMultiPart pkg = new FormDataMultiPart();
    pkg.field("foo", input, new MediaType("text", "cql"));
    Response resp = target.path("formatter").request(MediaType.MULTIPART_FORM_DATA).post(Entity.entity(pkg, MediaType.MULTIPART_FORM_DATA));
    assertEquals(Status.OK.getStatusCode(), resp.getStatus());
    assertEquals(MediaType.MULTIPART_FORM_DATA_TYPE.getType(), resp.getMediaType().getType());
    assertEquals(MediaType.MULTIPART_FORM_DATA_TYPE.getSubtype(), resp.getMediaType().getSubtype());
    assertTrue(resp.hasEntity());
    FormDataMultiPart translatedPkg = resp.readEntity(FormDataMultiPart.class);
    assertEquals(1, translatedPkg.getBodyParts().size());
    assertEquals(1, translatedPkg.getFields("foo").size());
    String actual = translatedPkg.getBodyParts().get(0).getEntityAs((String.class));
    String expected = String.join(
      newLine,
      "library HelloWorld2",
      "",
      "using QDM",
      "",
      "define Hello:",
      "  'World2'"
    );
    assertEquals(expected, actual);
  }

  @Test
  void testTwoUnformattedLibrariesAsMultipart() {
    String input = "library HelloWorld3 using FHIR define Hello: 'World3'";
    String input2 = "library FHIRHelpers version '4.0.1' using FHIR version '4.0.1'\n" +
      "context Patient define \"IsFakeFHIRHelpers\": true";
    FormDataMultiPart pkg = new FormDataMultiPart();
    pkg.field("foo", input, new MediaType("text", "cql"));
    pkg.field("zoo", input2, new MediaType("text", "cql"));
    Response resp = target.path("formatter").request(MediaType.MULTIPART_FORM_DATA).post(Entity.entity(pkg, MediaType.MULTIPART_FORM_DATA));
    assertEquals(Status.OK.getStatusCode(), resp.getStatus());
    assertEquals(MediaType.MULTIPART_FORM_DATA_TYPE.getType(), resp.getMediaType().getType());
    assertEquals(MediaType.MULTIPART_FORM_DATA_TYPE.getSubtype(), resp.getMediaType().getSubtype());
    assertTrue(resp.hasEntity());
    FormDataMultiPart translatedPkg = resp.readEntity(FormDataMultiPart.class);
    assertEquals(2, translatedPkg.getBodyParts().size());
    assertEquals(1, translatedPkg.getFields("foo").size());
    String actual = translatedPkg.getBodyParts().get(0).getEntityAs((String.class));
    String expected = String.join(
      newLine,
      "library HelloWorld3",
      "",
      "using FHIR",
      "",
      "define Hello:",
      "  'World3'"
    );
    assertEquals(expected, actual);
    assertEquals(1, translatedPkg.getFields("zoo").size());
    String actual2 = translatedPkg.getBodyParts().get(1).getEntityAs((String.class));
    String expected2 = String.join(
      newLine,
      "library FHIRHelpers version '4.0.1'",
      "",
      "using FHIR version '4.0.1'",
      "",
      "context Patient",
      "",
      "define \"IsFakeFHIRHelpers\":",
      "  true"
    );
    assertEquals(expected2, actual2);
  }

  @Test
  void testInvalidCqlInMultipart() {
    String input = "library HelloWorld3 using FHIR define Hello: 'World3'";
    String input2 = "library FHIRHelpers version '4.0.1' using FHIR version '4.0.1'\n" +
      "ctx Patient define \"IsFakeFHIRHelpers\": true";
    FormDataMultiPart pkg = new FormDataMultiPart();
    pkg.field("foo", input, new MediaType("text", "cql"));
    pkg.field("zoo", input2, new MediaType("text", "cql"));
    Response resp = target.path("formatter").request(MediaType.MULTIPART_FORM_DATA).post(Entity.entity(pkg, MediaType.MULTIPART_FORM_DATA));
    assertEquals(Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
    assertEquals("text/plain", resp.getMediaType().toString());
    assertTrue(resp.hasEntity());
    String actual = resp.readEntity(String.class);
    System.out.println(actual);
    assertTrue(actual.startsWith("CQL formatting failed due to errors:[2:0]: extraneous input 'ctx'"));
  }
}

