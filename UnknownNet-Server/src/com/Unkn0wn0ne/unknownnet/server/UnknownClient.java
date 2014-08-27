package com.Unkn0wn0ne.unknownnet.server;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

import com.Unkn0wn0ne.unknownnet.server.net.InternalPacket1Kick;
import com.Unkn0wn0ne.unknownnet.server.net.InternalPacket2Handshake;
import com.Unkn0wn0ne.unknownnet.server.net.InternalPacket3KeepAlive;
import com.Unkn0wn0ne.unknownnet.server.net.Packet;
import com.Unkn0wn0ne.unknownnet.server.net.Packet.PACKET_PRIORITY;
import com.Unkn0wn0ne.unknownnet.server.net.errors.ProtocolViolationException;

/**
 * UnknownClient - A client object in the UnknownNet network.
 * This class is responsible for reading and writing packets to and from the client, along with handling these packets or passing them onto the server implementation to handle
 * This class has various protocol and security checks implemented into it to help prevent abuse or errors from misuse/incorrect use from interfering with the network
 * @author Unkn0wn0ne
 *
 */
public class UnknownClient implements Runnable {
    
	/*
	 * -1 = Unauthenticated - Client is placed in a sandbox and can only access the authentication service of the server. 
	 * 0 = Authenticated - Server has accepted the client and it can proceed to access the rest of server. At this point a client can become visible.
	 * 1 = Administrative - This client has administrative access and can access administrative commands (implies 0; unauth'd clients cannot gain admin for security reasons (i.e. if rogue admin was banned they could bypass auth or hacking.)
	 */
	private int clientState = -1; 
	private boolean hasBeenEjected = false;
	
	private Socket connection;
	private UnknownServer server;
	
	private DataInputStream dataInputStream;
	private DataOutputStream dataOutputStream;
	
	
	private Queue<Packet> internalsToBeSent = new LinkedList<Packet>();
	private Queue<Packet> highPriorityToBeSent = new LinkedList<Packet>();
	private Queue<Packet> lowPriorityToBeSent = new LinkedList<Packet>();
	
	private Packet packet = null;
	private int missedKeepAlives = -1;
	private InternalPacket3KeepAlive keepAlivePacket = null;
	
	private Object tag;
	private int clientId;
	
	
	/**
	 * Internal constructor. Should not be called
	 * @param socket
	 * @param server
	 */
	protected UnknownClient(Socket socket, UnknownServer server) {
		this.connection = socket;
		this.server = server;
		
	}

