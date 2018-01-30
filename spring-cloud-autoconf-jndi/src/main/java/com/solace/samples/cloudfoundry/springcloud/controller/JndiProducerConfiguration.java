package com.solace.samples.cloudfoundry.springcloud.controller;

import javax.jms.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.destination.JndiDestinationResolver;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.jndi.JndiTemplate;

@Configuration
public class JndiProducerConfiguration {

    // Resource definitions: connection factory and queue destination
    @Value("${solace.jms.demoConnectionFactoryJndiName}")
    private String connectionFactoryJndiName;


    // Use from the jndi connection config
    @Autowired
	private JndiTemplate jndiTemplate;
    
//	// TODO: fix it that this shouldn't be required (transfer to SempJndiConfigClient
//	@Autowired
//    private RestTemplateBuilder restTemplateBuilder;
//	@Autowired
//	private SolaceMessagingInfo solaceMessagingInfo;

	@Bean
    public JndiObjectFactoryBean connectionFactory() {
        JndiObjectFactoryBean factoryBean = new JndiObjectFactoryBean();
        factoryBean.setJndiTemplate(jndiTemplate);

//        // First ensure the JNDI config exists on the message router
//        SempJndiConfigClient scc = new SempJndiConfigClient(restTemplateBuilder, solaceMessagingInfo);
//        scc.provisionJndiConnectionFactoryIfNotExists(connectionFactoryJndiName);
        factoryBean.setJndiName(connectionFactoryJndiName);
        
        return factoryBean;
    }

    @Bean
	public CachingConnectionFactory cachingConnectionFactory() {
		CachingConnectionFactory ccf = new CachingConnectionFactory((ConnectionFactory) connectionFactory().getObject());
		ccf.setSessionCacheSize(10);
		return ccf;
	}

    // DynamicDestinationResolver can be used instead for physical, non-jndi destinations
    @Bean
    public JndiDestinationResolver jndiDestinationResolver() {
    	JndiDestinationResolver jdr = new JndiDestinationResolver();
        jdr.setCache(true);
        jdr.setJndiTemplate(jndiTemplate);
        return jdr;
    }
    
	@Bean
	public JmsTemplate producerJmsTemplate() {
		JmsTemplate jt = new JmsTemplate(cachingConnectionFactory());
		jt.setDeliveryPersistent(true);
		jt.setDestinationResolver(jndiDestinationResolver());
		jt.setPubSubDomain(true);	// This sample is publishing to topics
		return jt;
	}
}
