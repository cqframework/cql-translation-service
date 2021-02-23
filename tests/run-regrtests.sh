cat << EOT >> regtest.xml
<testsuite name="Regression tests" tests="1" failures="0" errors="0" skipped="0" timestamp="Mon, 15 Feb 2021 01:03:46 GMT" time="0.011">
<testcase classname="AppReg" name="Test regression" time="0"/>
</testsuite>
EOT

cat regtest.xml
