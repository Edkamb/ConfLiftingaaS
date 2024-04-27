#!/bin/bash
echo "Building TwinManager"
echo "-----------------------------"
(cd tools/TwinManager && mvn -f pom.xml package -Dmaven.test.skip=true -Dmaven.javadoc.skip=true)
cp tools/TwinManager/target/TwinManagerFramework-0.0.3.jar tools/
