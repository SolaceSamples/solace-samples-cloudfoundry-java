---
layout: docs
title: Java Application
---

This tutorial will introduce you to Solace Messaging for Pivotal Cloud Foundry by creating a Java application which
connects to a Solace Messaging service instance.

# Assumptions

This tutorial assumes the following:

* You are familiar with Solace [core concepts](http://dev.solacesystems.com/docs/core-concepts/).
* You have access to a running Pivotal Cloud Foundry environment.
* Solace Messaging for PCF have been installed in your Pivotal Cloud Foundry environment.
 
# Goals

The goal of this tutorial is to demonstrate extracting the information from the Application Service Binding and connect
to the Solace Messaging service instance.  This tutorial will show you:

1. How to parse the VCAP_SERVICES environment variable and extract the Solace Messaging service instance.
1. How to use the information in the Service Instance to establish a connection to the Solace Router.

# Obtaining the Solace API

This tutorial depends on you having the Java API downloaded and available. The Java API library can be 
[downloaded here](http://dev.solacesystems.com/downloads/). The Java API is distributed as a zip file containing the
required jars, API documentation, and examples. The instructions in this tutorial assume you have downloaded the
Java API library and unpacked it to a known location. If your environment differs then adjust the build instructions
appropriately.

