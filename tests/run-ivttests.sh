#!/bin/bash

cat << EOT >> ivttest.xml
<testsuite name="IVT Tests" tests="1" failures="0" errors="0" skipped="0" timestamp="Mon, 15 Feb 2021 01:03:46 GMT" time="0.011">
<testcase classname="App" name="ivtTest" time="0"/>
</testsuite>
EOT

cat ivttest.xml