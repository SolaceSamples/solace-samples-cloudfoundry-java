/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.solace.samples.cloudfoundry.javaapp.controller;

import com.solace.samples.cloudfoundry.javaapp.model.SimpleMessage;
import com.solace.samples.cloudfoundry.javaapp.model.SimpleSubscription;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class SolaceController {

    private static final Log logger = LogFactory.getLog(SolaceController.class);

    private SimpleMessage lastReceivedMessage;
    private SimpleMqttCallback simpleMqttCallback = new SimpleMqttCallback();

    // Stats
    private final AtomicInteger numMessagesReceived = new AtomicInteger();
    private final AtomicInteger numMessagesSent = new AtomicInteger();

    private MqttClient mqttClient;

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

    @PostConstruct
    public void init() {

        // Connect to Solace
        logger.info("************* Init Called ************");

        // Look for Service Keys Data..

        String serviceKey = System.getenv("SERVICE_KEY");

        logger.info(serviceKey);

        if (serviceKey == null || serviceKey.equals("") || serviceKey.equals("{}")) {
            logger.error("The SERVICE_KEY variable wasn't set in the environment. Aborting connection.");
            logger.info("************* Aborting Solace initialization!! ************");
            return;
        }

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

		mqttClient.setCallback(simpleMqttCallback);

		try {
			mqttClient.connect(connOpts);
		} catch (MqttException e) {
			logger.error("Unable to connecting using the MqttClient and its connection options. Aborting connection.",e);
	        logger.info("************* Aborting Solace initialization!! ************");
	        return;
		}

    }

    @RequestMapping(value = "/message", method = RequestMethod.POST)
    public ResponseEntity<String> sendMessage(@RequestBody SimpleMessage message) {

    	if( mqttClient == null )
    		return new ResponseEntity<>("{'description': 'Unable to perform operation, the MqttClient was not initialized'}", HttpStatus.INTERNAL_SERVER_ERROR);

    	if (!mqttClient.isConnected()) {
			logger.error("mqttClient was not connected, Could not send message");
			return new ResponseEntity<>("{'description': 'Unable to perform operation, the MqttClient was not connected!'}", HttpStatus.INTERNAL_SERVER_ERROR);
		}

		logger.info(
				"Sending message on topic: " + message.getTopic() + " with body: " + message.getBody());

		try {
			MqttMessage mqttMessage = new MqttMessage(message.getBody().getBytes());
			mqttMessage.setQos(0);
			mqttClient.publish(message.getTopic(), mqttMessage);
			numMessagesSent.incrementAndGet();
		} catch (MqttException e) {
			logger.error("sendMessage failed.", e);
            return new ResponseEntity<>("{'description': '" + e.getMessage() + "'}", HttpStatus.BAD_REQUEST);
		}
        return new ResponseEntity<>("{'description': 'Message sent on topic " + message.getTopic() + "'}", HttpStatus.OK);
    }

    @RequestMapping(value = "/message", method = RequestMethod.GET)
    public ResponseEntity<SimpleMessage> getLastMessageReceived() {

        if (lastReceivedMessage != null) {
            logger.info("Sending the lastReceivedMessage");
            // Return the last received message if it exists.
            return new ResponseEntity<>(lastReceivedMessage, HttpStatus.OK);
        } else {
            logger.info("Sorry did not find a lastReceivedMessage");
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

    }

    @RequestMapping(value = "/subscription", method = RequestMethod.POST)
    public ResponseEntity<String> addSubscription(@RequestBody SimpleSubscription subscription) {
        String subscriptionTopic = subscription.getSubscription();
        logger.info("Adding a subscription to topic: " + subscriptionTopic);

        try {
        	if( mqttClient != null )
        		mqttClient.subscribe(subscriptionTopic);
        	else
        		return new ResponseEntity<>("{'description': 'Unable to perform operation, the MqttClient was not initialized'}", HttpStatus.INTERNAL_SERVER_ERROR);
            logger.info("Finishing Adding a subscription to topic: " + subscriptionTopic);
		} catch (MqttException e) {
			logger.error("Subscription Creation failed.", e);
			return new ResponseEntity<>("{'description': '" + e.getMessage() + "'}", HttpStatus.BAD_REQUEST);
		}
        return new ResponseEntity<>("{}", HttpStatus.OK);
    }

    @Deprecated
    @RequestMapping(value = "/subscription", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteSubscription(@RequestBody SimpleSubscription subscription) {
		return deleteSubscription(subscription.getSubscription());
	}

	@RequestMapping(value = "/subscription/{subscriptionName}", method = RequestMethod.DELETE)
	public ResponseEntity<String> deleteSubscription(@PathVariable("subscriptionName") String subscriptionTopic) {

        logger.info("Deleting a subscription to topic: " + subscriptionTopic);
		try {
            if( mqttClient != null )
        		mqttClient.unsubscribe(subscriptionTopic);
        	else
        		return new ResponseEntity<>("{'description': 'Unable to perform operation, the MqttClient was not initialized'}", HttpStatus.INTERNAL_SERVER_ERROR);
            logger.info("Finished Deleting a subscription to topic: " + subscriptionTopic);
		} catch (MqttException e) {
			logger.error("removeSubscription failed.", e);
			return new ResponseEntity<>("{'description': '" + e.getMessage() + "'}", HttpStatus.BAD_REQUEST);
		}

        return new ResponseEntity<>("{}", HttpStatus.OK);
    }

    @RequestMapping(value = "/status", method = RequestMethod.GET)
    public ResponseEntity<String> getStatus() {

        logger.info("Received request for getStatus");

        JSONObject statusJson = new JSONObject();
        statusJson.put("numMsgsSent", numMessagesSent.get());
        statusJson.put("numMsgsReceived", numMessagesReceived.get());
        return new ResponseEntity<>(statusJson.toString(), HttpStatus.OK);
    }

    @RequestMapping(value = "/status", method = RequestMethod.DELETE)
    public ResponseEntity<String> resetStats() {
        numMessagesReceived.set(0);
        numMessagesSent.set(0);
        lastReceivedMessage = null;
        return new ResponseEntity<>("{}", HttpStatus.OK);
    }

}
