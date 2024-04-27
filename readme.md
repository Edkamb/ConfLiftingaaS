# Digital Twin Systems with coupled behavior and semantic services
This repository contains the source code for the architectural approach to realize Digital Twin systems with coupled behavior and semantic services.

## Architecture
The architecture is an extension of a previous work, the *TwinManager* (originally [DT Manager](https://github.com/cdl-mint/DTManagementFramework)) to easily integrate black-box simulation in Digital Twin platforms.  
This extension now integrates a co-simulation engine, [Maestro](https://github.com/INTO-CPS-Association/maestro), to enable the composition of Digital Twins with coupled behavior.  
The source code for the prototypical Java implementation of the architecture is in the folder ```DTProject/tools/TwinManager```.

### Build the implementation
Use the script ```DTProject/build_TwinManager.sh``` to build the JAR that is used as part of the examples. The resulting JAR will be copied to the tools folder, where it is taken by the programs to run the examples. The installation requires Maven 3.9+.


## Semantic services
The structure to provide Digital Twin services on top of the architecture is based on the Semantic Micro Object Language ([SMOLang](https://smolang.org/)).  
The structure is adapted to the architecture, so it can execute rules and queries using the ontological model behind the architecture.  
The source code for the prototypical Kotlin implementation of the structure and semantic services is in the folder ```src```, which uses the models stored in the folder ```examples```.  
The script that executes the semantic services is ```src/main/kotlin/Main.kt```, which can be parameterized to work on different ontological models.

## Execution of experiments
There are two examples to be run using the architecture, the Flex-cell case study and the three-tank system case study.

### Models
All the models, i.e., FMUs and Asset Administration Shell (AAS) representations (Twin Schemas), are in the ```DTProject/models``` folder.


### Flex-cell
The configuration files (Twin Configurations and Twin System Configurations) for the Flex-cell example are in the folder ```DTProject/config_files_flexcell```.  

To run the Flex-cell case study, use the script ```DTProject/execution_flexcell.sh``` to run the example. For the simulation-only experiment, the example is dependent on the [RabbitMQ FMU](https://github.com/INTO-CPS-Association/fmu-rabbitmq), which requires a RabbitMQ broker with default credentials up and running in the machine. The physical twin is attached via MQTT, but it is not a dependency to run the example.  
This script takes the TwinManager, the Flex-cell java script in ```DTProject/tools/FlexCellExample.java```, and the RabbitMQ routine publisher in ```DTProject/python-code/publisher-flexcell-physical.py``` to execute the experiment using the FMU models for the UR5e and the Kuka lbr iiwa 7, and the RabbitMQ FMU.

The output of experiments is generated to the folder ```DTProject/data_flexcell/output```.

### Three-Tank System
The configuration files (Twin Configurations and Twin System Configurations) for the Flex-cell example are in the folder ```DTProject/config_files_three_tank```.  

To run the Three-Tank System case study, use the script ```DTProject/execution_three_tank.sh``` to run the example. This is a simulation-only example where all the dependencies are already included.  
This script takes the TwinManager and the Three-Tank java script in ```DTProject/tools/ThreeTankSystemExample.java``` to execute the experiments of a prototypical three-tank system where each tank is defined by an FMU of the same class, but parameterized with different values.

The output of experiments is generated to the folder ```DTProject/data_three_tank/output```.

## Additional resources 
More information about the TwinManager (originally DT Manager) and SMOLang is available in:

1. D. Lehner, S. Gil, P. H. Mikkelsen, P. G. Larsen and M. Wimmer,
   "An Architectural Extension for Digital Twin Platforms to Leverage
   Behavioral Models," 2023 IEEE 19th International Conference on
   Automation Science and Engineering (CASE), Auckland, New Zealand,
   2023, pp. 1-8, doi: 10.1109/CASE56687.2023.10260417. https://ieeexplore.ieee.org/abstract/document/10260417
2. S. Gil, P. H. Mikkelsen, D. Tola, C. Schou and P. G. Larsen,
   "A Modeling Approach for Composed Digital Twins in Cooperative Systems,"
   2023 IEEE 28th International Conference on Emerging Technologies
   and Factory Automation (ETFA), Sinaia, Romania, 2023, pp. 1-8,
   doi: 10.1109/ETFA54631.2023.10275601. https://ieeexplore.ieee.org/abstract/document/10275601
3. Kamburjan, E., Klungre, V.N., Schlatte, R., Johnsen, E.B., Giese, M. (2021). "Programming and Debugging with Semantically Lifted States." In: Verborgh, R., et al. The Semantic Web. ESWC 2021. Lecture Notes in Computer Science(), vol 12731. Springer, Cham. https://doi.org/10.1007/978-3-030-77385-4_8
