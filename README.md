# ACE Resource Server (Unconstrained)

## Prerequisites
The following software needs to be installed for this project to compile and run:
* Java JDK 8+
* Gradle

This project also depends on the ace-java (https://bitbucket.org/sebastian_echeverria/ace-java-postgres) and aaiot-lib (https://github.com/SEI-TTG/aaiot-lib) libraries. You should download, compile, and deploy both of them to a local Maven repo, so that this project will find them when resolving its dependencies.
 
## Configuration
No configuration is needed. Optional parameters are available in "config.json":

 * id: the ID or name the RS will use
 * local_coap_port: the port the RS server will be use to listen to COAP requests. 
 * local_coaps_port: the port the RS server will be use to listen to COAPS requests. Note that the default value is not the standard COAPS port to avoid clashing with an AS running on the same computer.

Credentials from a pairing procedure are stored in credentials.json. If you want to clear a previous pairing, simply delete the file.
 
## Usage
The main entry class is edu.cmu.sei.ttg.aaiot.rs.main. This starts the command-line interface. A simple way to start it from gradle is with `./gradlew run` 

The following resources are available on this server:
 * ace/temp: returns a random temperature.
   * GET - scopes to access: r_temp
 * ace/helloWorld: returns a hello world message.
   * GET - scopes to access: HelloWorld
 * ace/light: returns a random value of 0 or 1.
   * GET - scopes to access: r_light
 * ace/lock: handle a lock value that can be true or false.
   * GET - scopes to access: r_lock, or rw_lock
   * PUT - scopes to access: rw_lock
   
For more information, see https://github.com/SEI-TAS/ace-client/wiki
