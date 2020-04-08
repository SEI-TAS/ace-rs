# ACE Resource Server (Unconstrained)

## Prerequisites
The following software needs to be installed for this project to compile and run:
* Java JDK 8+
* Gradle

This project also depends on the ace-java (https://bitbucket.org/sebastian_echeverria/ace-java-postgres) and aaiot-lib (https://github.com/SEI-TTG/aaiot-lib) libraries. You should download, compile, and deploy both of them to a local Maven repo, so that this project will find them when resolving its dependencies.

 
## Configuration
No configuration is needed.
 
## Usage
The main entry class is edu.cmu.sei.ttg.aaiot.rs.main. This starts the command-line interface. A simple way to start it from gradle is with `./gradlew run` .

