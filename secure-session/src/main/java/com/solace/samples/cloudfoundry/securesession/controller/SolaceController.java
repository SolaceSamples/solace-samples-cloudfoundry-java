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
package com.solace.samples.cloudfoundry.securesession.controller;

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
import com.solace.samples.cloudfoundry.securesession.model.SimpleMessage;
import com.solace.samples.cloudfoundry.securesession.model.SimpleSubscription;
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
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

@RestController
public class SolaceController {

    private static final Log logger = LogFactory.getLog(SolaceController.class);

<<<<<<< HEAD
=======
    // This determines whether we validate the certificate. 
    // In production systems this should be set to true.
    private static final boolean VALIDATE_CERTIFICATE = false;

>>>>>>> Improvements to the secure-session tutorial based on Mark Spielman's second review.
    // If true, we will install a certificate residing in the src/main/resources directory
    // so that we can validate self-signed certificates.
    private static final boolean INSTALL_CERTIFICATE = false;

    // Change this to match the file in src/main/resources
    private static final String CERTIFICATE_FILE_NAME = "my-cert.cer";

    // Each certificate in the trusted store needs to have a unique alias.
    private static final String CERTIFICATE_ALIAS = "my-alias";

    // Path to the jre trusted store, when this is deployed in Cloud Foundry.
    private static final String TRUST_STORE = "/home/vcap/app/.java-buildpack/open_jdk_jre/lib/security/cacerts";

    // Standard default password for the trust store
    private static final String TRUST_STORE_PASSWORD = "changeit";

    private JCSMPSession session;
    private XMLMessageProducer producer;
    private TextMessage lastReceivedMessage;

    // Stats
    private final AtomicInteger numMessagesReceived = new AtomicInteger();
    private final AtomicInteger numMessagesSent = new AtomicInteger();

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

    @PostConstruct
    public void init() {

        // Connect to Solace
        logger.info("************* Init Called ************");

        // Import the certificate on the JRE packaged with the Cloud Foundry
        // application. See the function definition below for details.
        if (INSTALL_CERTIFICATE) {
            try {
                importCertificate();
            } catch (Exception ex) {
                logger.error("Installation of the certificate failed.", ex);
                logger.info("************* Aborting Solace initialization!! ************");
                return;
            }
        }

        CloudFactory cloudFactory = new CloudFactory();
        Cloud cloud = cloudFactory.getCloud();

        SolaceMessagingInfo solaceMessagingServiceInfo
                = (SolaceMessagingInfo) cloud.getServiceInfo("solace-messaging-sample-instance");

        if (solaceMessagingServiceInfo == null) {
            logger.error("Did not find instance of 'solace-messaging' service");
            logger.info("************* Aborting Solace initialization!! ************");
            return;
        }

        logger.info("Solace client initializing and using SolaceMessagingInfo: " + solaceMessagingServiceInfo);

        final JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST, solaceMessagingServiceInfo.getSmfTlsHost());
        properties.setProperty(JCSMPProperties.VPN_NAME, solaceMessagingServiceInfo.getMsgVpnName());
        properties.setProperty(JCSMPProperties.USERNAME, solaceMessagingServiceInfo.getClientUsername());
        properties.setProperty(JCSMPProperties.PASSWORD, solaceMessagingServiceInfo.getClientPassword());
        properties.setProperty(JCSMPProperties.SSL_VALIDATE_CERTIFICATE, true);
        properties.setProperty(JCSMPProperties.SSL_VALIDATE_CERTIFICATE_DATE, true);
        properties.setProperty(JCSMPProperties.SSL_TRUST_STORE, TRUST_STORE);
        properties.setProperty(JCSMPProperties.SSL_TRUST_STORE_PASSWORD, TRUST_STORE_PASSWORD);

        try {
            session = JCSMPFactory.onlyInstance().createSession(properties);
            session.connect();
        } catch (Exception e) {
            logger.error("Error connecting and setting up session.", e);
            logger.info("************* Aborting Solace initialization!! ************");
            return;
        }

