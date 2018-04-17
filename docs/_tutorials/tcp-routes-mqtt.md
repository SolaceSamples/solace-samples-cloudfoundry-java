---
layout: tutorials
title: TCP Routes for IoT - MQTT Java Application
summary: A Simple Application showing how to consume a SERVICE KEY for Solace Messaging when using TCP Routes.
icon: I_mqtt_iot.svg
---

## Overview

This tutorial is part of a series of tutorials which aims to introduce users to Solace Messaging in Pivotal Cloud Foundry. Solace Messaging in Pivotal Cloud Foundry is delivered as a Tile on the [Pivotal Network]({{ site.links-ext-pivotal }}){:target="_blank"}. You can see the [Solace Messaging for Pivotal Cloud Foundry Documentation]({{ site.links-ext-pivotal-solace }}){:target="_blank"} for full details.

This tutorial will introduce you to Solace Messaging for Pivotal Cloud Foundry by creating a Java application which connects to a Solace Messaging service with enabled TCP Routes as trivial example of an IoT use case.
TCP Routes is a feature that allows a Solace Messaging service hosted in a Pivotal Cloud Foundry deployment to become accessible "outside" the PCF domain.


![overview]({{ site.baseurl }}/images/tcp-routes-architecture.png){: .center-image}

## Goals

The goal of this tutorial is to demonstrate how an application can use Cloud Foundry provided [Service Keys]({{ site.links-ext-service-keys }}){:target="_blank"} for a Solace Messaging [Service Instance]({{ site.links-ext-service-instances }}){:target="_blank"}. Using [Service Keys]({{ site.links-ext-service-keys }}){:target="_blank"} an application running "outside" of Cloud Foundry can obtain [credentials]({{ site.links-ext-credentials-servicekey-example }}){:target="_blank"} to access a Solace Messaging [Service Instance]({{ site.links-ext-service-instances }}){:target="_blank"}. An application running "outside" of Cloud Foundry should use [Service Keys]({{ site.links-ext-service-keys }}){:target="_blank"} and [TCP Routes]({{ site.links-ext-pivotal-tcp-routes }}){:target="_blank"}{:target="_blank"}.

This tutorial will show you:

1. How to get the Solace Messaging service credentials as service keys for a given Solace Messaging service in the the Cloud Foundry environment.
1. How to make the Solace Messaging service credentials as service keys accessible to an application in or out of a Cloud Foundry environment.
1. How to establish a connection to the Solace Messaging service.
1. How to publish, subscribe and receive messages.

## Assumptions

This tutorial assumes the following:

* You are familiar with Solace [core concepts]({{ site.docs-core-concepts }}).
* You are familiar with [Spring RESTful Web Services]({{ site.links-ext-spring-rest }}){:target="_blank"}.
* You are familiar with [Cloud Foundry]({{ site.links-ext-cloudfoundry }}){:target="_blank"}.
* You have access to a running Pivotal Cloud Foundry environment.
* You are familiar with Using Solace Messaging [Service Instances]({{ site.links-ext-service-instances }}){:target="_blank"} and [Service Keys]({{ site.links-ext-service-keys }}){:target="_blank"}
* Solace Messaging for PCF has been installed in your Pivotal Cloud Foundry environment having enabled [TCP Routes]({{ site.links-ext-pivotal-tcp-routes }}){:target="_blank"}{:target="_blank"} with 'MQTT / Plain-Text' set to "Enabled by default" or "Disabled by default"

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

### Obtaining the Solace Credentials from a service key in the Application

The Pivotal Cloud Foundry environment can provide a service key for a given Service instance. It is the responsibility of the user to pass the service key details to a running application.
Here is an example of a SERVICE_KEY with all the fields of interest to us, notice the "public*" ports:

```
{
  'clientPassword': '10170ae9-2faa-4a7b-9161-152c9d32cc05',
  'clientUsername': 'v005.cu000066',
  'msgVpnName': 'v005',
  'managementHostnames': [ 'shared-vmr-0.local.pcfdev.io' ],
  'managementPassword': 'e30a4c0c506a42a696204f3fa83d9fd3',
  'managementUsername': 'v005-mgmt',
  'publicMqttTlsUris': [ 'ssl://tcp.local.pcfdev.io:61049' ],
  'publicMqttUris': [ 'tcp://tcp.local.pcfdev.io:61070' ],
  (...)
}
```

You can find out from the [Solace Messaging for PCF documentation Service Key example]({{ site.links-ext-credentials-servicekey-example }}){:target="_blank"}.

The sample starts by extracting the SERVICE_KEY environment variable, logging its content and confirming it contains useful information.  This is done in the `init()` method:

```java
String serviceKey = System.getenv("SERVICE_KEY");

logger.info(serviceKey);

if (serviceKey == null || serviceKey.equals("") || serviceKey.equals("{}")) {
	logger.error("The SERVICE_KEY variable wasn't set in the environment. Aborting connection.");
	logger.info("************* Aborting Solace initialization!! ************");
	return;
}
```

