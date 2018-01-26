package com.solace.samples.cloudfoundry.springcloud.jndidemo;

import java.util.Properties;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
    
//	@Autowired
//	private Properties jndiProperties;
	
//    @Bean
//    public JndiTemplate jndiTemplate() {
//        JndiTemplate jndiTemplate = new JndiTemplate();
//        jndiTemplate.setEnvironment(jndiProperties);
//        return jndiTemplate;
//    }

    
    @Bean
    public JndiObjectFactoryBean connectionFactory() {
        JndiObjectFactoryBean factoryBean = new JndiObjectFactoryBean();
        factoryBean.setJndiTemplate(jndiTemplate);
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
		return jt;
	}
}
