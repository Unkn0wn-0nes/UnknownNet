/** Copyright 2014 Unkn0wn0ne

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. **/
package com.Unkn0wn0ne.unknownnet.server;


import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;

import com.Unkn0wn0ne.unknownnet.server.logging.LogType;
import com.Unkn0wn0ne.unknownnet.server.logging.UnknownLogger;
import com.Unkn0wn0ne.unknownnet.server.net.InternalPacket1Kick;
import com.Unkn0wn0ne.unknownnet.server.net.InternalPacket3KeepAlive;
import com.Unkn0wn0ne.unknownnet.server.net.Packet;
import com.Unkn0wn0ne.unknownnet.server.net.Packet.PACKET_PRIORITY;
import com.Unkn0wn0ne.unknownnet.server.net.errors.ProtocolViolationException;
import com.Unkn0wn0ne.unknownnet.server.util.Protocol;

/**
 * UnknownClient - A client object in the UnknownNet network.
 * This class is responsible for reading and writing packets to and from the client, along with handling these packets or passing them onto the server implementation to handle
 * This class has various protocol and security checks implemented into it to help prevent abuse or errors from misuse/incorrect use from interfering with the network
 * @author Unkn0wn0ne
 *
 */
public abstract class UnknownClient implements Runnable {
    
	/*
	 * -1 = Unauthenticated - Client is placed in a sandbox and can only access the authentication service of the server. 
	 * 0 = Authenticated - Server has accepted the client and it can proceed to access the rest of server. At this point a client can become visible.
	 * 1 = Administrative - This client has administrative access and can access administrative commands (implies 0; unauth'd clients cannot gain admin for security reasons (i.e. if rogue admin was banned they could bypass auth or hacking.)
	 */
	protected int clientState = -1; 
	protected boolean hasBeenEjected = false;
	
	protected Socket connection;
	protected UnknownServer server;
	
	protected DataInputStream dataInputStream;
	protected DataOutputStream dataOutputStream;
	
	
	protected Queue<Packet> internalsToBeSent = new LinkedList<Packet>();
	protected Queue<Packet> highPriorityToBeSent = new LinkedList<Packet>();
	protected Queue<Packet> lowPriorityToBeSent = new LinkedList<Packet>();
	
	protected int missedKeepAlives = -1;
	private InternalPacket3KeepAlive keepAlivePacket = null;
	
	private Object tag;
	protected int clientId;
	protected int port;
	
	protected Queue<Packet> datagramsToBeProcessed = new LinkedList<Packet>();
	private Protocol protocol = null;
	
	protected DatagramPacket datagram = null;
	private InetAddress addr = null;
	protected ByteArrayOutputStream udpWriter = null;
	protected boolean udpActive = false;
	
	protected float sendKeepAlives = 0;
	protected float receivedKeepAlives = 0;
	
	protected int privilegeLevel = -1000;
	
	/**
	 * Internal constructor. Should not be called
	 * @param socket
	 * @param server
	 * @param protocol
	 */
	protected UnknownClient(Socket socket, UnknownServer server, Protocol protocol) {
		this.protocol = protocol;
		this.connection = socket;
		this.server = server;
		this.addr = connection.getInetAddress();
		this.port = connection.getPort();
		
		if (this.protocol == Protocol.UDP || this.protocol == Protocol.DUALSTACK) {
			byte[] buffer;
			try {
				buffer = new byte[socket.getReceiveBufferSize()];
				this.datagram = new DatagramPacket(buffer, buffer.length);
				this.datagram.setAddress(this.addr);
			} catch (SocketException e) {
			}
		}
	}

	
	@Override 
	public void run() {
		try {
			this.keepAlivePacket = (InternalPacket3KeepAlive)this.server.getRepository().getPacket(-3);
		} catch (ProtocolViolationException e2) {
			UnknownLogger.log(Level.WARNING, LogType.NETWORKING, "Internal/UnknownClient: ProtocolViolationException occurred while creating keep alive packet, this should never happen.");
			this.keepAlivePacket = new InternalPacket3KeepAlive();
		}
		
			try {
				dataInputStream = new DataInputStream(this.connection.getInputStream());
				dataOutputStream = new DataOutputStream(this.connection.getOutputStream());
			} catch (IOException e1) {
				
			}
			
			int id = 1;
			try {
				id = dataInputStream.readInt();
			} catch (IOException e1) {
				this.eject("Protocol error; IOException while accepting you.", false);
				return;
			}
			if (id != -2) {
				this.eject("Security Violation: First packet was not Handshake packet.", false);
				return;
			}
			
			try {
				this.authenticateClient();
			} catch (ProtocolViolationException e1) {
				this.eject("Protocol Error: A protocol violation has occurred. Message: " + e1.getMessage(), false);
				return;
			} catch (IOException e) {
				this.eject("Protocol Error", false);
				return;
			}
			
			this.handleConnection();
	}
	
	

	
	/**
	 * Ejects the client from the server, removing it from the server's client list and sending the InternalPacket1Kick that kicks the client from the server. It stops various functions to allow the client to die.
	 * @param msg The message to kick the client for.
	 * @param isSilent Determines whether or not this kick will be logged. This can be used to either reduce log spam or to hide the reasoning from the logs
	 */
	public void eject(String msg, boolean isSilent) {
		if (this.hasBeenEjected) {
			return;
		}
		if (!isSilent) {
			UnknownLogger.log(Level.INFO, LogType.NETWORKING, "Internal/UnknownClient: Ejecting client '" + this.addr.getHostAddress() + ":" + this.port + "' for reason: " + msg);
		}
		this.hasBeenEjected = true;
		try {
			InternalPacket1Kick ejectPacket = (InternalPacket1Kick)this.server.getRepository().getPacket(-1);
			ejectPacket.setVariables(msg);
			if (this.clientState == -1) {
				ejectPacket._write(dataOutputStream);
				return;
			}
			this.queuePacket(ejectPacket);
		} catch (ProtocolViolationException e) {
			// Kill the socket
			try {
				this.connection.close();
			} catch (IOException e1) {
				
			}
		} catch (IOException e) {
			
		}
		this.server.handleClientLeaving(this);
	}

