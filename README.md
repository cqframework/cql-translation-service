# CQL to ELM Translation Service

A microservice wrapper for the CQL to ELM conversion library.

Build:

    mvn package

Execute via the command line:

    java -jar target/cqlTranslationServer-1.5.10-jar-with-dependencies.jar

## Simple Request

Example usage via HTTP request:

    POST /cql/translator HTTP/1.1
    Content-Type: application/cql
    Accept: application/elm+json
    Host: localhost:8080
    Connection: close
    Content-Length: 610

    library CMS146 version '2'

    using QUICK

    valueset "Acute Pharyngitis": '2.16.840.1.113883.3.464.1003.102.12.1011'
    ...

Will return:

    HTTP/1.1 200 OK
    Content-Type: application/elm+json
    Date: Wed, 10 Feb 2016 22:15:33 GMT
    Connection: close
    Content-Length: 6932

    {
      "library": {
        "identifier": {
          "id": "CMS146",
          "version": "2"
        },
        "usings": {"def": [
          {
            "localIdentifier": "System",
            "uri": "urn:hl7-org:elm-types:r1"
          },
          {
            "localId": "1",
            "localIdentifier": "QUICK",
            "uri": "http://hl7.org/fhir"
          }
        ]},
        "valueSets": {"def": [{
          "localId": "2",
          "name": "Acute Pharyngitis",
          "id": "2.16.840.1.113883.3.464.1003.102.12.1011",
          "accessLevel": "Public"
        },...]},
        ...
      }
    }

## Multipart Request

The service also supports `POST` of multiple CQL libraries packaged as
`multipart/form-data`. The result will be a similar package with one ELM part for each
CQL part in the submitted package.

Example usage via HTTP request:

    POST /cql/translator HTTP/1.1
    Content-Type: multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW
    Accept: multipart/form-data
    Host: localhost:8080
    content-length: 545
    Connection: keep-alive

    ------WebKitFormBoundary7MA4YWxkTrZu0gW--,
    Content-Disposition: form-data; name="HelloWorld"

    library HelloWorld version '1.0.0'

    using QDM

    include Speaker version '1.0.0' called Speaker

    define Hello: 'World'

    define SpeakerName: Speaker.Name
    ------WebKitFormBoundary7MA4YWxkTrZu0gW--
    Content-Disposition: form-data; name="Speaker"

    library Speaker version '1.0.0'

    using QDM

    define Name: 'Bob'
    ------WebKitFormBoundary7MA4YWxkTrZu0gW--

Will return:

    HTTP/1.1 200
    status: 200
    MIME-Version: 1.0
    Content-Type: multipart/form-data;boundary=Boundary_2_526521536_1556163069788
    Date: Thu, 25 Apr 2019 03:47:49 GMT
    Content-Length: 2365

    --Boundary_2_526521536_1556163069788
    Content-Type: application/elm+json
    Content-Disposition: form-data; name="HelloWorld"

    {
      "library" : {
          "identifier" : {
            "id" : "HelloWorld",
            "version" : "1.0.0"
          },
          "schemaIdentifier" : {
            "id" : "urn:hl7-org:elm",
            "version" : "r1"
          },
          ...
      }
    }
    --Boundary_2_526521536_1556163069788
    Content-Type: application/elm+json
    Content-Disposition: form-data; name="Speaker"

    {
      "library" : {
          "identifier" : {
            "id" : "Speaker",
            "version" : "1.0.0"
          },
          "schemaIdentifier" : {
            "id" : "urn:hl7-org:elm",
            "version" : "r1"
          },
          ...
      }
    }
    --Boundary_2_526521536_1556163069788--

## CQL-to-ELM Options

The CQL-to-ELM translator supports many options to control the output.  These options can be passed to the service as query parameters when you post CQL to the service (e.g., `POST http://localhost:8080/cql/translator?annotations=true&result-types=true`).  These query parameters are supported for both simple requests and multipart requests.  See the table below for the available options:

|Option|Values|Default|
|----|----|----|
|date-range-optimization|true\|false|false|
|annotations|true\|false|false|
|locators|true\|false|false|
|result-types|true\|false|false|
|signatures|None\|Differing\|Overloads\|All|None|
|detailed-errors|true\|false|false|
|disable-list-traversal|true\|false|false|
|disable-list-demotion|true\|false|false|
|disable-list-promotion|true\|false|false|
|enable-interval-demotion|true\|false|false|
|enable-interval-promotion|true\|false|false|
|disable-method-invocation|true\|false|false|
|require-from-keyword|true\|false|false|
|strict|true\|false|false|
|debug|true\|false|false|
|validate-units|true\|false|false|

For more information on each of these options, see the [CQL-to-ELM Overview](https://github.com/cqframework/clinical_quality_language/blob/master/Src/java/cql-to-elm/OVERVIEW.md#usage).

_**NOTE:**_
* _Previous versions of the CQL-to-ELM Translation Service defaulted **annotations** to true.  To align better with the CQL-to-ELM console client, the translation service now defaults annotations to false._
* _Previous versions of the CQL-to-ELM Translation Service allowed list-promotion to be disabled via an extra multipart form field named **disablePromotion**. This is no longer supported, as it was ambiguous and inconsistent with the CQL-to-ELM console clinet.  The **disable-list-promotion** query parameter should be used instead._

## Docker Deployment

You may deploy pre-built Docker images into your existing hosting environment with:

	docker run -d -p 8080:8080 --restart unless-stopped cqframework/cql-translation-service:latest # or any official tag

And you're done. No environment variables or further configuration are needed. Jedis may use your existing Kubernetes, Open Shift etc installations as you see fit. :)

To build your own image:

	docker build -t cqframework/cql-translation-service:latest . # but use your your own repo and tag strings!

## Environment Variables

**CQL_TRANSLATOR_PORT** - Allows you to set the port on which the translation service runs, default value is 8080

## License

Copyright 2016-2019 The MITRE Corporation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

		http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
