---
layout: tutorials
title: Java Application
summary: A Java Application showing how to consume the VCAP_SERVICES environment variable.
icon: I_java.svg
---

## Overview

This tutorial is part of a series of tutorials which aims to introduce users to Solace Messaging in Pivotal Cloud Foundry. Solace Messaging in Pivotal Cloud Foundry is delivered as a Tile on the [Pivotal Network]({{ site.links-ext-pivotal }}){:target="_blank"}. You can see the [Solace Messaging for Pivotal Cloud Foundry Documentation]({{ site.links-ext-pivotal-solace }}){:target="_blank"} for full details.

This tutorial will introduce you to Solace Messaging for Pivotal Cloud Foundry by creating a Java application which connects to a Solace Messaging service instance.

![overview]({{ site.baseurl }}/images/java-app-architecture.png){: .center-image}

## Goals

The goal of this tutorial is to demonstrate extracting the information from the application's Cloud Foundry Service Bindings and connect to the Solace Messaging service instance.  This tutorial will show you:

1. How to extract the Solace Messaging service credentials from the Cloud Foundry environment.
1. How to establish a connection to the Solace Messaging service.
1. How to publish, subscribe and receive messages.

## Assumptions

This tutorial assumes the following:

* You are familiar with Solace [core concepts]({{ site.links-docs-core-concepts }}).
* You are familiar with [Spring RESTful Web Services]({{ site.links-ext-spring-rest }}){:target="_blank"}.
* You are familiar with [Cloud Foundry]({{ site.links-ext-cloudfoundry }}){:target="_blank"}.
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

The Java API library can be [downloaded here]({{ site.links-downloads }}). The Java API is distributed as a zip file containing the required jars, API documentation, and examples.

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

The Pivotal Cloud Foundry environment exposes any bound Service Instances in a JSON document stored in the `VCAP_SERVICES` environment variable.  Here is an example of a VCAP_SERVICES with all the fields of interest to us:

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
          "smfHosts": [ "tcp://192.168.132.14:7000" ],
            (...)
        }
      }
    }
  ]
}
```

You can see the full structure of the Solace Messaging `VCAP_SERVICES` in the [Solace Messaging for PCF documentation]({{ site.links-ext-vcap }}){:target="_blank"}.

The sample starts by extracting the JSON document from this environment variable, logging its content and confirming it contains useful information.  This is done in the `init()` method:

```java
String vcapServices = System.getenv("VCAP_SERVICES");
logger.info(vcapServices);

if (vcapServices == null || vcapServices.equals("") || vcapServices.equals("{}")) {
    logger.error("The VCAP_SERVICES variable wasn't set in the environment. Aborting connection.");
    return;
}

JSONObject vcapServicesJson = new JSONObject(vcapServices);
```

Given the `VCAP_SERVICES` JSON, the application needs to extract the Solace Messaging Service URI and the credentials of the Solace Messaging service instance.  The following code extract an instance of `solace-messaging` from the array of Service Instances bound to the application.  If such an instance is found, the credentials JSON object is extracted :

```java
JSONArray solMessagingArray = vcapServicesJson.getJSONArray("solace-messaging");

if (solMessagingArray == null) {
    logger.error("Did not find Solace provided messaging service \"solace-messaging\"");
    logger.info("************* Aborting Solace initialization!! ************");
    return;
}

logger.info("Number of provided bindings: " + solMessagingArray.length());

// Get the Solace credentials from the first binding
JSONObject solaceCredentials = null;
if (solMessagingArray.length() > 0) {
    solaceCredentials = solMessagingArray.getJSONObject(0);
    if (solaceCredentials != null) {
        solaceCredentials = solaceCredentials.getJSONObject("credentials");
    }
}

// Check whether the credentials were succesfully extracted
if (solaceCredentials == null) {
    logger.error("Did not find Solace messaging service credentials");
    logger.info("************* Aborting Solace initialization!! ************");
    return;
}

logger.info("Solace client initializing and using Credentials: " + solaceCredentials.toString(2));
```

### Connecting to the Solace Messaging Service

Once the credentials are extracted, you can create and then connect the Solace Session in the conventional way as outlined in the [Publish/Subscribe tutorial]({{ site.links-pubsub-tutorial }}). You set the JCSMP properties and then use the `JCSMPFactory` to create a `Session`:

```java

// The host property is in a json array. Two hosts are provided in a High Availability
// environment, one for the primary router and one for the backup.
JSONArray hostsArray = solaceCredentials.getJSONArray("smfHosts");
String host = hostsArray.getString(0);

