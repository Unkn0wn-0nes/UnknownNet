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
package com.Unkn0wn0ne.unknownet.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import com.Unkn0wn0ne.unknownet.client.errors.ProtocolViolationException;
import com.Unkn0wn0ne.unknownet.client.net.ClientRepository;
import com.Unkn0wn0ne.unknownet.client.net.InternalPacket1Kick;
import com.Unkn0wn0ne.unknownet.client.net.InternalPacket3KeepAlive;
import com.Unkn0wn0ne.unknownet.client.net.Packet;
import com.Unkn0wn0ne.unknownet.client.net.Packet.PACKET_PRIORITY;
import com.Unkn0wn0ne.unknownet.client.util.Protocol;

/**
 * UnknownClient - Abstract class for connecting to an UnknownNet server.
 * @author Unkn0wn0ne
 */
public abstract class UnknownClient implements Runnable{

	public Logger logger = Logger.getLogger("UnknownNet");
	
	protected ClientRepository clientRepository = new ClientRepository();
	
	protected String ipAddress = "";
	protected int port = 4334;
	protected boolean useSSL = false;
	protected String protocolVersion = "unknownserver-dev";
	private Protocol protocol = null;
	
	protected Socket socket = null;
	protected DatagramSocket dSocket = null;
	protected int authPort = 4333;
	
	protected DataInputStream dataInputStream = null;
	protected DataOutputStream dataOutputStream = null;
	protected DatagramPacket dPacket = null;
	protected DatagramPacket dPacket2 = null;
	protected ByteArrayOutputStream udpWriter = null;
	protected ByteArrayInputStream udpReader = null;
	protected int uid = -1;
	
	protected Queue<Packet> internalsToBeSent = new LinkedList<Packet>();
	protected Queue<Packet> highsToBeSent = new LinkedList<Packet>();
	protected Queue<Packet> lowsToBeSent = new LinkedList<Packet>();
	
	protected InternalPacket3KeepAlive keepAlivePacket = null;
	
	protected long lastReceivedKeepAlive;
	
	protected String[] loginParams = null;

	protected boolean shouldDisconnect = false;

	private IClientImplementation clientImpl;
	
	/**
	 * Creates an UnknownClient object for use in connecting to an UnknownNet server.
	 * @param useSSL Whether or not to connect to the server using SSL (Secure Socket Layer)
	 * @param protocolVersion The version of your custom protocol, used by the server to verify your client is up to date.
	 * @param useTCP Whether or not the client is using TCP. If set to false, the client uses UDP
	 */
	public UnknownClient(boolean useSSL, String protocolVersion) {
		this.useSSL = useSSL;
		this.protocolVersion = protocolVersion;
		
		this.clientRepository.init();
		try {
			this.keepAlivePacket = (InternalPacket3KeepAlive)this.clientRepository.getPacket(-3);
		} catch (ProtocolViolationException e) {
			this.logger.severe("Internal/UnknownClient: ProtocolViolationException while creating keep alive packet, this should never happen.");
			this.keepAlivePacket = new InternalPacket3KeepAlive();
		}
	}
	
	/**
	 * Connects the client to a UnknownNet-based server over TCP
	 * @param ip The IP address or hostname of the server to connect to
	 * @param port The port that the server is running on
	 * @param loginData A string array full of data that will be sent to the server for authentication purposes. This can be null and what this contains is completely up to your implementation
	 */
	public void connectTCP(String ip, int port, String[] loginData) {
		this.protocol = Protocol.TCP;
		this.ipAddress = ip;
		this.port = port;
		this.loginParams = loginData;
		this.clientImpl = new TCPClient(this);
		new Thread(this).start();
	}
	
	public void connectUDP(String ip, int port, int authPort, String[] loginData) {
		this.protocol = Protocol.UDP;
		this.ipAddress = ip;
		this.port = port;
		this.authPort = authPort;
		this.loginParams = loginData;
		this.clientImpl = new UDPClient(this);
		new Thread(this).start();
	}
	
	public void connectDualstack(String ip, int tcpport, int udpport, String[] loginData) {
		this.protocol = Protocol.DUALSTACK;
		this.ipAddress = ip;
		this.port = tcpport;
		this.authPort = udpport;
		this.loginParams = loginData;
		this.clientImpl = new DualstackClient(this);
		new Thread(this).start();
	}
	
