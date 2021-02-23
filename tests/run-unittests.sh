#
# (C) Copyright IBM Corp. 2021, 2021
#
# SPDX-License-Identifier: Apache-2.0
#
#!/bin/bash

cat << EOT >> unittest.xml
<testsuite name="Pass Tests" tests="1" failures="0" errors="0" skipped="0" timestamp="Mon, 15 Feb 2021 01:03:46 GMT" time="0.011">
<testcase classname="AllTests" name="build would have failed if a test failed" time="0"/>
</testsuite>
EOT

cat unittest.xml