final JCSMPProperties properties = new JCSMPProperties();
properties.setProperty(JCSMPProperties.HOST, host);
properties.setProperty(JCSMPProperties.VPN_NAME, solaceCredentials.getString("msgVpnName"));
properties.setProperty(JCSMPProperties.USERNAME, solaceCredentials.getString("clientUsername"));
properties.setProperty(JCSMPProperties.PASSWORD, solaceCredentials.getString("clientPassword"));

try {
    session = JCSMPFactory.onlyInstance().createSession(properties);
    session.connect();
} catch (Exception e) {
    logger.error("Error connecting and setting up session.", e);
    logger.info("************* Aborting Solace initialization!! ************");
    return;
}
```

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

For more details on sending and receiving messages, you can checkout the [JCSMP Publish/Subscribe tutorial]({{ site.links-pubsub-tutorial }}).

## Building

The full source code for this example is available in [GitHub]({{ site.repository }}){:target="_blank"}. To build, just clone and use gradle. Here is an example:

```
git clone {{ site.repository }}
cd {{ site.baseurl | remove: '/'}}
./gradlew build
```

## Cloud Foundry Setup

The sample application specifies a dependency on a service instance named `solace-messaging-sample-instance` in its manifiest (See `java-app/manifest.yml`).  This must be an instance of the Solace Messaging Service which can be created with this command:

```
cf create-service solace-messaging shared solace-messaging-sample-instance
```

## Deploying

To deploy this tutorial's application you first need to go inside it's project directory and then push the application:

```
cd java-app
cf push
```

This will push the application and will give the application the name specified by the manifest: `solace-sample-java-app`.

## Trying Out the Application

As described above, the sample application has a simple REST interface that allows you to:

* Subscribe
* Send a message
* Receive a message
* Unsubscribe

In order to interact with the application you need to determine the application's URL.  These shell commands can be used to quickly find out the URL:

```
export APP_NAME=solace-sample-java-app
export APP_URL=`cf apps | grep $APP_NAME | grep started | awk '{ print $6}'`
echo "The application URL is: ${APP_URL}"
```

To demonstrate Solace messaging, the application will send a message to itself. Then we will read the message back to confirm the successful delivery of the message:

```
# Subscribes the application to the topic "test"
curl -X POST -H "Content-Type: application/json;charset=UTF-8" -d '{"subscription": "test"}' http://$APP_URL/subscription

# Send message with topic "test" and this content: "TEST_MESSAGE"
curl -X POST -H "Content-Type: application/json;charset=UTF-8" -d '{"topic": "test", "body": "Test Message"}' http://$APP_URL/message

# The message should have been asynchronously received by the application.  Check that the message was indeed received:
curl -X GET http://$APP_URL/message

# Unsubscribe the application from the topic "test"
curl -X DELETE http://$APP_URL/subscription/test
```
## High Availability

Solace provides the option to set up High Availability configurations, where each message router has a backup. To use this in Cloud Foundry you need to create a service of type medium-ha or large-ha, for example:

```
cf create-service solace-messaging medium-ha solace-messaging-sample-instance
```

The Solace Java API automatically switches to the backup connection if it loses the connection to the primary router.

When using a High Availability service, the environment provided to the applications contains two connection urls rather than one. The first is the primary router and the second is the backup, for example:

```
"smfHosts": [ "tcp://192.168.101.16:7000", "tcp://192.168.101.17:7000" ]
```

In order to provide both hosts to the Solace Java API, you need to create a string containing a comma-separated list of the two hosts:

```
// The host property is in a json array. Two hosts are provided in a High Availability
// environment, one for the primary router and one for the backup.
JSONArray hostsArray = solaceCredentials.getJSONArray("smfHosts");
String primary = hostsArray.getString(0);
String backup = hostsArray.getString(1);
String hosts = String.join(",", primary, backup);
properties.setProperty(JCSMPProperties.HOST, hosts);
```
When you do the Spring Cloud tutorial, you will see that the Solace Spring Cloud Connector takes care of this for you.

Please consult the [Configuring Connections]({{ site.links-configuring-connection }}) page.

As described in that document, when using High Availability it is recommended to set the following configuration:

```
JCSMPProperties properties = new JCSMPProperties();
JCSMPChannelProperties cp = (JCSMPChannelProperties) properties
    .getProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES);
cp.setConnectRetries(1);
cp.setReconnectRetries(5);
cp.setReconnectRetryWaitInMillis(3000);
cp.setConnectRetriesPerHost(20);
```

