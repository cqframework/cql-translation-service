library TestListPromotionExists

using FHIR version '4.0.1'

context Patient

define Foo: First({1, 2, 3, 4})
define Bar: exists Foo
