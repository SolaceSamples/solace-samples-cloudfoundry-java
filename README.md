[![Build Status](https://travis-ci.org/SolaceSamples/solace-samples-cloudfoundry-java.svg?branch=master)](https://travis-ci.org/SolaceSamples/solace-samples-cloudfoundry-java)

# Getting Started Examples
## Solace Cloud Foundry Java

The repository contains example applications that use the Solace Pubsub+ service on Pivotal Cloud Foundry. The goal of these sample applications is to illustrate various ways of consuming the `VCAP_SERVICES` environment variable from a Solace Pubsub+ Cloud Foundry service instance. You can get more details on the Solace Pubsub+ Service for Pivotal Cloud Foundry [here](http://docs.pivotal.io/solace-messaging/).

This repository contains a sample application modified in the following ways:

* Simple Java Application
* Using Spring Cloud Connectors
* Connecting using Transport Level Security

While not necessary for a Java Application or a straight Spring Cloud Connector applications, the samples in this repository still make use of of Spring Boot so they can easily expose a simple REST interface to provide interactive ways to subscribe, send and receive Solace messages. Spring Boot is not required. You can make use of the Spring Cloud Connectors in any Java Application. See the walk through tutorials for more details.

All of these sample applications have full walk through tutorials that explain the code in detail. Tutorials for each sample are available here:

* https://dev.solace.com/samples/solace-samples-cloudfoundry-java/

What follows is a brief summary for people that want to dive straight into the code.

## Common Setup

The sample applications specify a dependency on a Solace Pubsub+ service instance named `solace-pubsub-sample-instance`. To create the required Solace PubSub+ service instance, do the following:

	cf create-service solace-pubsub shared solace-pubsub-sample-instance

### Building

Just clone and build. For example:

1. clone this GitHub repository
1. `./gradlew build`

### Deploying

To deploy the individual applications to Cloud Foundry:

1. cd to the project directory (`java-app` or `spring-cloud`)
1. `$ cf push`

## Java Application

application name: `solace-sample-java-app`

This application uses the Java library from http://www.JSON.org/ to parse the `VCAP_SERVICES` environment variable to determine the connection details for Solace PubSub+. For more details and example usage, see the walk through tutorial here:

* [Online Tutorial](https://dev.solace.com/samples/solace-samples-cloudfoundry-java/java-app/)

## Java Application using Spring Cloud Connector

application name: `solace-sample-spring-cloud`

This application makes use of the Spring Cloud Connectors project to automatically parse the `VCAP_SERVICES` environment variable. Applications do *not* have to be a Spring Boot application to make use of Spring Cloud Connectors. This example makes use of Spring Boot for convenience in enabling the simple REST API. In any Java Applications, simply specify the following dependencies in your build:

	compile 'org.springframework.cloud:spring-cloud-spring-service-connector:1.2.3.RELEASE'
	compile 'org.springframework.cloud:spring-cloud-cloudfoundry-connector:1.2.3.RELEASE'
	compile 'com.solace.cloud.cloudfoundry:solace-spring-cloud-connector:2.1.+'

The `solace-spring-cloud-connector` is a Spring Cloud Connectors extension to parse the `VCAP_SERVICES` for the Solace PubSub+ service instance information. Check out the project page for more details:

* https://github.com/SolaceProducts/solace-spring-cloud/tree/master/solace-spring-cloud-connector

The easiest way for applications to access the SolaceServiceCredentials object is by Service Id (ex: "MyService) as follows:

	CloudFactory cloudFactory = new CloudFactory();
	Cloud cloud = cloudFactory.getCloud();
	SolaceServiceCredentials solaceServiceCredentials = (SolaceServiceCredentials) cloud.getServiceInfo("MyService");

Alternatively applications could search through the environment and discover matching services as follows:

	SolaceServiceCredentials solaceServiceCredentials = null;
	List<ServiceInfo> services = cloud.getServiceInfos();

	// Connect to the first Solace PubSub+ service that is found in the services list.
	for (ServiceInfo service : services) {
		if (service instanceof SolaceServiceCredentials) {
			solaceServiceCredentials = (SolaceServiceCredentials)service;
			break;
		}
	}

For more details and example usage, see the walk through tutorial here:

* [Online Tutorial](https://dev.solace.com/samples/solace-samples-cloudfoundry-java/spring-cloud/)

## Java Application using Java CFEnv

application name: `solace-sample-spring-cloud-java-cfenv`

This application makes use of the Java CFEnv project to automatically parse the `VCAP_SERVICES` environment variable. In any Java Applications, simply specify the following dependencies in your build:

    compile 'com.solace.cloud.cloudfoundry:solace-java-cfenv'

The `solace-java-cfenv` is the successor to `solace-spring-cloud-connector` due to the deprecation of Spring Cloud Connectors. It is a Java CFEnv extension to parse the `VCAP_SERVICES` for the Solace PubSub+ service instance information. Check out the project page for more details:

* https://github.com/SolaceProducts/solace-spring-boot/tree/master/solace-java-cfenv

To access the SolaceServiceCredentials object, applications must retrieve it in a manner similar to the following:

	SolaceServiceCredentials solaceServiceCredentials = null;
	List<SolaceServiceCredentials> solaceServiceCredentialsList = SolaceServiceCredentialsFactory.getAllFromCloudFoundry();

	// Connect to the first Solace PubSub+ service that is found in the services list.
	if (solaceServiceCredentialsList.size() > 0) {
	    solaceServiceCredentials = solaceServiceCredentialsList.get(0);
	}

## Secure Session

application name: `solace-sample-secure-session`

This application is based on the [Solace Java CFEnv](https://github.com/SolaceProducts/solace-spring-boot/tree/master/solace-java-cfenv) described above, but shows how to use
Transport Level Security (TLS) between the Java application and the Solace PubSub+ Service Instance.

* [Online Tutorial](https://dev.solace.com/samples/solace-samples-cloudfoundry-java/secure-session/)

## LDAP

This is not a standalone application, but instead a modification to the existing sample apps.

If application access is enabled by the cloud operator, bindings will not contain application access credentials and the credentials will instead have to be provided to the application externally.

This manifests as the service instance owner having to manually configure LDAP authorization groups for application access.

* [Online Tutorial](https://dev.solace.com/samples/solace-samples-cloudfoundry-java/ldap/)

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

Unsubscribe the application from topic "test"

    curl -X DELETE http://$APP_URL/subscription/test

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct, and the process for submitting pull requests to us.

## Authors

See the list of [contributors](https://github.com/SolaceSamples/solace-samples-cloudfoundry-java/contributors) who participated in this project.

## License

This project is licensed under the Apache License, Version 2.0. - See the [LICENSE](LICENSE) file for details.

## Resources

For more information try these resources:


- [Tutorials](https://tutorials.solace.dev/)
- The Solace Developer Portal website at: http://dev.solace.com
- Get a better understanding of [Solace technology](http://dev.solace.com/tech/).
- Check out the [Solace blog](http://dev.solace.com/blog/) for other interesting discussions around Solace technology
- Ask the [Solace community.](http://dev.solace.com/community/)
