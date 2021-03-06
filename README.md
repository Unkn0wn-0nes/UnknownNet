UnknownNet is currently in development. For progress on the first release of UnknownNet, check our initial alpha milestone: https://github.com/Unkn0wn-0nes/UnknownNet/milestones/Alpha-1.0.0
================

UnknownNet
==========

Build Status: ![](https://travis-ci.org/Unkn0wn-0nes/UnknownNet.svg)

UnknownNet is a Java-based networking architecture wrapping around the TCP and UDP networking protocols, primiarly focusing on game servers, but can be used for other types of software. It contains a client library for clients to use and a server base for you to build a game server over without dealing with the pesky networking code. 

Feel free to join the (very) small community over at irc.freenode.net, 6667 Channel #UnknownNet for discussions and help

License 
==========

UnkownNet is licensed under the Apache 2.0 license, meaning you can use in non-commerical and commerical projects! You are not required to submit patches/bug fixes/enchancements upstream, but if you find a bug, security issue, or have a nice little enchancement you think would prove helpful, we'd love to incoperate into the project. You are not required to give credit if you include this in your project, but it is always nice.

SSL Notice
=========
UnknownNet has support for SSL TCP client/server sockets using the javax.net and javax.net.ssl libraries. SSL supported can be disabled by setting the 'useSSL' paramter to false on the client and server. SSL is primarily used to prevent direct manipulation of the data streams and help protect confidential information (i.e. user passwords / session tokens)

Notice
========
UnknownNet has not even reached an alpha release yet. Project goals may change and code/protocol breakages may occur. We (the project team) are not liable for anything that happens as a result of using this software. As this is the nature of experimental software.

