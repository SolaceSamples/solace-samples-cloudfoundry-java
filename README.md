# solace-samples-cloudfoundry-java

The repository contains example applications that use the Solace Messaging service on Pivotal Cloud Foundry. The goal of these sample applications is to illustrate various ways of consuming the `VCAP_SERVICES` environment variable from a Solace Messaging Cloud Foundry service instance. You can get more details on the  Solace Messaging Service for Pivotal Cloud Foundry [here](TODO - Need link to PCF tile cloud foundry documentation).

This repository contains the following sample applications:
- Java main application
- Java main application using spring cloud connectors
- (Coming soon) Spring boot application using spring cloud connectors

All of these sample applications have full walk through tutorials that explain the code in detail. Tutorials for each sample are available here:

* https://solacesamples.github.io/solace-samples-cloudfoundry-java/

What follows is a brief summary for people that want to dive straight into the code.

## Common Setup 

The sample applications specify a dependency on a Solace Messaging service instance named `solace-messaging-sample-instance`. To create the required Solace messaging service instance, do the following:

	cf create-service solacemessaging vmr-shared solace-messaging-sample-instance
	

### Building

Clone this repo, then build all examples with:

	$ ./gradlew build

### Deploying

To deploy the individual applications to Cloud Foundry:
1. cd to the project dir
1. `$ cf push`

## Java main application

application name: `solace-sample-java-app`

This application uses the Java library from http://www.JSON.org/ to parse the `VCAP_SERVICES` environment variable to determine the connection details for Solace messaging. For more details and example usage, see the walk through tutorial here:

* [Online Tutorial](https://solacesamples.github.io/solace-samples-cloudfoundry-java/docs/java-app/)

## Java main application using spring cloud connector

application name: `solace-sample-spring-cloud`

This application makes use of the Spring Cloud Connectors project to automatically parse the `VCAP_SERVICES` environment variable. Applications do *not* have to be a spring boot application to make use of Spring Cloud Connectors. Simply specify the following dependencies in your build:

	compile 'org.springframework.cloud:spring-cloud-spring-service-connector:1.2.3.RELEASE'
	compile 'org.springframework.cloud:spring-cloud-cloudfoundry-connector:1.2.3.RELEASE'
	compile 'com.solace.labs.cloud.cloudfoundry:solace-labs-spring-cloud-connector:0.1.0'

The `solace-labs-spring-cloud-connector` is a Spring Cloud Connectors extension to parse the `VCAP_SERVICES` for the Solace Messaging service instance information. Check out the project page for more details:

* https://github.com/SolaceLabs/sl-solace-messaging-service-info

Application can access the SolaceMessagingInfo object as follows:

	CloudFactory cloudFactory = new CloudFactory();
	Cloud cloud = cloudFactory.getCloud();
	SolaceMessagingInfo solacemessaging = (SolaceMessagingInfo) cloud.getServiceInfo("MyService");

For more details and example usage, see the walk through tutorial here:

* [Online Tutorial](https://solacesamples.github.io/solace-samples-cloudfoundry-java/docs/spring-cloud/)


## Try out the Applications

The sample applications have a simple REST interface that allows you to subscribe, send and receive Solace messages. You can try the applications out using command like the following:

Subscribe to topic "test"

	curl -sX POST -H "Authorization: Basic c29sYWNlZGVtbzpzb2xhY2VkZW1v" -H "Content-Type: application/json;charset=UTF-8" -d '{"subscription": "test"}' http://${APP_URL}/subscription 

Send message with topic "test"

	curl -sX POST -H "Authorization: Basic c29sYWNlZGVtbzpzb2xhY2VkZW1v" -H "Content-Type: application/json;charset=UTF-8" -d '{"topic": "test", "body": "TEST_MESSAGE"}' http://${APP_URL}/message 

The message is received asynchronously, check for the last message.
	
	curl -sX GET -H "Authorization: Basic c29sYWNlZGVtbzpzb2xhY2VkZW1v"  http://${APP_URL}/message 