Given the `SERVICE_KEY` was available and is a valid JSON, the application confirms the presence of the required fields to support creating an MQTT connection.


```java
JSONObject solaceCredentials = null;
try {
	solaceCredentials = new JSONObject(serviceKey);
} catch(JSONException e) {
	logger.error("Unable to read the SERVICE_KEY content as a JSON structure. Aborting connection.",e);
	logger.info("************* Aborting Solace initialization!! ************");
	return;
}

logger.info("Solace client initializing and using Credentials: " + solaceCredentials.toString(2));

if( ! solaceCredentials.has("publicMqttUris") ) {
	logger.error("Unable to find publicMqttUris in the SERVICE_KEY. Aborting connection.");
	logger.info("************* Aborting Solace initialization!! ************");
	return;
}

if( ! solaceCredentials.has("clientUsername") ) {
	logger.error("Unable to find clientUsername in the SERVICE_KEY. Aborting connection.");
	logger.info("************* Aborting Solace initialization!! ************");
	return;
}

if( ! solaceCredentials.has("clientPassword") ) {
	logger.error("Unable to find clientPassword in the SERVICE_KEY. Aborting connection.");
	logger.info("************* Aborting Solace initialization!! ************");
	return;
}

```


Get the mqtt server uris into an String array to be used by the MQTT client.


```java
String[] mqttServerURIs = null;
try {
	JSONArray hostsArray = solaceCredentials.getJSONArray("publicMqttUris");

	if( hostsArray == null || hostsArray.length() == 0  ) {
		logger.error("Did not find any entries in the  publicMqttUris array from the SERVICE_KEY. Aborting connection.");
		logger.info("************* Aborting Solace initialization!! ************");
		return;
	}

	mqttServerURIs = new String[hostsArray.length()];
	for(int i=0; i < hostsArray.length(); i++) {
		mqttServerURIs[i] = hostsArray.getString(i);
	}
} catch(JSONException e) {
	logger.error("Unable to read publicMqttUris array from the SERVICE_KEY. Aborting connection.",e);
	logger.info("************* Aborting Solace initialization!! ************");
	return;
}

```


### Connecting to the Solace Messaging Service

Once the credentials are extracted, you can create and then connect the Solace Session in the conventional way as outlined in the [MQTT Publish/Subscribe tutorial]({{ site.links-mqtt-pubsub-tutorial }}).

Create an mqtt client and connection properties.


```java

// Create a client using the first server URL, and random client Id.
try {
	mqttClient = new MqttClient(mqttServerURIs[0], UUID.randomUUID().toString());
} catch (MqttException e) {
	logger.error("Unable to create an MqttClient. Aborting connection.",e);
	logger.info("************* Aborting Solace initialization!! ************");
	return;
}


MqttConnectOptions connOpts = new MqttConnectOptions();
connOpts.setServerURIs(mqttServerURIs);
connOpts.setUserName(solaceCredentials.getString("clientUsername"));
connOpts.setPassword(solaceCredentials.getString("clientPassword").toCharArray());

```

### Creating a Message Consumer

To receive messages you will need to create callback. The following code will create a simple MqttCallback that will keep the last recieved message and log other events:

```java
   private SimpleMqttCallback simpleMqttCallback = new SimpleMqttCallback();

   class SimpleMqttCallback implements MqttCallback {

                @Override
                public void connectionLost(Throwable cause) {
                        logger.error("connectionLost", cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                        logger.info("Received message : " + message);
                        numMessagesReceived.incrementAndGet();
                        synchronized (simpleMqttCallback) {
                                lastReceivedMessage = new SimpleMessage();
                                lastReceivedMessage.setTopic(topic);
                                lastReceivedMessage.setBody(new String(message.getPayload()));
                        }
                        logger.info("Received message kept: " + lastReceivedMessage);

                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                        logger.info("deliveryComplete: " + token);
                }
        }
```

### Establishing a connection and setting the MqttCallback


```java

mqttClient.setCallback(simpleMqttCallback);

try {
	mqttClient.connect(connOpts);
} catch (MqttException e) {
	logger.error("Unable to connecting using the MqttClient and its connection options. Aborting connection.",e);
	logger.info("************* Aborting Solace initialization!! ************");
	return;
}

```

### Sending messages


Using the mqttClient we can send a message on a given topic

```java
try {
	MqttMessage mqttMessage = new MqttMessage(message.getBody().getBytes());
	mqttMessage.setQos(0);
	mqttClient.publish(message.getTopic(), mqttMessage);
	numMessagesSent.incrementAndGet();
} catch (MqttException e) {
	logger.error("sendMessage failed.", e);
	return new ResponseEntity<>("{'description': '" + e.getMessage() + "'}", HttpStatus.BAD_REQUEST);
}

```

