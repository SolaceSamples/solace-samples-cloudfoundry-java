---
layout: docs
title: Java Application
---

This tutorial will introduce you to Solace Messaging for Pivotal Cloud Foundry by creating a Java application which
connects to a Solace Messaging service instance.

# Assumptions

This tutorial assumes the following:

* You are familiar with Solace [core concepts](http://dev.solacesystems.com/docs/core-concepts/).
* You are familiar with [Spring RESTful Web Services](https://spring.io/guides/gs/rest-service/). 
* You have access to a running Pivotal Cloud Foundry environment.
* Solace Messaging for PCF have been installed in your Pivotal Cloud Foundry environment.
 
# Goals

The goal of this tutorial is to demonstrate extracting the information from the Application Service Binding and connect
to the Solace Messaging service instance.  This tutorial will show you:

1. How to extract the Solace Messaging service credentials from the Cloud Foundry environment.
1. How to establish a connection to the Solace Messaging service.
1. How to publish, subscribe and receive messages.

# Obtaining the Solace API

This tutorial depends on you having the Java API downloaded and available. The Java API library can be 
[downloaded here](http://dev.solacesystems.com/downloads/). The Java API is distributed as a zip file containing the
required jars, API documentation, and examples. The instructions in this tutorial assume you have downloaded the
Java API library and unpacked it to a known location. If your environment differs then adjust the build instructions
appropriately.

## Unpacking the Solace API

Follow these steps to unpack the Solace API:

1. Create a directory called ``libs`` into the sample root directory.
1. Unzip the Solace API file ``sol-jcsmp-<version>.zip``.
1. Copy the jar files contained into the zip file's ``lib`` directory into the sample's ``libs`` directory.

Example:

```
unzip sol-jcsmp-7.1.2.248.zip
git clone https://github.com/SolaceSamples/solace-samples-cloudfoundry-java
cd solace-samples-cloudfoundry-java
mkdir libs
cp ../sol-jcsmp-7.1.2.248/lib/*jar libs
```

# Code walk through

This section will explain what the code in the samples does.

## Structure

The sample application contains the following source files :

| Source File      | Description |
| ---------------- | ----------- |
| Application.java | The Sprint Boot application class |
| SolaceController.java | The Application's REST controller.  This class also implements the initialization procedure which connects the application to the Solace Messaging Service and subscribes to a topic to receive messages. |
| SimpleMessage.java | This class wraps the information to be stored in a message |
| SimpleSubscription.java | This class wraps the information describing a topic subscription |

This tutorial will only cover the source code in ``SolaceController.java`` as the other files contains no logic that
pertains to establishing a connection to the Solace Messaging Service.

## Obtaining the Solace credentials in the application

The environment exposes the bound Service Instances in a JSON document stored in the ``VCAP_SERVICES`` environment
variable.  The sample starts by extracting the JSON document from this environment variable, logging its content and
confirming it contains useful information.  This is done in the init() method:

```
String vcapServices = System.getenv("VCAP_SERVICES");
logger.info(vcapServices);

if (vcapServices == null || vcapServices.equals("") || vcapServices.equals("{}")) {
    logger.error("The VCAP_SERVICES variable wasn't set in the environment. Aborting connection.");
    return;
}

JSONObject vcapServicesJson = new JSONObject(vcapServices);
```

Given the JSON object structure documented [here](https://solacedev.github.io/pcf/docs/vcap_services/), the application
needs to extract the Messaging Service URI and the credentials of the Solace Messaging service instance.  The following
code will iterate through the array of Service Instances bound to the application and search for an instance of 
``solace-messaging``.  If such an instance is found, the credentials JSON object is extracted :

```
JSONArray solMessagingArray = vcapServicesJson.getJSONArray("solace-messaging");

if (solMessagingArray == null) {
    logger.error("Did not find Solace provided messaging service \"solace-messaging\"");
    logger.info("************* Aborting Solace initialization!! ************");
    return;
}

logger.info("Number of provided bindings: " + solMessagingArray.length());

// Get the first Solace credentials from the array
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

## Connecting to the Solace Messaging Service

Once the credentials are extracted, you can create and then connect the Solace Session in the conventional way as
outlined in the
[Publish/Subscribe tutorial](http://dev.solacesystems.com/get-started/java-tutorials/publish-subscribe_java/).
The JCSMP properties must be set, from which a Session is created :

```
final JCSMPProperties properties = new JCSMPProperties();
properties.setProperty(JCSMPProperties.HOST, solaceCredentials.getString("smfUri"));
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

## Creating the message consumer and producer

To receive and send messages you will need to create a consumer and a producer by using the connected session :

```
try {
    final XMLMessageConsumer cons = session.getMessageConsumer(new SimpleMessageListener());
    cons.start();

    producer = session.getMessageProducer(new SimplePublisherEventHandler());

    logger.info("************* Solace initialized correctly!! ************");
} catch (Exception e) {
    logger.error("Error creating the consumer and producer.", e);
}
```

The listener that was passed to the consumer in the code above simply logs incoming messages, and retain the 
last received message.  It also logs asynchronous exceptions :

```
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
}
```

The handler that was passed to the producer simply logs any events produced by the publisher object :

```
private class SimplePublisherEventHandler implements JCSMPStreamingPublishEventHandler {
    @Override
    public void responseReceived(String messageID) {
        logger.info("Producer received response for msg: " + messageID);
    }

    @Override
    public void handleError(String messageID, JCSMPException e, long timestamp) {
        logger.error("Producer received error for msg: " + messageID + " - " + timestamp, e);
    }

}
```

## Publishing, Subscribing and Receiving Messages

The consumer created in the previous step will only receive messages matching topics that the session did subscribe to.
It is thus necessary to create subscriptions in order to receive messages.  This will be done from the ``/subscription``
REST endpoint's ``POST`` method which accepts a subscription parameter :

```
@RequestMapping(value = "/subscription", method = RequestMethod.POST)
public ResponseEntity<String> addSubscription(@RequestBody SimpleSubscription subscription) {
    String subscriptionTopic = subscription.getSubscription();
    logger.info("Adding a subscription to topic: " + subscriptionTopic);

    final Topic topic = JCSMPFactory.onlyInstance().createTopic(subscriptionTopic);
    try {
        boolean waitForConfirm = true;
        session.addSubscription(topic, waitForConfirm);
    } catch (JCSMPException e) {
        logger.error("Adding a subscription failed.", e);
        return new ResponseEntity<>("{'description': '" + e.getMessage() + "'}", HttpStatus.BAD_REQUEST);
    }
    logger.info("Finished Adding a subscription to topic: " + subscriptionTopic);
    return new ResponseEntity<>("{}", HttpStatus.OK);
}
```

Sending messages will be done via another REST endpoint ``/message`` with the ``POST`` method.  The method accepts 
the message to send as parameter.  The message also contains an attribute which specifies which topic it belongs to.
Here is the implementation of this method :

```
@RequestMapping(value = "/message", method = RequestMethod.POST)
public ResponseEntity<String> sendMessage(@RequestBody SimpleMessage message) {

    if (session == null || session.isClosed()) {
        logger.error("Session was null or closed, Could not send message");
        return new ResponseEntity<>("{'description': 'Somehow the session is not connected, please see logs'}",
                HttpStatus.BAD_REQUEST);
    }

    logger.info("Sending message on topic: " + message.getTopic() + " with body: " + message.getBody());

    final Topic topic = JCSMPFactory.onlyInstance().createTopic(message.getTopic());
    TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
    msg.setText(message.getBody());
    try {
        producer.send(msg, topic);
        numMessagesSent.incrementAndGet();

    } catch (JCSMPException e) {
        logger.error("Sending message failed.", e);
        return new ResponseEntity<>("{'description': '" + e.getMessage() + "'}", HttpStatus.BAD_REQUEST);
    }
    return new ResponseEntity<>("{}", HttpStatus.OK);
}
```

Receiving messages is done at the backend via the ``SimpleMessageListener`` listener described above.  To access the
last message received by the backend, a ``GET`` method at the ``/message`` endpoint must be added :
 
```
@RequestMapping(value = "/message", method = RequestMethod.GET)
public ResponseEntity<SimpleMessage> getLastMessageReceived() {

    if (lastReceivedMessage != null) {
        logger.info("Sending the lastReceivedMessage");

        // Return the last received message if it exists.
        SimpleMessage receivedMessage = new SimpleMessage();

        receivedMessage.setTopic(lastReceivedMessage.getDestination().getName());
        receivedMessage.setBody(lastReceivedMessage.getText());
        return new ResponseEntity<>(receivedMessage, HttpStatus.OK);
    } else {
        logger.info("Sorry did not find a lastReceivedMessage");
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

}
```

For further information on the subject of sending and receiving messages please consult the
[Publish/Subscribe tutorial](http://dev.solacesystems.com/get-started/java-tutorials/publish-subscribe_java/).