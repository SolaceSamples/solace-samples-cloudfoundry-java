package com.solace.samples.cloudfoundry.springcloud.controller;

import javax.jms.ConnectionFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

@Configuration
public class ProducerConfiguration {

	@Autowired
	private ConnectionFactory connectionFactory;

	// Example configuration of JmsTemplate
	@Bean
	public JmsTemplate jmsTemplate() {
		CachingConnectionFactory ccf = new CachingConnectionFactory(connectionFactory);
		JmsTemplate jmst = new JmsTemplate(ccf);
		jmst.setPubSubDomain(true);	// This sample is publishing to topics
		return jmst;
	}
}