	@Override 
	public void run() {
		try {
			this.keepAlivePacket = (InternalPacket3KeepAlive)this.server.getRepository().getPacket(-3);
		} catch (ProtocolViolationException e2) {
			this.server.logger.severe("Internal/UnknownClient: ProtocolViolationException occurred while creating keep alive packet, this should never happen.");
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
		
		String[] loginData = null;
		try {
			InternalPacket2Handshake handshakePacket = (InternalPacket2Handshake)this.server.getRepository().getPacket(-2);
			handshakePacket.read(dataInputStream);
			String version = handshakePacket.getVersion();
			loginData = handshakePacket.getLoginData();
			if (!version.equalsIgnoreCase(this.server.getProtocolVersion())) {
				handshakePacket.setVariables(false);
				handshakePacket._write(dataOutputStream);
				this.eject("Protocol Error: Protocol version mismatch. (Server = " + this.server.getProtocolVersion() + " You = " + version + ") Have you updated your client?", false);
				return;
			} else if (!this.server.handleNewConnection(this, loginData)) {
				handshakePacket.setVariables(false);
				handshakePacket._write(dataOutputStream);
				if (!this.hasBeenEjected()) {
					this.eject("Server has refused to authenicate you.", false);
					return;
				}
			} else {
				this.setState(0); // Allows the client to escape the sandbox and access the rest of the server
				server.freeClientFromSandbox(this);
				handshakePacket.setVariables(true);
				handshakePacket._write(dataOutputStream);
			}	
		} catch (ProtocolViolationException e1) {
			this.eject("Protocol Error: A protocol violation has occurred. Message: " + e1.getMessage(), false);
			return;
		} catch (IOException e) {
			this.eject("Protocol Error", false);
			return;
		}
		
		
		
		while (this.connection.isConnected()) {
			try {
				Thread.sleep(25);
			} catch (InterruptedException e) {
				
			}
			
			try {
				if (dataInputStream.available() > 0) {
					handlePacket(dataInputStream.readInt());
				}
			} catch (IOException e) {
				
			} catch (ProtocolViolationException e) {
				this.eject("Protocol Error: " + e.getMessage(), false);
			}
			
			while (!this.internalsToBeSent.isEmpty()) {
				Packet internalPacket = this.internalsToBeSent.poll();
				
				try { 
					internalPacket._write(this.dataOutputStream);
					
				} catch (IOException e) {
					this.eject("IOException occurred while sending data to stream.", false);
				} 
				
				if (internalPacket instanceof InternalPacket1Kick) {
						return;
				}
			}
			
			while (!this.highPriorityToBeSent.isEmpty()) {
				try {
					this.highPriorityToBeSent.poll()._write(this.dataOutputStream);
				} catch (IOException e) {
					this.eject("IOException occurred while sending data to stream.", false);
				}
			}
			
			while (!this.lowPriorityToBeSent.isEmpty()) {
				try {
					this.lowPriorityToBeSent.poll()._write(this.dataOutputStream);
				} catch (IOException e) {
					this.eject("IOException occurred while sending data to stream.", false);
				}
			}
		}
	}
	
	private void handlePacket(int id) throws ProtocolViolationException, IOException {
		if (this.hasBeenEjected) {
			// Don't handle it. Client has been ejected and this thread will be shutting down.
			return;
		}
		switch (id) {
		case -4: {
			if (this.clientState != 1) {
				// Client is not registered as an administrator. (Hacking? Possible accident?)
				// We won't handle this, instead we'll log it to our security manager, ServerGuard
				this.server.getSeverGuard().logSecurityViolation(VIOLATION_TYPE.SECURITY_ISSUE, this);
			}
			return;
		}
		case -3: {
			if (this.missedKeepAlives == -1) {
				// Client is spamming keep-alive packets, eject them
				this.eject("Protocol Error: Invalid keep alive packet received.", false);
			}
			this.missedKeepAlives = -1;
			return;
		}
		case -2: {
			// Handshake packets are handled before this method is used, and can only be sent once in the beginning of the connection.  
			// This should not happen and violates the protocol specification. This results in termination of the connection.
			this.eject("Protocol Error: You've already been authenticated.", false);
			return;
     	}
		case -1: {
			InternalPacket1Kick disconnectPacket = (InternalPacket1Kick)this.server.getRepository().getPacket(-1);
			disconnectPacket.read(this.dataInputStream);
			this.server.logger.info("Internal/UnknownClient: Client '" + this.connection.getInetAddress().getHostAddress() + "' has disconnection. [Reason: " + disconnectPacket.getMessage() + "]");
			this.server.handleClientLeaving(this);
			break;
		}
		default: {
			// Not an internal packet, let implementation handle it.
			packet = this.server.getRepository().getPacket(id);
			packet.read(this.dataInputStream);
			this.server.onPacketReceived(this, packet);
			break;
		}
		}
	}

	/**
	 * Gets the socket object of the client
	 * @return The socket object the client is using.
	 */
	public Socket getSocket() {
		return this.connection;
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
			this.server.logger.info("Internal/UnknownClient: Ejecting client '" + this.connection.getInetAddress().getHostAddress() + "' for reason: " + msg);
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

	/**
	 * Internal method. Do not call
	 * Starts the client thread.
	 */
	protected void start() {
		new Thread(this).start();
	}
	
	/**
	 * Queues a packet to be sent as soon as possible
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

	/**
	 * Internal method. Do not call
	 * Sends a keep alive packet to the client.
	 * If a client has not responded the the previous one, this method will eject it for timing out
	 */
	protected void sendKeepAlive() {
		this.missedKeepAlives++;
		if (this.missedKeepAlives > 0) {
			this.eject("Client Timeout: Missed a keep alive.", false);
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
}
