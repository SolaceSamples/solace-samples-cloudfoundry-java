package com.solace.samples.cloudfoundry.springcloud.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.support.destination.JndiDestinationResolver;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.jndi.JndiTemplate;

@EnableJms
public class JndiConsumerConfiguration {

	// Resource definitions: connection factory and queue destination
    @Value("${solace.jms.demoConnectionFactoryJndiName}")
    private String connectionFactoryJndiName;

    @Autowired
    JndiTemplate jndiTemplate;
    
	// TODO: fix it that this shouldn't be required (transfer to SempJndiConfigClient
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
    
    // DynamicDestinationResolver can be used instead for physical, non-jndi destinations
    @Bean
    public JndiDestinationResolver jndiDestinationResolver() {
    	JndiDestinationResolver jdr = new JndiDestinationResolver();
        jdr.setCache(true);
        jdr.setJndiTemplate(jndiTemplate);
        return jdr;
    }
}