        try {
            final XMLMessageConsumer cons = session.getMessageConsumer(new SimpleMessageListener());
            cons.start();

            producer = session.getMessageProducer(new SimplePublisherEventHandler());

            logger.info("************* Solace initialized correctly!! ************");
        } catch (Exception e) {
            logger.error("Error creating the consumer and producer.", e);
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
<<<<<<< HEAD
            logger.error("Message post failed.", e);
            return handleError(e);
=======
            logger.error("Service Creation failed.", e);
            return new ResponseEntity<>("{'description': '" + e.getMessage() + "'}", HttpStatus.BAD_REQUEST);
>>>>>>> Improvements to the secure-session tutorial based on Mark Spielman's second review.
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
<<<<<<< HEAD
            logger.error("Subscription delete failed.", e);
            return handleError(e);
=======
            logger.error("Service Creation failed.", e);
            return new ResponseEntity<>("{'description': '" + e.getMessage() + "'}", HttpStatus.BAD_REQUEST);
>>>>>>> Improvements to the secure-session tutorial based on Mark Spielman's second review.
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
<<<<<<< HEAD
            logger.error("Subscription delete failed.", e);
            return handleError(e);
=======
            logger.error("Service Creation failed.", e);
            return new ResponseEntity<>("{'description': '" + e.getMessage() + "'}", HttpStatus.BAD_REQUEST);
>>>>>>> Improvements to the secure-session tutorial based on Mark Spielman's second review.
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

    /**
<<<<<<< HEAD
     * This formats a string showing the exception class name and message,
     * as well as the class name and message of the underlying cause
     * if it exists.
     * Then it returns that string in a ResponseEntity.
     * @param exception
     * @return ResponseEntity<String>
     */
    private ResponseEntity<String> handleError(Exception exception) {
        
        Throwable cause = exception.getCause();
        String causeString = "";

        if (cause != null) {
            causeString = "Cause: " + cause.getClass() + ": " + cause.getMessage();
        }

        String desc = String.format("{'description': ' %s: %s %s'}", 
                exception.getClass().toString(), exception.getMessage(), causeString);
        return new ResponseEntity<>(desc, HttpStatus.BAD_REQUEST);

    }

    /**
     * This utility function installs a certificate into the JRE's trusted
     * store. Normally you would not do this, but this is provided to
     * demonstrate how to use TLS, and have the client validate a self-signed
     * server certificate.
     *
     * @throws Exception
     */
    private static void importCertificate() throws Exception {

        File file = new File(CERTIFICATE_FILE_NAME);
        logger.info("Loading certificate from " + file.getAbsolutePath());

=======
     * This utility function installs a certificate into the JRE's 
     * trusted store. Normally you would not do this, but this is provided
     * to demonstrate how to use TLS, and have the client validate a
     * self-signed server certificate.
     * 
     * @throws Exception 
     */
    public static void importCertificate() throws Exception {

        File file = new File(CERTIFICATE_FILE_NAME);
        logger.info("Loading certificate from " + file.getAbsolutePath());
        
>>>>>>> Improvements to the secure-session tutorial based on Mark Spielman's second review.
        // This loads the KeyStore from the default location 
        // (i.e. default for a Clound Foundry app) using the default password.
        FileInputStream is = new FileInputStream(TRUST_STORE);
        char[] password = TRUST_STORE_PASSWORD.toCharArray();
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(is, password);
        is.close();

<<<<<<< HEAD
=======

>>>>>>> Improvements to the secure-session tutorial based on Mark Spielman's second review.
        // Create an ByteArrayInputStream stream from the 
        FileInputStream fis = new FileInputStream(CERTIFICATE_FILE_NAME);
        DataInputStream dis = new DataInputStream(fis);
        byte[] bytes = new byte[dis.available()];
        dis.readFully(bytes);
        ByteArrayInputStream certstream = new ByteArrayInputStream(bytes);
<<<<<<< HEAD

        // This takes that Byte Array and creates a certificate out of it.
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate certs = cf.generateCertificate(certstream);

=======
        
        // This takes that Byte Array and creates a certificate out of it.
        CertificateFactory cf = CertificateFactory.getInstance("X.509");        
        Certificate certs = cf.generateCertificate(certstream);
        
>>>>>>> Improvements to the secure-session tutorial based on Mark Spielman's second review.
        // Finally, store the new certificate in the keystore.
        keystore.setCertificateEntry(CERTIFICATE_ALIAS, certs);

        // Save the new keystore contents
        FileOutputStream out = new FileOutputStream(TRUST_STORE);
        keystore.store(out, password);
        out.close();

    }

}