	/**
	 * Returns whether or not the client has been ejected (kicked) from the server
	 */
	public boolean hasBeenEjected() {
		return this.hasBeenEjected;
	}
	
	/**
	 * Internal method. Do not call
	 * Sets the client state
	 * @param stateCode the new state of the client
	 */
	protected void setState(int stateCode) {
		this.clientState = stateCode;
	}
	
	/**
	 * Gets the client state. 
	 * -1 = Unauthenticated - Client is placed in a sandbox and can only access the authentication service of the server. 
	 * 0 = Authenticated - Server has accepted the client and it can proceed to access the rest of server. At this point a client can become visible.
	 * 1 = Administrative - This client has administrative access and can access administrative commands (implies 0; unauth'd clients cannot gain admin for security reasons (i.e. if rogue admin was banned they could bypass auth or hacking.)
	 * @return the clients current state
	 */
	public int getState() {
		return this.clientState;
	}

	public int getPrivilegeLevel() {
		return this.privilegeLevel;
	}
	
	public void setPrivilegeLevel(int newLevel) {
		this.privilegeLevel = newLevel;
	}
	
	/**
	 * Internal method. Do not call
	 * Starts the client thread.
	 */
	protected void start() {
		new Thread(this, "Client-" + this.getId()).start();
	}
	
	/**
	 * Queues a packet to be sent as soon as possible.
	 * Note: If you are sending the packet to more than one client you MUST specify the number of clients you are sending it to by setting {@link Packet#setRecipentCount(int) }
	 * The packet will be cleared and put back into the pool after all clients it had been sent to have sent it.
	 * @param p The packet to be sent
	 */
	public void queuePacket(Packet p) {
		if (p == null) {
			return;
		}
		
		if (p.getPriority() == PACKET_PRIORITY.INTERNAL) {
			this.internalsToBeSent.add(p);
			return;
		} else if (p.getPriority() == PACKET_PRIORITY.HIGH) {
			this.highPriorityToBeSent.add(p);
			return;
		} else {
			this.lowPriorityToBeSent.add(p);
		}
	}

	protected void queueUDPPacketProcess(Packet packet) {
		this.datagramsToBeProcessed.add(packet);
	}
	
	
	/**
	 * Internal method. Do not call
	 * Sends a keep alive packet to the client.
	 * If a client has not responded the the previous one, this method will eject it for timing out
	 */
	protected void sendKeepAlive() {
		this.missedKeepAlives++;
		this.sendKeepAlives++;
		if (this.missedKeepAlives > 3) {
			this.eject("Client Timeout: Missed 3 keep alives.", false);
		}
		this.queuePacket(this.keepAlivePacket);
	}
	
	
	/**
	 * Allows your implementation to set an object for the client to have to identify it or that belongs to it.
	 * @param object The object you'd like to set as a tag
	 */
	public void setObjectTag(Object object) {
		this.tag = object;
	}
	
	/**
	 * Gets the object that the client has a tag
	 * @return The client's object tag.
	 */
	public Object getClientTag() {
		return this.tag;
	}

	/**
	 * Gets the client's unique id for the session
	 * @return The clients unique id
	 */
	public int getId() {
		return this.clientId;
	}
	
	/** 
	 * Internal method. Do not call.
	 * Sets the client id
	 * @param id The id to set as the clients id.
	 */
	protected void setId(int id) {
		this.clientId = id;
	}

	public InetAddress getAddress() {
		return this.addr;
	}
	
	protected void setUDP(int port) {
		if (this.udpActive) return;
		this.port = port;
		this.datagram.setPort(port);
	}
	
	protected abstract void processPacket(Packet packet);
	
	protected abstract void authenticateClient() throws IOException, ProtocolViolationException;
	
	protected abstract void handleConnection();

	protected abstract void shutdown();

	public int getUDP() {
		return this.datagram.getPort();
	}

	public UnknownServer getServer() {
		return this.server;
	}
}