### Publishing, Subscribing and Receiving Messages

The simpleMqttCallback created in the previous step will only receive messages matching topics that the Solace mqtt connection is subscribed to. It is thus necessary to create subscriptions in order to receive messages.  You can add a topic subscription by sending a `POST` to the `/subscription` REST endpoint.  The payload of the `POST` is a simple JSON structure containing the topic subscripion. For example: `{"subscription": "test"}`. Here is the method signature:

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

Receiving messages is done at the backend via the `simpleMqttCallback` listener described above.  This sample stores the last message received. To access ths received message you can send a `GET` request to `/message` endpoint. The same JSON structure of a message will be returned in the payload of the `GET`.

```java
@RequestMapping(value = "/message", method = RequestMethod.GET)
public ResponseEntity<SimpleMessage> getLastMessageReceived() {
    // ...
}
```

The subscription JSON document used by the `/subscription` endpoint is modeled by the `SimpleSubscription` class, whereas the `/message` endpoint JSON document is modeled by the `SimpleMessage` class.

For more details on sending and receiving messages, you can checkout the [MQTT Publish/Subscribe tutorial]({{ site.links-mqtt-pubsub-tutorial }}).

## Building

The full source code for this example is available in [GitHub]({{ site.repository }}){:target="_blank"}. To build, just clone and use gradle. Here is an example:

```
git clone {{ site.repository }}
cd {{ site.baseurl | remove: '/'}}
./gradlew build
```

## Cloud Foundry Setup

The sample application has a dependency on a `SERVICE_KEY` which needs to be satified by the user. A service key can be created for an existing Solace Messaging Service.

### Creating the service

Assuming TCP Routes is enabled and MQTT Plain-Text is set to "Enabled by default", you can create your service like so:

```
cf create-service solace-messaging shared solace-messaging-sample-instance
```

Assuming TCP Routes is enabled and MQTT Plain-Text is set to "Disabled by default", you can request MQTT be enabled upon service creation like so:

```
cf create-service solace-messaging shared solace-messaging-sample-instance -c '{ "mqtt_tcp_route_enabled" : "true" }'

```

### Creating the service key


```
cf create-service-key solace-messaging-sample-instance solace-messaging-sample-service-key
cf service-key solace-messaging-sample-instance solace-messaging-sample-service-key
```

## Getting and passing the service key to an application

The service key details can be obtained from cf and should be passed to an application somehow.
When you have access to cf, get the service key and save it to a file so you may keep it and send it to where you plan on running your application.

```
cf service-key solace-messaging-sample-instance solace-messaging-sample-service-key | grep -v Getting > solace-messaging-sample-service.key
```

You need to get the file 'solace-messaging-sample-service.key' to where your application will be running somehow.

Assuming you have the file now accessible where will run your application, load it in an environment variable we have chosen and named SERVICE_KEY.
Load the service key from file to the SERVICE_KEY as an environment variable:

```
export SERVICE_KEY=$( cat solace-messaging-sample-service.key )
```

## Running

This application can be used Standalone or deployed in PCF.

### Option 1 - Run the application in Standalone mode

Running the application in a standalone mode and providing it with the necessary service key to access the Solace Messaging service.
Assumes you a saved service key in file 'solace-messaging-sample-service.key'

```
export SERVICE_KEY=$( cat solace-messaging-sample-service.key )
cd tcp-routes-mqtt
java -Dserver.port=8080 -jar build/libs/solace-sample-java-app.jar
```

At this point the application is running and using port 8080 providing the services that it is supposed to so we can continue testing the messaging features. Please note that you will need to open another window to continue the remaining parts of the tutorial.
Assumes you a saved service key in file 'solace-messaging-sample-service.key'

### Option 2 - To run in PCF

If running in PCF keep in mind that the idea is that the application is connecting to an external port created by TCP Routes, and that the necessary credentials are passed to this application by the user.

```
export SERVICE_KEY=$( cat solace-messaging-sample-service.key )
cd tcp-routes-mqtt
cf push
cf set-env solace-sample-java-app SERVICE_KEY "$SERVICE_KEY"
cf restage solace-sample-java-app
```

This will save the service key in an environment variable called SERVICE_KEY, will push the application given the name specified by the manifest: `solace-sample-java-app`, then provide the SERVICE_KEY as an environment variable to the application and restage it so it has access to the SERVICE_KEY.


## Trying Out the Application

As described above, the sample application has a simple REST interface that allows you to:

* Subscribe
* Send a message
* Receive a message
* Unsubscribe

In order to interact with the application you need to determine the application's URL.

If you ran the application in Standalone mode (notice the matching 8080 port):

```
export APP_URL="localhost:8080/"
echo "The application URL is: ${APP_URL}"
```

If you have deployed your app in PCF, these shell commands can be used to quickly find out the URL:

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

