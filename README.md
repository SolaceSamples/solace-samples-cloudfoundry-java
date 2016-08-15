# solace-samples-cloudfoundry-java

The repository contains example applications that use the Solace Messaging service on Pivotal Cloud Foundry. The goal of these sample applications is to illustrate various ways of consuming the `VCAP_SERVICES` environment variable from a Solace Messaging Cloud Foundry service instance. You can get more details on the  Solace Messaging Service for Pivotal Cloud Foundry [here](TODO - Need link to PCF tile cloud foundry documentation).

This repository contains a sample application modified in the following ways:

* Java Application
* Java Application using Spring Cloud Connectors
* (Coming soon) Annotated Spring Boot application using Spring Cloud Connectors

While not necessary for Java Application and straight Spring Cloud Connector applications, the samples in this repository still make use of of Spring Boot so they can easily expose a simple REST interface to provide interactive ways to subscribe, send and receive Solace messages. Spring Boot is not required. You can make use of the Spring Cloud Connectors in any Java Application. See the walk through tutorials for more details.

All of these sample applications have full walk through tutorials that explain the code in detail. Tutorials for each sample are available here:

* https://solacesamples.github.io/solace-samples-cloudfoundry-java/

What follows is a brief summary for people that want to dive straight into the code.

## Common Setup

The sample applications specify a dependency on a Solace Messaging service instance named `solace-messaging-sample-instance`. To create the required Solace messaging service instance, do the following:

	cf create-service solace-messaging vmr-shared solace-messaging-sample-instance

### Building

Clone this repo, then build all examples with:

	$ ./gradlew build

### Deploying

To deploy the individual applications to Cloud Foundry:

1. cd to the project dir
1. `$ cf push`

## Java Application

application name: `solace-sample-java-app`

This application uses the Java library from http://www.JSON.org/ to parse the `VCAP_SERVICES` environment variable to determine the connection details for Solace messaging. For more details and example usage, see the walk through tutorial here:

* [Online Tutorial](https://solacesamples.github.io/solace-samples-cloudfoundry-java/docs/java-app/)

## Java Application using Spring Cloud Connector

application name: `solace-sample-spring-cloud`

This application makes use of the Spring Cloud Connectors project to automatically parse the `VCAP_SERVICES` environment variable. Applications do *not* have to be a Spring Boot application to make use of Spring Cloud Connectors. This example makes use of Spring Boot for convenience in enabling the simple REST API. In any Java Applications, simply specify the following dependencies in your build:

	compile 'org.springframework.cloud:spring-cloud-spring-service-connector:1.2.3.RELEASE'
	compile 'org.springframework.cloud:spring-cloud-cloudfoundry-connector:1.2.3.RELEASE'
	compile 'com.solace.labs.cloud.cloudfoundry:solace-labs-spring-cloud-connector:0.1.0'

The `solace-labs-spring-cloud-connector` is a Spring Cloud Connectors extension to parse the `VCAP_SERVICES` for the Solace Messaging service instance information. Check out the project page for more details:

* https://github.com/SolaceLabs/sl-solace-messaging-service-info

The easiest way for applications to access the SolaceMessagingInfo object is by Service Id (ex: "MyService) as follows:

	CloudFactory cloudFactory = new CloudFactory();
	Cloud cloud = cloudFactory.getCloud();
	SolaceMessagingInfo solaceMessagingServiceInfo = (SolaceMessagingInfo) cloud.getServiceInfo("MyService");
	
Alternatively applications could search through the environment and discover matching services as follows:

	SolaceMessagingInfo solaceMessagingServiceInfo = null;
	List<ServiceInfo> services = cloud.getServiceInfos();
		
	// Connect to the first Solace-Messaging service that is found in the services list.
	for (ServiceInfo service : services) {
		if (service instanceof SolaceMessagingInfo) {
			solaceMessagingServiceInfo = (SolaceMessagingInfo)service;
			break;
		}
	}

For more details and example usage, see the walk through tutorial here:

* [Online Tutorial](https://solacesamples.github.io/solace-samples-cloudfoundry-java/docs/spring-cloud/)


## Try out the Applications

The sample applications have a simple REST interface that allows you to subscribe, send and receive Solace messages. You can try the applications out using command like the following.

Determine the URL of the sample application and export it for use in the `curl` commands. Adjust the app name as appropriate to match the sample you're using:

	export APP_NAME=solace-sample-java-app
	export APP_URL=`cf apps | grep $APP_NAME | grep started | awk '{ print $6}'`
	echo "The application URL is: ${APP_URL}"

Subscribe to topic "test"

	curl -X POST -H "Content-Type: application/json;charset=UTF-8" -d '{"subscription": "test"}' http://$APP_URL/subscription

Send message with topic "test"

	curl -X POST -H "Content-Type: application/json;charset=UTF-8" -d '{"topic": "test", "body": "TEST_MESSAGE"}' http://$APP_URL/message

The message is received asynchronously, check for the last message.

	curl -X GET http://$APP_URL/message