	/**
	 * Internal method.
	 * Handles the client connection along with sending packets
	 */
	@Override
	public void run() {
		// TCP Client
		if (this.protocol == Protocol.TCP) {
			this.logger.info("Internal/UnknownClient: Client running with TCP");
			if (!this.useSSL) {
				logger.warning("Internal/UnknownClient: Client not configured to use SSL, this could be a security risk and may not be suitable for production builds depending on your implementation.");
				logger.info("Internal/UnknownClient: Connecting to " + ipAddress + ":" + port + " via unsecured socket (no SSL enabled)");
				try {
					this.socket = new Socket(this.ipAddress, this.port);
				} catch (UnknownHostException e) {
					logger.severe("Internal/UnknownClient: Failed to connect to server; an UnknownHostException hasoccurred.  (Message: " + e.getMessage() + ")");
					this.onConnectionFailed("Failed to connect to server; an UnknownHostException has occurred. (Message: " + e.getMessage() + ")");
					e.printStackTrace();
					return;
				} catch (IOException e) {
					logger.severe("Internal/UnknownClient: Failed to connect to server; an IOException occurred.  (Message: " + e.getMessage() + ")");
					this.onConnectionFailed("Failed to connect to server; an IOException has occurred. (Message: " + e.getMessage() + ")");
					e.printStackTrace();
					return;
				}
			} else {
				logger.info("Internal/UnknownClient: Connecting to " + ipAddress + ":" + port + " via SSL socket");
				// TODO: Allow alternate SSL certificates to be loaded instead of Java's default.
				SocketFactory sslSocketFactory = SSLSocketFactory.getDefault();
				try {
					this.socket = sslSocketFactory.createSocket(this.ipAddress, this.port);
				} catch (UnknownHostException e) {
					logger.severe("Internal/UnknownClient: Failed to connect to server; an UnknownHostException has occurred.  (Message: " + e.getMessage() + ")");
					this.onConnectionFailed("Failed to connect to server; an UnknownHostException has occurred. (Message: " + e.getMessage() + ")");
					e.printStackTrace();
					return;
				} catch (IOException e) {
					logger.severe("Internal/UnknownClient: Failed to connect to server; an IOException has occurred.  (Message: " + e.getMessage() + ")");
					this.onConnectionFailed("Failed to connect to server; an IOException has occurred. (Message: " + e.getMessage() + ")");
					e.printStackTrace();
					return;
				}
			}
			
			
			try {
				this.dataOutputStream = new DataOutputStream(this.socket.getOutputStream());
				this.dataInputStream = new DataInputStream(this.socket.getInputStream());
			} catch (IOException e1) {
				logger.info("Internal/UnknownClient: Failed to connect to " + ipAddress + ":" + port + ", an IOException has occurred.");
				this.onConnectionFailed("Failed to connect to " + ipAddress + ":" + port + ", an IOException has occurred.");
				return;
			}
			
			if (!this.clientImpl.authenticate()) {
				return;
			}
			logger.info("Internal/UnknownClient: Connection to " + ipAddress + ":" + port + " succeeded.");
			this.clientImpl.handleConnection();
		} else {
			// UDP CLIENT
			if (this.protocol == Protocol.UDP) {
				this.logger.info("Internal/UnknownClient: Running with UDP.");
				
				if (this.useSSL) {
					this.logger.warning("Internal/UnknownClient: SSL over UDP is not supported in UnknownNet, but UnknownNet initally connects with TCP to preform authentication to prevent complications and will use ssl for this process. After authentication UnknownNet will be running without SSL.");
				}
				
				this.logger.info("Internal/UnknownClient: Connecting to authentication service on " + this.ipAddress + ":" + this.authPort + " via TCP socket.");
				if (!this.clientImpl.authenticate()) {
					return;
				}
				this.logger.info("Internal/UnknownClient: Connecting to " + this.ipAddress + ":" + this.port + " via datagram socket.");
				
				try {
					
					this.dSocket = new DatagramSocket();
					this.dSocket.connect(InetAddress.getByName(this.ipAddress), this.port);
					this.dPacket = new DatagramPacket(new byte[dSocket.getReceiveBufferSize()], dSocket.getReceiveBufferSize());
					this.dPacket2 = new DatagramPacket(new byte[dSocket.getReceiveBufferSize()], dSocket.getReceiveBufferSize());
				} catch (SocketException e) {
					this.logger.severe("Internal/UnknownClient: Failed to connect to server; a SocketException has occurred. (Message: " + e.getMessage() + ")");
					this.onConnectionFailed("Failed to connect to server; a SocketException has occurred. (Message: " + e.getMessage() + ")");
					e.printStackTrace();
				} catch (UnknownHostException e) {
					logger.severe("Internal/UnknownClient: Failed to connect to server; an UnknownHostException has occurred.  (Message: " + e.getMessage() + ")");
					this.onConnectionFailed("Failed to connect to server; an UnknownHostException has occurred. (Message: " + e.getMessage() + ")");
					e.printStackTrace();
					return;
				}
				
				this.udpWriter = new ByteArrayOutputStream();
				this.dataOutputStream = new DataOutputStream(this.udpWriter);
				logger.info("Internal/UnknownClient: Connection to " + ipAddress + ":" + port + " succeeded.");
				this.clientImpl.handleConnection();
			}  else if (this.protocol == Protocol.DUALSTACK) {
				// DUALSTACK CLIENT
				this.logger.info("Internal/UnknownClient: Client running with DUALSTACK (TCP + UDP)");
				if (!this.clientImpl.authenticate()) {
					return;
				}		
				try {
					this.dSocket = new DatagramSocket();
					this.dSocket.connect(InetAddress.getByName(this.ipAddress), this.authPort);
					this.dPacket = new DatagramPacket(new byte[dSocket.getReceiveBufferSize()], dSocket.getReceiveBufferSize());
					this.dPacket2 = new DatagramPacket(new byte[dSocket.getReceiveBufferSize()], dSocket.getReceiveBufferSize());
				} catch (SocketException e2) {
					this.logger.severe("Internal/UnknownClient: Failed to create a DatagramSocket, a SocketException occurred. Aborting...");
					e2.printStackTrace();
					this.onConnectionFailed("Failed to create a DatagramSocket, a SocketException occurred.");
					return;
				} catch (UnknownHostException e) {
					this.logger.severe("Internal/UnknownClient: Failed to create a DatagramSocket, an UnknownHostException ocurred. Aborting...");
					e.printStackTrace();
					this.onConnectionFailed("Failed to create a DatagramSocket, an UnknownHostException occurred.");
					return;
				}
			
			this.udpWriter = new ByteArrayOutputStream();
			this.dataOutputStream = new DataOutputStream(this.udpWriter);
			
			logger.info("Internal/UnknownClient: Connection to " + ipAddress + ":" + port + " succeeded.");
			this.clientImpl.handleConnection();
	      }
		}
	}

	
	/**
	 * Internal method.
	 * Handles receiving of packets
	 * @param id The id of the packet received
	 * @throws ProtocolViolationException If the protocol was violated during the packet receive
	 * @throws IOException If there was an IO error receiving the packet
	 */
	protected void handlePacketReceive(int id, DataInputStream inputStream) throws ProtocolViolationException, IOException {
		switch (id) {
		case -3: {
			this.lastReceivedKeepAlive = System.currentTimeMillis();
			this.queuePacket(this.keepAlivePacket);
			return;
		}
		case -2: {
			throw new ProtocolViolationException("Unexpected InternalPacket2Handshake received.");
		} 
		case -1: {
			InternalPacket1Kick kickPacket = new InternalPacket1Kick();
			kickPacket.read(inputStream);
			logger.info("Internal/UnknownClient: Server is kicking us out! Message: " + kickPacket.getReason());
			this.onClientKicked(kickPacket.getReason());
			this.clientRepository.freePacket(kickPacket);
			return;
		}
		default: {
			Packet packet = this.clientRepository.getPacket(id);
			packet.read(inputStream);
			this.onPacketReceived(packet);
			this.clientRepository.freePacket(packet);
			return;
		}
		}
	}

