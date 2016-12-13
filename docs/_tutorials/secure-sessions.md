---
layout: tutorials
title: Secure Sessions
summary: A simple Spring Cloud application showing how to connect with the Solace Messaging service using Transport Level Security (TLS).
icon: spring-cloud.jpg
---

* [Overview](#overview)
* [Goals](#goals)
* [Assumptions](#assumptions)
* [Working with a trusted certificate](#working-with-a-trusted-certificate)
* [Code walk through - trusted certificates](#code-walk-through---trusted-certificates)
* [Working with a self-signed certificate](#working-with-a-self-signed-certificate)


## Overview

This tutorial is part of a series of tutorials which aims to introduce users to Solace Messaging in Pivotal Cloud Foundry. Solace Messaging in Pivotal Cloud Foundry is delivered as a tile on the [Pivotal Network](https://network.pivotal.io/){:target="_blank"}. you can see the [Solace Messaging for Pivotal Cloud Foundry documentation](http://docs.pivotal.io/solace-messaging/){:target="_blank"} for full details.


This tutorial is based on the [Spring Cloud]({{ site.baseurl }}/spring-cloud) tutorial, adding a demonstration on how to connect to the Solace Messaging service using TLS.

## Goals

The goal of this tutorial is to demonstrate how to connect to the Solace Messaging service instance using TLS. This tutorial will show you how to establish a connection to the solace messaging service using both self-signed and Certificate Authority approved certificates.

## Assumptions

This tutorial assumes the following:

* You have completed the [Spring Cloud]({{ site.baseurl }}/spring-cloud) tutorial.
* You are familiar with [Transport Level Security](https://en.wikipedia.org/wiki/Transport_Layer_Security){:target="_blank"} concepts, including how to create or obtain certificates.


## Working with a Trusted Certificate

In order to use Transport Level Security, your Solace Message Router needs to be configured for TLS with an installed servcer certificate. The instructions for doing that are described in the Solace documentation - see [Managing Server Certificates]({{ site.links-tls-server }}){:target="_blank"}.

This section assumes that you have a Solace Messaging Service Instance properly configured for TLS access with a certificate that has been purchased from a certificate authority. Instructions for working with self-signed certificates are below.

## Code Walk Through - Trusted Certificates

when we initialize the connection to the service, we can specify whether to use TLS or not. to use TLS, we replaced this line from the Spring Cloud tutorial:

```java
properties.setProperty(JCSMPProperties.HOST, solaceMessagingServiceInfo.getSmfHost());
```

with this:

```java
properties.setProperty(JCSMPProperties.HOST, solaceMessagingServiceInfo.getSmfTlsHost());
```

When you use the TLS host, you can also specify whether the client should validate the TLS certificate.
This should always be done in production environments. We do this by setting the `JCSMPProperties`:

```java
properties.setproperty(JCSMPProperties.SSL_VALIDATE_CERTIFICATE, true);
properties.setproperty(JCSMPProperties.SSL_VALIDATE_CERTIFICATE_DATE, true);
```

That is all that is required when using a Certificate Authority issued certificate.

## Working with a Self-signed Certificate

You can install your Solace Messaging service for PCF with a self-signed certificate. For instructions on how to do this see the [Solace Messaging Documentation]({{ site.links-tls-server }}){:target="_blank"}.

With self-signed certificates you have two choices. Either you can have the client validate the self-signed certificate,
or not. For testing purposes, you can choose to not validate the certificate simply by
setting the VALIDATE_CERTIFICATE constant to false:

```java
properties.setproperty(JCSMPProperties.SSL_VALIDATE_CERTIFICATE, false);
properties.setproperty(JCSMPProperties.SSL_VALIDATE_CERTIFICATE_DATE, false);
```

Note that this is insecure and should never be done in production. This provides you with a TLS encrypted connection to the Solace Message Router but no validation of the identity so you're still exposed to a variety of attacks.

It is also possible to validate the self-signed certificate. This provides an environment that is a little closer to a production. To do this, you must be a certificate to the client-side trust store. This is tricky because the trust store is bundled along with the JRE together with the Cloud Foundry app. It must be added after the application is deployed but before it connects with the Solace Messaging service. Follow these steps:

1. Copy the certificate (the *.pem file) to the directory secure-app/src/main/resources.
1. Edit the file. Remove the private key section and just leave the lines starting with -----BEGIN CERTIFICATE----- and ending with ----- END CERTIFICATE-----.
1. In the CertificateUtil class, change the `CERTIFICATE_FILE_NAME` to match your certificate's file name.
1. In the SolaceController class, enable certificate validation.

```java
properties.setproperty(JCSMPProperties.SSL_VALIDATE_CERTIFICATE, false);
properties.setproperty(JCSMPProperties.SSL_VALIDATE_CERTIFICATE_DATE, false);
```

When you set `INSTALL_CERTIFICATE` to `true`, it instructs the SolaceController to use the CertificateUtil class to install the certificate in the JRE's trusted store before trying to connect to the service. 

The way it works is this. When you copy the certificate into the src/main/resources file, it gets packaged with the application. The application can then read the file from its local file system at runtime.

The standard location for a trusted store, relative to a JRE directory, is in a file at lib/security/cacerts.

The JRE is provided at runtime by Cloud Foundry, and from the point of view of an application the path to the jre is
```
/home/vcap/app/.java-buildpack/open_jdk_jre
```

The CertificateUtil class reads the certificate and installs it in the trusted store when the application starts running, but before it tries to connect to the service.

Note that we only need to do this when we want the client to validate a self-signed certificate. There should never be a need to do this kind of thing in a production system.

### Next Steps

Once this has been done, the process of building, deploying and testing the application are the same as in the [Spring Cloud]({{ site.baseurl }}/spring-cloud) tuturial.

TODO: Repeat I suggest repeating the steps for how to run