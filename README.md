<<<<<<< HEAD
# solace-samples-cloudfoundry-java

The repository contains example applications that use the Solace Messaging service on Pivotal Cloud Foundry. The goal of these sample applications is to illustrate various ways of consuming the `VCAP_SERVICES` environment variable from a Solace Messaging Cloud Foundry service instance. You can get more details on the Solace Messaging Service for Pivotal Cloud Foundry [here](http://docs.pivotal.io/solace-messaging/).

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
=======
# Solace Samples Template

This project is the common template from which all Solace Samples are merged. When creating a new Solace sample, you should fork this repo and then create your sample. Template updates can be applied by merging from this repo on both the `master` and `gh-pages` branch. The `gh-pages` branch is setup and ready to be used to create tutorials. See the [README](https://github.com/SolaceSamples/solace-samples-template/blob/gh-pages/README.md) in that branch for details. Any code and description in the samples should not overlap with this template.

## Instructions (to be deleted in a Solace Samples project)

Here are some instructions once you've forked the repository and are creating new Solace samples.

1. Update the repository links in [](CONTRIBUTING.md)
2. Add your Samples source code to the master branch
3. Update this README with instructions on how to build and run.
4. Create walk through tutorials in the docs directory. Specifically by modifying `_config.yml`, `_data/tutorials.yml`, and `_tutorials/...` 

To merge changes to a Samples project from the template, you would use the following commands:

    git remote add samples-template https://github.com/SolaceSamples/solace-samples-template.git
    git fetch samples-template
    git merge samples-template/gh-pages
    git remote remove samples-template

Below this are common sections that should appear in all Solace Samples README.md. Leave them! :)

## Using Eclipse

To generate Eclipse metadata (.classpath and .project files), do the following:

    ./gradlew eclipse

Once complete, you may then import the projects into Eclipse as usual:

 *File -> Import -> Existing projects into workspace*

Browse to the *'solace-samples-java'* root directory. All projects should import
free of errors.

## Using IntelliJ IDEA

To generate IDEA metadata (.iml and .ipr files), do the following:

    ./gradlew idea
>>>>>>> samples-template/master

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct, and the process for submitting pull requests to us.

## Authors

<<<<<<< HEAD
See the list of [contributors](https://github.com/SolaceSamples/solace-samples-cloudfoundry-java/contributors) who participated in this project.
=======
See the list of [contributors](https://github.com/SolaceSamples/solace-samples-java/contributors) who participated in this project.
>>>>>>> samples-template/master

## License

This project is licensed under the Apache License, Version 2.0. - See the [LICENSE](LICENSE) file for details.
<<<<<<< HEAD
=======

## Resources

For more information try these resources:

- The Solace Developer Portal website at:
[http://dev.solacesystems.com](http://dev.solacesystems.com/)
- Get a better understanding of [Solace technology.](http://dev.solacesystems.com/tech/)
- Check out the [Solace blog](http://dev.solacesystems.com/blog/) for other interesting discussions around Solace technology
- Ask the [Solace community.](http://dev.solacesystems.com/community/)
>>>>>>> samples-template/master
