---
layout: tutorials
title: Configuring LDAP
summary: How to setup an LDAP Server to work with Solace and some examples on how LDAP auth works
icon: I_ldap.svg
---

## Overview

LDAP is directory-based application protocol, which Solace can use for user authentication and authorization.
On the LDAP server, there will be a directory structure of users, which have an associated username and password, as well as a list of groups that each user belongs to.
On the PubSub+ message broker, groups are configured to be mapped to certain levels of authorization, and users are authorized based on the groups they belong to.

Assuming LDAP is enabled, when a user connects the username and password they provided are validated against those stored on the LDAP server.
If the credentials are valid, the LDAP server is queried to get the list of groups that the user belongs to.
The user is then authorized based on the groups they belong to, and given no authorization if no groups are configured.

There are two types of access:

* **Application access** allows clients to send and receive messages through various messaging protocols that Solace supports (e.g. MQTT, SMF).
* **Management access** allows users to view operational status and modify configuration by sending commands through SEMP or on the CLI.

## Goals

The goal of this tutorial is to demonstrate extracting the information from the application's Cloud Foundry Service Bindings and connect to the Solace Messaging service instance.  This tutorial will show you:

1. How to configure an LDAP server with some example ldif files.
1. How to use the credentials stored on the LDAP server for authentication and authorization.

## Assumptions

This tutorial assumes the following:

* You are familiar with Solace [core concepts]({{ site.docs-core-concepts }}).
* You are familiar with [Cloud Foundry]({{ site.links-ext-cloudfoundry }}){:target="_blank"}.
* You have access to a running Pivotal Cloud Foundry environment.
* Solace Messaging for PCF has been installed in your Pivotal Cloud Foundry environment.
* You have completed the Java app tutorial.
* You are using OpenLDAP.

## Files

This section only concerns you if you are setting up your own LDAP server.

If you already have a configured LDAP server then you can skip this section.

The following files are written for OpenLDAP and may need to be modified to support other LDAP implementations.

#### memberOf.ldif

The memberOf overlay is mainly for convenience.

With the memberOf overlay whenever the list of members in a group is updated, that member automatically gets updated with the correct memberOf attribute as well. This means only updating one place instead of two.

Without the memberOf overlay, a memberOf attribute for every user has to be updated manually with the correct groups.

```
dn: cn=modulecn=config
objectClass: olcModuleList
cn: module
olcModulePath: /usr/lib64/openldap
olcModuleLoad: memberof

dn: olcOverlay=memberofolcDatabase={2}bdbcn=config
objectClass: olcMemberOf
objectClass: olcOverlayConfig
objectClass: olcConfig
objectClass: top
olcOverlay: memberof
olcMemberOfDangling: ignore
olcMemberOfRefInt: TRUE
olcMemberOfGroupOC: groupOfNames
olcMemberOfMemberAD: member
olcMemberOfMemberOfAD: memberOf
```

#### content.ldif

Here is an example of how to configure a running LDAP server, with user `hank` belonging to group `finance`.

```
dn: dc=example,dc=com
changetype: add
dc: example
objectClass: dcObject
objectClass: top
objectClass: organization
o: example

dn: ou=users,dc=example,dc=com
changetype: add
objectClass: organizationalUnit
objectClass: top
ou: users

dn: ou=groups,dc=example,dc=com
changetype: add
objectClass: organizationalUnit
objectClass: top
ou: groups

dn: cn=hank,ou=users,dc=example,dc=com
changetype: add
objectClass: organizationalPerson
objectClass: person
objectClass: top
sn: hank
cn: hank
userPassword: hunter2

dn: cn=finance,ou=groups,dc=example,dc=com
changetype: add
objectClass: groupOfNames
objectClass: top
member: cn=hank,ou=users,dc=example,dc=com
cn: finance
```
#### Commands

These commands will apply the configuration described in the files above to the LDAP server.

```
ldapadd -Y EXTERNAL -H ldapi:/// -f memberOf.ldif
ldapadd -x -D 'bindDNUser' -w bindDNPassword -H ldapi:/// -f content.ldif
```

Where bindDNUser and binDNPassword are the bind DN credentials (if configured).

## Application Access Setup

You need to setup LDAP in the Solace tile correctly, see the [Solace Messaging Documentation]({{ site.links-ext-ldap-settings }}).

In this case, the the username is `hank` and the password is `hunter2`.

Set two cloud-foundry environment variables for a given app `APP_NAME` called `LDAP_CLIENTUSERNAME` and `LDAP_CLIENTPASSWORD` using the [Cloud Foundry command line tool]({{ site.links-ext-cf-cli }}).

```
    cf set-env APP_NAME LDAP_CLIENTUSERNAME hank
    cf set-env APP_NAME LDAP_CLIENTPASSWORD hunter2
```

If the Cloud Operator has set Application Access to `LDAP` instead of `Internal`, bindings will not come with application access credentials.

If the sample app does not receive any credentials in the binding, it will at these environment variables and use those for authentication instead.

#### Configuring Authorization for Application Access

Authorization groups for application access are not setup through PCF and thus have to be setup manually.

SSH into the PubSub+ message broker using a user with at least read-write access to that VPN.

Assuming you are using VPN `v001`, use these commands on the CLI:
```
    enable
    configure
    message-vpn v001
    authorization user-class client
    create authorization-group cn=finance,ou=groups,dc=example,dc=com
```

This will give all users in the `finance` group (ie `hank`) application access to that VPN.

You can read more about setting up authorization groups, including changing ACL profiles and client profiles in the [Solace Configuring Client LDAP Authorization Documentation]({{ site.links-client-ldap-authorization }}).

By default, all newly created authorization-groups will use the default ACL profile and default client profile.

NOTE: When a service is deleted all authorization groups associated with that VPN are deleted as well.

## Management Access Setup

When creating a service you can give read-write or read-only access to an LDAP group using the command line parameters 'ldapGroupAdminReadWrite' and 'ldapGroupAdminReadOnly'.

Here is an example, creating a service called `test` with a shared plan using the [Cloud Foundry command line tool]({{ site.links-ext-cf-cli }}).

```
    cf create-service solace-pubsub enterprise-shared test -c "{\"ldapGroupAdminReadWrite\": \"cn=finance,ou=groups,dc=example,dc=com\"}"
```

You can also do this through ops manager.

You can read more about setting up management access with LDAP in the [Solace Messaging Documentation]({{ site.links-ext-ldap-management-settings }}).
