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

package com.solace.samples.cloudfoundry.springcloud.controller;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.solace.labs.spring.cloud.core.SolaceMessagingInfo;
import com.solace.samples.cloudfoundry.springcloud.model.SimpleMessage;
import com.solace.samples.cloudfoundry.springcloud.model.SimpleSubscription;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPStreamingPublishEventHandler;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.XMLMessageListener;
import com.solacesystems.jcsmp.XMLMessageProducer;

@RestController
public class SolaceController {

	private static final Log logger = LogFactory.getLog(SolaceController.class);

	JCSMPSession session;
	XMLMessageProducer producer;
	TextMessage lastReceivedMessage;

	// Stats
	AtomicInteger numMessagesReceived = new AtomicInteger();
	AtomicInteger numMessagesSent = new AtomicInteger();

	class SimplePublisherEventHandler implements JCSMPStreamingPublishEventHandler {
		@Override
		public void responseReceived(String messageID) {
			logger.info("Producer received response for msg: " + messageID);
		}

		@Override
		public void handleError(String messageID, JCSMPException e, long timestamp) {
			logger.error("Producer received error for msg: " + messageID + " - " + timestamp, e);
		}

	};

	class SimpleMessageListener implements XMLMessageListener {

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

	@PostConstruct
	public void init() {

		// Connect to Solace
		logger.info("************* Init Called ************");

		CloudFactory cloudFactory = new CloudFactory();
		Cloud cloud = cloudFactory.getCloud();
		SolaceMessagingInfo solacemessaging = (SolaceMessagingInfo) cloud.getServiceInfo("solace-messaging-sample-instance");
		
		logger.info("Solace client initializing and using SolaceMessagingInfo: " + solacemessaging);

		final JCSMPProperties properties = new JCSMPProperties();
		properties.setProperty(JCSMPProperties.HOST, solacemessaging.getSmfUri());
		properties.setProperty(JCSMPProperties.VPN_NAME, solacemessaging.getMsgVpnName());
		properties.setProperty(JCSMPProperties.USERNAME, solacemessaging.getClientUsername());
		properties.setProperty(JCSMPProperties.PASSWORD, solacemessaging.getClientPassword());
		
		try {
			session = JCSMPFactory.onlyInstance().createSession(properties);
			session.connect();

			final XMLMessageConsumer cons = session.getMessageConsumer(new SimpleMessageListener());
			cons.start();

			producer = session.getMessageProducer(new SimplePublisherEventHandler());

			logger.info("************* Solace initialized correctly!! ************");

		} catch (Exception e) {
			logger.error("Error connecting and setting up session.", e);
		}
	}

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
			logger.error("Service Creation failed.", e);
			return new ResponseEntity<>("{'description': '" + e.getMessage() + "'}", HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>("{}", HttpStatus.OK);
	}

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

	@RequestMapping(value = "/subscription", method = RequestMethod.POST)
	public ResponseEntity<String> addSubscription(@RequestBody SimpleSubscription subscription) {
		String subscriptionTopic = subscription.getSubscription();
		logger.info("Adding a subscription to topic: " + subscriptionTopic);

		final Topic topic = JCSMPFactory.onlyInstance().createTopic(subscriptionTopic);
		try {
			boolean waitForConfirm = true;
			session.addSubscription(topic, waitForConfirm);
		} catch (JCSMPException e) {
			logger.error("Service Creation failed.", e);
			return new ResponseEntity<>("{'description': '" + e.getMessage() + "'}", HttpStatus.BAD_REQUEST);
		}
		logger.info("Finished Adding a subscription to topic: " + subscriptionTopic);
		return new ResponseEntity<>("{}", HttpStatus.OK);
	}

	@RequestMapping(value = "/subscription", method = RequestMethod.DELETE)
	public ResponseEntity<String> deleteSubscription(@RequestBody SimpleSubscription subscription) {
		String subscriptionTopic = subscription.getSubscription();
		final Topic topic = JCSMPFactory.onlyInstance().createTopic(subscriptionTopic);
		logger.info("Deleting a subscription to topic: " + subscriptionTopic);

		try {
			boolean waitForConfirm = true;
			session.removeSubscription(topic, waitForConfirm);
		} catch (JCSMPException e) {
			logger.error("Service Creation failed.", e);
			return new ResponseEntity<>("{'description': '" + e.getMessage() + "'}", HttpStatus.BAD_REQUEST);
		}
		logger.info("Finished Deleting a subscription to topic: " + subscriptionTopic);
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
