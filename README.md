# CQL to ELM Translation Service

A microservice wrapper for the CQL to ELM conversion library and CQL formatter.

Build:

    mvn package

Execute via the command line:

    java -jar target/cqlTranslationServer-2.4.0.jar

_NOTE: The cqlTranslationServer jar assumes that all dependency jars are located in a `libs` directory relative to the jar's location. If you move the jar from the `target` directory, you will need to move the `target/libs` directory as well. This project no longer produces an "uber-jar", as the CQL-to-ELM classes do not function properly when repackaged into a single jar file._

## Version Table

CQL Translation Service versions prior to version 2.0.0 always mirrored the CQL Tools (CQL-to-ELM translator) version they exposed. Starting with version 2.0.0, semantic versioning is now used. As a result, the version of the CQL Translation Service differs from the version of the CQL Tools that it exposes. The following table shows the relationship between [CQL Translation Service releases](https://github.com/cqframework/cql-translation-service/releases) and [CQL Tools releases](https://github.com/cqframework/clinical_quality_language/releases). Note that prior releases from the [MITRE repo](https://github.com/mitre/cql-translation-service/releases) are not included here.

| CQL Translation Service | CQL Tools                               |
|-------------------------|-----------------------------------------|
| 2.4.0                   | 3.7.1                                   |
| 2.3.0                   | 3.3.2                                   |
| 2.2.0                   | 2.11.0                                  |
| 2.1.0                   | 2.10.0                                  |
| 2.0.0                   | 2.7.0                                   |
| 1.1.0-SNAPSHOT - 1.5.12 | Matches CQL Translation Service version |
| 1.0.2                   | 1.0.0                                   |

## Translator Endpoint

The `/cql/translator` endpoint handles translating CQL to ELM JSON and/or XML.

### Simple Translator Request

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

### Multipart Translator Request

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

### CQL-to-ELM Translator Options

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
* _Previous versions of the CQL-to-ELM Translation Service allowed list-promotion to be disabled via an extra multipart form field named **disablePromotion**. This is no longer supported, as it was ambiguous and inconsistent with the CQL-to-ELM console client.  The **disable-list-promotion** query parameter should be used instead._

## Formatter Endpoint

The `/cql/formatter` endpoint handles reformatting CQL for improved consistency and readability.

### Simple Formatter Request

Example usage via HTTP request:

    POST /cql/formatter HTTP/1.1
    Content-Type: application/cql
    Accept: application/cql
    Host: localhost:8080
    Content-Length: 50

    library HelloWorld using QDM define Hello: 'World'

Will return:

    HTTP/1.1 200 OK
    Content-Type: application/cql
    Content-Length: 59

    library HelloWorld

    using QDM

    define Hello:
      'World'

### Multipart Formatter Request

The service also supports `POST` of multiple CQL libraries packaged as
`multipart/form-data`. The result will be a similar package with one
formatted part for each CQL part in the submitted package.

Example usage via HTTP request:


    POST /cql/formatter HTTP/1.1
    Host: localhost:8080
    Content-Type: multipart/form-data; boundary=X-INSOMNIA-BOUNDARY
    Accept: multipart/form-data
    Content-Length: 465

    --X-INSOMNIA-BOUNDARY
    Content-Disposition: form-data; name="Simple.cql"
    library "SimpleLibrary" version '0.0.1' using FHIR version '4.0.1'
    include "FHIRHelpers" version '4.0.1' called FHIRHelpers
    context Patient define "MeaningOfLife": 42
    --X-INSOMNIA-BOUNDARY
    Content-Disposition: form-data; name="FHIRHelpers.cql"
    library FHIRHelpers version '4.0.1' using FHIR version '4.0.1'
    context Patient define "IsFakeFHIRHelpers": true
    --X-INSOMNIA-BOUNDARY--

Will return:

    HTTP/1.1 200 OK
    MIME-Version: 1.0
    Content-Type: multipart/form-data;boundary=Boundary_2_1638692479_1658770032240
    Content-Length: 600

    --Boundary_2_1638692479_1658770032240
    Content-Type: application/cql
    Content-Disposition: form-data; name="FHIRHelpers.cql"

    library FHIRHelpers version '4.0.1'

    using FHIR version '4.0.1'

    context Patient

    define "IsFakeFHIRHelpers":
      true
    --Boundary_2_1638692479_1658770032240
    Content-Type: application/cql
    Content-Disposition: form-data; name="Simple.cql"

    library "SimpleLibrary" version '0.0.1'

    using FHIR version '4.0.1'

    include "FHIRHelpers" version '4.0.1' called FHIRHelpers

    context Patient

    define "MeaningOfLife":
      42
    --Boundary_2_1638692479_1658770032240--


## Docker Deployment

You may deploy pre-built Docker images into your existing hosting environment with:

	docker run -d -p 8080:8080 --restart unless-stopped cqframework/cql-translation-service:latest # or any official tag

And you're done. No environment variables or further configuration are needed. Jedis may use your existing Kubernetes, Open Shift etc installations as you see fit. :)

To build your own image for your current architecture:

	docker build -t cqframework/cql-translation-service:latest . # but use your your own repo and tag strings!

To build your own image for multiple architectures (e.g., Intel and Mac M1):

	docker buildx build --platform linux/amd64,linux/arm64 -t cqframework/cql-translation-service:latest . # but use your your own repo and tag strings!

Note that Docker doesn't support loading multi-platform builds locally, so the above multi-platform build commmand is only helpful when used with `--push`. See: [https://github.com/docker/buildx/issues/59](https://github.com/docker/buildx/issues/59).

## Environment Variables

**CQL_TRANSLATOR_PORT** - Allows you to set the port on which the translation service runs, default value is 8080

## License

Copyright 2016-2023 The MITRE Corporation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

		http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
