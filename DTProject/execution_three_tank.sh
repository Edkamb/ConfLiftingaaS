#!/bin/bash
echo "Executing experiment"
echo "-----------------------------"
#python3 python-app/publisher-flexcell-physical.py &
#java -cp "tools/TwinManagerFramework-0.0.3.jar" tools/FlexCellExample.java
java -cp "tools/TwinManagerFramework-0.0.3.jar" tools/ThreeTankSystemExample.java

pkill -9 java
#pkill -9 -f python-app/publisher-flexcell-physical.py
