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

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.destination.JndiDestinationResolver;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import com.solace.samples.cloudfoundry.springcloud.model.SimpleMessage;
import com.solace.samples.cloudfoundry.springcloud.model.SimpleSubscription;
import com.solace.spring.cloud.core.SolaceMessagingInfo;
import com.solacesystems.jms.SpringSolJmsJndiTemplateCloudFactory;

@RestController
public class SolaceController {

	private static final Log logger = LogFactory.getLog(SolaceController.class);

	@Autowired
	private JndiDestinationResolver jndiDestinationResolver;

	@Autowired
	private JndiObjectFactoryBean connectionFactory;

    @Autowired
    private JmsTemplate jmsTemplate;

	// The auto selected Solace Messaging service
	// the relevant information provided by this bean have already been injected
	// into the JndiTemplate
	// This bean is for information only, it can be used to discover more about
	// the solace service in use.
	@Autowired
	private SolaceMessagingInfo solaceMessagingInfo;

	// A Factory of Factories
	// Has the ability to create JndiTemplate(s) for all available
	// SolaceMessagingInfo(s)
	// Can be used in case there are multiple Solace Messaging Services to
	// select from.
	@Autowired
	SpringSolJmsJndiTemplateCloudFactory springSolJmsJndiTemplateCloudFactory;

	private TextMessage lastReceivedMessage;
    private HashMap<String, DefaultMessageListenerContainer> listenerContainersMap = new HashMap<String, DefaultMessageListenerContainer>();

	// Stats
	private final AtomicInteger numMessagesReceived = new AtomicInteger();
	private final AtomicInteger numMessagesSent = new AtomicInteger();

    // JMS Message listener helpers
	
	public class SimpleMessageListener implements MessageListener {
		@Override
        public void onMessage(Message message) {

			numMessagesReceived.incrementAndGet();

			if (message instanceof TextMessage) {
				lastReceivedMessage = (TextMessage) message;
				try {
					logger.info("Received message : " + lastReceivedMessage.getText());
				} catch (JMSException e) {
					logger.error("Error getting text of the received TextMessage: " + e);
				}
			} else {
				logger.error("Received message that was not a TextMessage: " + message);
			}
        }
    }

    // Create a listener explicitly, runtime
    public DefaultMessageListenerContainer createListener(String destination) {
        // do something here to create a message listener container
        DefaultMessageListenerContainer lc = new DefaultMessageListenerContainer();
        lc.setConnectionFactory((ConnectionFactory) connectionFactory.getObject());
        lc.setDestinationResolver(jndiDestinationResolver);
        lc.setDestinationName(destination);
        lc.setMessageListener(new SimpleMessageListener());
        lc.setPubSubDomain(true);
        lc.initialize();
        return lc;
    }

    @PostConstruct
	public void init() {

		// Show available services
		logger.info("************* Init Called ************");

		logger.info(String.format("SpringSolJmsJndiTemplateCloudFactory discovered %s solace-messaging service(s)",
				springSolJmsJndiTemplateCloudFactory.getSolaceMessagingInfos().size()));

		// Log what Solace Messaging Services were discovered
		for (SolaceMessagingInfo discoveredSolaceMessagingService : springSolJmsJndiTemplateCloudFactory
				.getSolaceMessagingInfos()) {
			logger.info(String.format(
					"Discovered Solace Messaging service '%s': HighAvailability? ( %s ), Message VPN ( %s )",
					discoveredSolaceMessagingService.getId(), discoveredSolaceMessagingService.isHA(),
					discoveredSolaceMessagingService.getMsgVpnName()));
		}

	}
    
    // REST services

	@RequestMapping(value = "/message", method = RequestMethod.POST)
	public ResponseEntity<String> sendMessage(@RequestBody SimpleMessage message) {

		logger.info("Sending message on topic: " + message.getTopic() + " with body: " + message.getBody());
		try {
			this.jmsTemplate.convertAndSend(message.getTopic(), message.getBody());
			numMessagesSent.incrementAndGet();
			
		} catch (Exception e) {
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

			try {
				receivedMessage.setTopic(lastReceivedMessage.getJMSDestination().toString());
				receivedMessage.setBody(lastReceivedMessage.getText());
			} catch (JMSException e) {
				logger.error("Unable to extract message details.", e);
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
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

		if ( this.listenerContainersMap.containsKey(subscriptionTopic) ) {
			// Already subscribed
			logger.error("Already subscribed to topic " + subscriptionTopic);
			return new ResponseEntity<>("{'description': 'Already subscribed'}", HttpStatus.BAD_REQUEST);
		}
		
		// Then create a listener
		try {
	    	DefaultMessageListenerContainer listenercontainer = createListener(subscriptionTopic);
	        listenercontainer.start();
	        this.listenerContainersMap.put(subscriptionTopic, listenercontainer);
		} catch (Exception e) {
			logger.error("Service Creation failed.", e);
			return new ResponseEntity<>("{'description': '" + e.getMessage() + "'}", HttpStatus.BAD_REQUEST);
		}
		logger.info("Finished Adding a subscription to topic: " + subscriptionTopic);
		return new ResponseEntity<>("{}", HttpStatus.OK);
	}

	@RequestMapping(value = "/subscription", method = RequestMethod.DELETE)
	public ResponseEntity<String> deleteSubscription(@RequestBody SimpleSubscription subscription) {
		String subscriptionTopic = subscription.getSubscription();
		logger.info("Deleting a subscription to topic: " + subscriptionTopic);

		if ( !this.listenerContainersMap.containsKey(subscriptionTopic) ) {
			// Not subscribed
			logger.error("Not subscribed to topic " + subscriptionTopic);
			return new ResponseEntity<>("{'description': 'Was not subscribed'}", HttpStatus.BAD_REQUEST);
		}
		
		try {
			DefaultMessageListenerContainer listenercontainer = this.listenerContainersMap.get(subscriptionTopic);
			listenercontainer.stop();
	        listenercontainer.destroy();
	        this.listenerContainersMap.remove(subscriptionTopic);

		} catch (Exception e) {
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