	/**
	 * Called if the connection to the server was successful
	 */
	public abstract void onConnectionSuccess();
	
	/**
	 * Called if the connection to the server failed
	 * @param reason The reason the connection failed
	 */
	public abstract void onConnectionFailed(String reason);
	
	/**
	 * Called when the client has been kicked from the server
	 * @param reason The reason the client was kicked
	 */
	public abstract void onClientKicked(String reason);
	
	/**
	 * Called when a packet has been received
	 * Note that after this method has finished processing the packet will be cleared and reinserted into the pool. The packet cannot be saved after this method. You can copy the values for further processing if you wish.
	 * @param packet The packet that has been received. 
	 */
	public abstract void onPacketReceived(Packet packet);
	
	/**
	 * Registers this packet for use on the network. All packets must be registered in order to be sent/received 
	 * @param id The id this packet will have
	 * @param packet The class of the packet you would like to register 
	 */
	public void registerPacket(int id, Class<? extends Packet> packet) {
		this.clientRepository.registerPacket(id, packet);
	}
	
	/**
	 * Disconnects the client from the server
	 */
	public void leaveServer(String message) {
		try {
			InternalPacket1Kick disconnectPacket = (InternalPacket1Kick) this.clientRepository.getPacket(-1);
			disconnectPacket.setVariables(message);
			this.queuePacket(disconnectPacket);
		} catch (ProtocolViolationException e) {
			this.logger.severe("Internal/UnknownClient: ProtocolViolationException while trying to disconnect from server, this should never happen.");
			try {
				this.socket.close();
			} catch (IOException e1) {
				this.logger.severe("Internal/UnknownClient: IOException while disconnecting from server. Message: " + e1.getMessage());
			}
		}	
	}

	/**
	 * Queues a packet to be sent to the server
	 * @param packet The packet to be sent
	 */
	public void queuePacket(Packet packet) {
		if (packet.getPriority() == PACKET_PRIORITY.INTERNAL) {
			this.internalsToBeSent.add(packet);
		} else if (packet.getPriority() == PACKET_PRIORITY.HIGH) {
			this.highsToBeSent.add(packet);
		} else {
			this.lowsToBeSent.add(packet);
		}
	}
	
    
	/**
	 * Gives you a packet object
	 * @param id The id if the packet you'd like to get
	 * @return
	 * @throws ProtocolViolationException
	 */
	public Packet createPacket(int id) throws ProtocolViolationException {
		return this.clientRepository.getPacket(id);
	}
}
