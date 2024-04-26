#!/bin/bash
echo "Executing Flex-cell experiment"
echo "-----------------------------"
pkill -9 java
pkill -9 -f python-app/publisher-flexcell-physical.py


python3 python-app/publisher-flexcell-physical.py &
java -cp "tools/TwinManagerFramework-0.0.3.jar" tools/FlexCellExample.java


pkill -9 java
pkill -9 -f python-app/publisher-flexcell-physical.py
