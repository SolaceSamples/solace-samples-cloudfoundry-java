package com.solacesystems.cloudfoundry.demos.intro.model;

public class SimpleMessage {
	
	private String topic;
	private String body;
	
	public SimpleMessage() {
		this.topic = "";
		this.body = "";
	}
	
	public String getTopic() {
		return topic;
	}
	
	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getBody() {
		return body;
	}
	
	public void setBody(String body) {
		this.body = body;
	}
}
