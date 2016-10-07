---
layout: tutorials
title: Spring Cloud
summary: A Simple Spring Cloud Application showing how to consume the VCAP_SERVICES using Spring Cloud Connectors.
icon: spring-cloud.png
---

* [Overview](#overview)
* [Goals](#goals)
* [Assumptions](#assumptions)
* [Obtaining the Solace API](#obtaining-the-solace-api)
* [Code Walk Through](#code-walk-through)
* [Building](#building)
* [Cloud Foundry Setup](#cloud-foundry-setup)
* [Deploying](#deploying)
* [Trying Out the Application](#trying-out-the-application)

## Overview

This tutorial is part of a series of tutorials which aims to introduce users to Solace Messaging in Pivotal Cloud Foundry. Solace Messaging in Pivotal Cloud Foundry is delivered as a Tile on the [Pivotal Network](https://network.pivotal.io/){:target="_blank"}. You can see the [Solace Messaging for Pivotal Cloud Foundry Documentation](http://docs.pivotal.io/solace-messaging/){:target="_blank"} for full details.

This tutorial is similar to the [Java App]({{ site.baseurl }}/java-app) tutorial. Like the Java App tutorial, it will introduce you to Solace Messaging for Pivotal Cloud Foundry by creating a Java application.  In contrast to the Java App, this application uses the Spring Cloud Connectors library to simplify the parsing of the `VCAP_SERVICES` environment variable. 

![overview]({{ site.baseurl }}/images/spring-cloud-app-architecture.png){: .center-image}

## Goals

The goal of this tutorial is to demonstrate extracting the information from the application's Cloud Foundry Service Bindings and connect to the Solace Messaging service instance.  This tutorial will show you:

1. How to extract the Solace Messaging service credentials from the Cloud Foundry environment using Spring Cloud Connectors.
1. How to establish a connection to the Solace Messaging service.
1. How to publish, subscribe and receive messages.

## Assumptions

This tutorial assumes the following:

* You are familiar with Solace [core concepts](http://dev.solacesystems.com/docs/core-concepts/){:target="_top"}.
* You are familiar with [Spring RESTful Web Services](https://spring.io/guides/gs/rest-service/){:target="_blank"}.
* You are familiar with [Cloud Foundry](https://www.cloudfoundry.org/){:target="_blank"}.
* You have access to a running Pivotal Cloud Foundry environment.
* Solace Messaging for PCF has been installed in your Pivotal Cloud Foundry environment.

## Obtaining the Solace API

This tutorial depends on you having the Solace Messaging API for Java (JCSMP). Here are a few easy ways to get the Java API. The instructions in the [Building](#building) section assume you're using Gradle and pulling the jars from maven central. If your environment differs then adjust the build instructions appropriately.

### Get the API: Using Gradle

```
compile("com.solacesystems:sol-jcsmp:{{ site.jcsmp_version }}")
```

### Get the API: Using Maven

```
<dependency>
  <groupId>com.solacesystems</groupId>
  <artifactId>sol-jcsmp</artifactId>
  <version>{{ site.jcsmp_version }}</version>
</dependency>
```

### Get the API: Using the Solace Developer Portal

The Java API library can be [downloaded here](http://dev.solacesystems.com/downloads/){:target="_top"}. The Java API is distributed as a zip file containing the required jars, API documentation, and examples. 

## Code Walk Through

This section will explain what the code in the samples does.

### Structure

The sample application contains the following source files :

| Source File      | Description |
| ---------------- | ----------- |
| Application.java | The Sprint Boot application class |
| SolaceController.java | The Application's REST controller which provides an interface to subscribe, publish and receive messages.  This class also implements the initialization procedure which connects the application to the Solace Messaging Service. |
| SimpleMessage.java | This class wraps the information to be stored in a message |
| SimpleSubscription.java | This class wraps the information describing a topic subscription |

This tutorial will only cover the source code in `SolaceController.java` as the other files do not contain logic related to establishing a connection to the Solace Messaging Service.

### Obtaining the Solace Credentials in the Application

The Pivotal Cloud Foundry environment exposes any bound Service Instances in a JSON document stored in the `VCAP_SERVICES` environment variable.  Here is an example of a VCAP_SERVICES will all the fields of interest to us : 

```
{
  "VCAP_SERVICES": {
    "solace-messaging": [ {
        "name": "solace-messaging-sample-instance",
        "label": "solace-messaging",
        "plan": "shared",
        "tags": [
            (...)
            ],
        "credentials": {
          "clientUsername": "v005.cu000001",
          "clientPassword": "bb90fcb0-6c83-4a10-bafa-3ec225bbfc08",
          "msgVpnName": "v005",
            (...)
          "smfHost": "tcp://192.168.132.14:7000",
            (...)
        }
      }
    }
  ]
}
```

You can see the full structure of the Solace Messaging `VCAP_SERVICES` in the Solace Messaging for PCF documentation [here](https://solacedev.github.io/pcf/docs/vcap_services/){:target="_blank"}.

This sample use the [Spring Cloud Connectors](http://cloud.spring.io/spring-cloud-connectors/){:target="_blank"} project to retrieve the `VCAP_SERVICES` information in the form of a `ServiceInfo` object. For Solace Messaging in Cloud Foundry, there is a Spring Cloud Connectors extension in GitHub called [sl-spring-cloud-connectors](https://github.com/SolaceLabs/sl-spring-cloud-connectors). This library parses the `VCAP_SERVICES` environment variable and exposes the information by implementing the `ServiceInfo` interface in the `SolaceMessagingInfo` object.

The `init()` method retrieves the Solace Messaging Service Instance details for the service named `solace-messaging-sample-instance` as follows:

```java
CloudFactory cloudFactory = new CloudFactory();
Cloud cloud = cloudFactory.getCloud();

SolaceMessagingInfo solaceMessagingServiceInfo = (SolaceMessagingInfo) cloud
        .getServiceInfo("solace-messaging-sample-instance");

if (solaceMessagingServiceInfo == null) {
    trace.error("Did not find instance of 'solace-messaging' service");
    trace.error("************* Aborting Solace initialization!! ************");
    return;
}
```

### Connecting to the Solace Messaging Service

Once you have the `SolaceMessagingInfo`, you can create and then connect the Solace Session in the conventional way as outlined in the [Publish/Subscribe tutorial](http://dev.solacesystems.com/get-started/java-tutorials/publish-subscribe_java/){:target="_top"}. You set the JCSMP properties and then use the `JCSMPFactory` to create a `Session`:

```java
final JCSMPProperties properties = new JCSMPProperties();
properties.setProperty(JCSMPProperties.HOST, solaceMessagingServiceInfo.getSmfHost());
properties.setProperty(JCSMPProperties.VPN_NAME, solaceMessagingServiceInfo.getMsgVpnName());
properties.setProperty(JCSMPProperties.USERNAME, solaceMessagingServiceInfo.getClientUsername());
properties.setProperty(JCSMPProperties.PASSWORD, solaceMessagingServiceInfo.getClientPassword());

try {
    session = JCSMPFactory.onlyInstance().createSession(properties);
    session.connect();
} catch (Exception e) {
    logger.error("Error connecting and setting up session.", e);
    logger.info("************* Aborting Solace initialization!! ************");
    return;
}
```

With the Solace session connected. The remainder of this tutorial is exactly the same as the [Java App]({{ site.baseurl }}/java-app) tutorial. The details are repeated here for convenience.

### Creating the Message Consumer and Producer

To receive and send messages you will need to create a consumer and a producer by using the connected session. The following code will create a simple message producer that is silent normally but will log any errors it receives:

```java
private class SimplePublisherEventHandler implements JCSMPStreamingPublishEventHandler {
    @Override
    public void responseReceived(String messageID) {
        logger.info("Producer received response for msg: " + messageID);
    }

    @Override
    public void handleError(String messageID, JCSMPException e, long timestamp) {
        logger.error("Producer received error for msg: " + messageID + " - " + timestamp, e);
    }
};
producer = session.getMessageProducer(new SimplePublisherEventHandler());
```

The following code will create a simple message consumer that will log any incoming messages and errors:

```java
private class SimpleMessageListener implements XMLMessageListener {
    @Override
    public void onReceive(BytesXMLMessage receivedMessage) {
        numMessagesReceived.incrementAndGet();

        if (receivedMessage instanceof TextMessage) {
            lastReceivedMessage = (TextMessage) receivedMessage;
            logger.info("Received message : " + lastReceivedMessage.getText());
        } else {
            logger.error("Received message that was not a TextMessage: " + receivedMessage.dump());
        }
    }

    @Override
    public void onException(JCSMPException e) {
        logger.error("Consumer received exception: %s%n", e);
    }
};
final XMLMessageConsumer cons = session.getMessageConsumer(new SimpleMessageListener());
cons.start();
```

### Publishing, Subscribing and Receiving Messages

The consumer created in the previous step will only receive messages matching topics that the Solace session subscribed to. It is thus necessary to create subscriptions in order to receive messages.  You can add a topic subscription by sending a `POST` to the `/subscription` REST endpoint.  The payload of the `POST` is a simple JSON structure containing the topic subscripion. For example: `{"subscription": "test"}`. Here is the method signature:

```java
@RequestMapping(value = "/subscription", method = RequestMethod.POST)
public ResponseEntity<String> addSubscription(@RequestBody SimpleSubscription subscription) {
    // ...
}
```

You can send a message by sending a `POST` to the `/message` REST endpoint.  The payload of the `POST` is a simple JSON structure containing the topic for publishing and the message contents. For example: `{"topic": "test", "body": "Test Message"}`. Here is the method signature:

```java
@RequestMapping(value = "/message", method = RequestMethod.POST)
public ResponseEntity<String> sendMessage(@RequestBody SimpleMessage message) {
   // ...
}
```

Receiving messages is done at the backend via the `SimpleMessageListener` listener described above.  This sample stores the last message received. To access ths received message you can send a `GET` request to `/message` endpoint. The same JSON structure of a message will be returned in the payload of the `GET`.
 
```java
@RequestMapping(value = "/message", method = RequestMethod.GET)
public ResponseEntity<SimpleMessage> getLastMessageReceived() {
    // ...
}
```

The subscription JSON document used by the `/subscription` endpoint is modeled by the `SimpleSubscription` class, whereas the `/message` endpoint JSON document is modeled by the `SimpleMessage` class.

For more details on sending and receiving messages, you can checkout the [JCSMP Publish/Subscribe tutorial](http://dev.solacesystems.com/get-started/java-tutorials/publish-subscribe_java/){:target="_top"}.

## Building

The full source code for this example is available in [GitHub]({{ site.repository }}){:target="_blank"}. Start by cloning the repository.

Here is an example:

```
git clone {{ site.repository }}
cd {{ site.baseurl | remove: '/'}} 
```

Then aquire the Solace Java API. Until the Solace Java API is available in Maven Central, you can [download the Java API here](http://dev.solacesystems.com/downloads/){:target="_top"}. Unpack the API and then copy the contents of the sol-jcsmp-VERSION/lib directory to a `libs` sub-directory in your `{{ site.baseurl | remove: '/'}}` project.

For example in the following command line replace VERSION with the Solace Java API version you downloaded:

1. `mkdir libs`
1. `cp ../sol-jcsmp-VERSION/lib/sol*.jar libs`

Then build the sample applications:

```
./gradlew build
```

## Cloud Foundry Setup

The sample application specifies a dependency on a service instance named `solace-messaging-sample-instance` in its manifiest (See `spring-cloud/manifest.yml`).  This must be an instance of the Solace Messaging Service which can be created with this command:

```
cf create-service solace-messaging shared solace-messaging-sample-instance
```

## Deploying

To deploy this tutorial's application you first need to go inside it's project directory and then push the application:

```
cd spring-cloud
cf push
```

This will push the application and will give the application the name specified by the manifest: `solace-sample-spring-cloud`.

## Trying Out The Application

As described above, the sample application has a simple REST interface that allows you to:

* Subscribe
* Send a message
* Receive a message

In order to interact with the application you need to determine the application's URL.  These shell commands can be used to quickly find out the URL:

```
export APP_NAME=solace-sample-spring-cloud
export APP_URL=`cf apps | grep $APP_NAME | grep started | awk '{ print $6}'`
echo "The application URL is: ${APP_URL}"
```

To demonstrate the application we will make the application send a message to itself.  Then we will read the message back to confirm the successful delivery of the message :

```
# Subscribes the application to the topic "test"
curl -X POST -H "Content-Type: application/json;charset=UTF-8" -d '{"subscription": "test"}' http://$APP_URL/subscription

# Send message with topic "test" and this content: "TEST_MESSAGE"
curl -X POST -H "Content-Type: application/json;charset=UTF-8" -d '{"topic": "test", "body": "Test Message"}' http://$APP_URL/message

# The message should have been asynchronously received by the application.  Check that the message was indeed received:
curl -X GET http://$APP_URL/message
```
