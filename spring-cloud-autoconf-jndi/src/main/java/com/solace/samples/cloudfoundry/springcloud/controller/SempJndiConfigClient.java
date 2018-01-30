package com.solace.samples.cloudfoundry.springcloud.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.solace.samples.cloudfoundry.springcloud.model.MsgVpnJndiConnectionFactory;
import com.solace.samples.cloudfoundry.springcloud.model.MsgVpnJndiTopic;
import com.solace.spring.cloud.core.SolaceMessagingInfo;

public class SempJndiConfigClient {

	private static final Log logger = LogFactory.getLog(SempJndiConfigClient.class);

	private RestTemplateBuilder restTemplateBuilder;
    private SolaceMessagingInfo solaceMessagingInfo;
    
    public SempJndiConfigClient(RestTemplateBuilder restTemplateBuilder, SolaceMessagingInfo solaceMessagingInfo) {
		this.restTemplateBuilder = restTemplateBuilder;
		this.solaceMessagingInfo = solaceMessagingInfo;
	}

	public void provisionJndiConnectionFactoryIfNotExists(String jndiCFName) {
        String sempUri = "http://" + solaceMessagingInfo.getActiveManagementHostname() +
		         "/SEMP/v2/config/msgVpns/" + solaceMessagingInfo.getMsgVpnName() +
		         "/jndiConnectionFactories";
        logger.info("*** provision Jndi ConnectionFactory with uri: " + sempUri);
        
        try {
        	// this will only continue if already exists, otherwise exception thrown
        	getRestTemplate().getForObject(sempUri + "/" + jndiCFName, String.class);
        	return;
		} catch (RestClientException e) {
	        logger.info("*** got RestClientException: " + e);
        	// will land here if the reqested config does not exist yet
			// if provisioning is not successful exception will be thrown
	        MsgVpnJndiConnectionFactory jndiCF = new MsgVpnJndiConnectionFactory(jndiCFName, false); // direct = false
        	getRestTemplate().postForObject(sempUri, jndiCF, String.class);
		}
    }

    public void provisionJndiTopicIfNotExists(String jndiTopicName) {
        String sempUri = "http://" + solaceMessagingInfo.getActiveManagementHostname() +
		         "/SEMP/v2/config/msgVpns/" + solaceMessagingInfo.getMsgVpnName() +
		         "/jndiTopics";
        try {
        	// this will only continue if already exists, otherwise exception thrown
        	getRestTemplate().getForObject( sempUri + "/" + jndiTopicName, String.class);
        	return;
		} catch (RestClientException e) {
        	// will land here if the reqested config does not exist yet
			// if provisioning is not successful exception will be thrown
	        MsgVpnJndiTopic jndiTopic = new MsgVpnJndiTopic(jndiTopicName, jndiTopicName); // using here same physical topic name
			getRestTemplate().postForObject(sempUri, jndiTopic, String.class);
		}
    }    

    private RestTemplate getRestTemplate() {
        RestTemplate restTemplate = restTemplateBuilder.basicAuthorization(		// build using basic authentication details
        		solaceMessagingInfo.getManagementUsername(),
        		solaceMessagingInfo.getManagementPassword()).build();         
        logger.info("*** creating restTemplate with credentials: " + solaceMessagingInfo.getManagementUsername() +
        		", " + solaceMessagingInfo.getManagementPassword());
        return restTemplate;
    }    
}
