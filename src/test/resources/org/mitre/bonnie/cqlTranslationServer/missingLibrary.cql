library CMS146 version '2'

using FHIR version '4.0.1'

include CMSAll version '1' called foo

valueset "Acute Pharyngitis": '2.16.840.1.113883.3.464.1003.102.12.1011'

parameter MeasurementPeriod Interval<DateTime>

context Patient

define InDemographic:
    AgeInYearsAt(start of MeasurementPeriod) >= 2 and AgeInYearsAt(start of MeasurementPeriod) < 18
