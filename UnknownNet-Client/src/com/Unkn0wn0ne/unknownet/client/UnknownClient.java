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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import com.Unkn0wn0ne.unknownet.client.distributed.DistributedObject;
import com.Unkn0wn0ne.unknownet.client.distributed.ObjectManager;
import com.Unkn0wn0ne.unknownet.client.errors.ProtocolViolationException;
import com.Unkn0wn0ne.unknownet.client.net.ClientRepository;
import com.Unkn0wn0ne.unknownet.client.net.InternalPacket1Kick;
import com.Unkn0wn0ne.unknownet.client.net.InternalPacket3KeepAlive;
import com.Unkn0wn0ne.unknownet.client.net.InternalPacket6DistributedObjectCreation;
import com.Unkn0wn0ne.unknownet.client.net.InternalPacket7DestroyDistributedObject;
import com.Unkn0wn0ne.unknownet.client.net.InternalPacket8DistributedObjectEdit;
import com.Unkn0wn0ne.unknownet.client.net.InternalPacket9LeaveZone;
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
	
	protected int authPort = 4334;
	
	protected ObjectManager dObjManager = new ObjectManager();
	protected HashMap<Integer, ObjectManager> clientZones = new HashMap<Integer, ObjectManager>();
	
	protected Queue<Packet> internalsToBeSent = new LinkedList<Packet>();
	protected Queue<Packet> highsToBeSent = new LinkedList<Packet>();
	protected Queue<Packet> lowsToBeSent = new LinkedList<Packet>();
	
	protected InternalPacket3KeepAlive keepAlivePacket = null;
	
	protected long lastReceivedKeepAlive;
	
	protected String[] loginParams = null;

	protected boolean shouldDisconnect = false;

	private IClientImplementation clientImpl;

	protected int uid;
	
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
		Socket authenticationSocket = null;
		// TCP Client
		if (this.protocol == Protocol.TCP) {
			this.logger.info("Internal/UnknownClient: Client running with TCP");
			if (!this.useSSL) {
				logger.warning("Internal/UnknownClient: Client not configured to use SSL, this could be a security risk and may not be suitable for production builds depending on your implementation.");
				logger.info("Internal/UnknownClient: Connecting to " + ipAddress + ":" + port + " via unsecured socket (no SSL enabled)");
				
				try {
					authenticationSocket = new Socket(this.ipAddress, this.port);
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
					authenticationSocket = sslSocketFactory.createSocket(this.ipAddress, this.port);
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
			
			DataOutputStream dataOutputStream = null;
			DataInputStream dataInputStream = null;
			try {
				dataOutputStream = new DataOutputStream(authenticationSocket.getOutputStream());
				dataInputStream = new DataInputStream(authenticationSocket.getInputStream());
			} catch (IOException e1) {
				logger.info("Internal/UnknownClient: Failed to connect to " + ipAddress + ":" + port + ", an IOException has occurred.");
				this.onConnectionFailed("Failed to connect to " + ipAddress + ":" + port + ", an IOException has occurred.");
				return;
			}
			
		    TCPClient impl = (TCPClient)this.clientImpl;
		    impl.setConnection(authenticationSocket, dataOutputStream, dataInputStream);
		    
			if (!impl.authenticate()) {
				return;
			}
			logger.info("Internal/UnknownClient: Connection to " + ipAddress + ":" + port + " succeeded.");
			this.clientImpl.handleConnection();
		} else {
			// UDP CLIENT and DUALSTACK Client
			if (this.protocol == Protocol.UDP || this.protocol == Protocol.DUALSTACK) {
				DatagramSocket dSocket = null;
				DatagramPacket dPacket = null;
				DatagramPacket dPacket2 = null;
				ByteArrayOutputStream udpWriter = new ByteArrayOutputStream();
				DataOutputStream dataOutputStream = new DataOutputStream(udpWriter);
				
				if (this.protocol == Protocol.UDP) {
					this.logger.info("Internal/UnknownClient: Running with UDP.");
				} else {
					this.logger.info("Internal/UnknownClient: Client running with DUALSTACK (TCP + UDP)");
				}
				
				if (this.useSSL) {
					this.logger.warning("Internal/UnknownClient: SSL over UDP is not supported in UnknownNet, but UnknownNet initally connects with TCP to preform authentication to prevent complications and will use ssl for this process. After authentication UnknownNet will be running without SSL on the UDP packets");
				}
				
				this.logger.info("Internal/UnknownClient: Connecting to authentication service on " + this.ipAddress + ":" + this.authPort + " via TCP socket.");
				if (!this.clientImpl.authenticate()) {
					return;
				}
				this.logger.info("Internal/UnknownClient: Connecting to " + this.ipAddress + ":" + this.port + " via datagram socket.");
				
				try {		
					dSocket = new DatagramSocket();
					dSocket.connect(InetAddress.getByName(this.ipAddress), this.port);
					dPacket = new DatagramPacket(new byte[dSocket.getReceiveBufferSize()], dSocket.getReceiveBufferSize());
					dPacket2 = new DatagramPacket(new byte[dSocket.getReceiveBufferSize()], dSocket.getReceiveBufferSize());
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
				if (this.protocol == Protocol.UDP) {
					UDPClient udpImpl = (UDPClient)this.clientImpl;
					udpImpl.setConnection(dSocket, dPacket, dPacket2, udpWriter, dataOutputStream);
				} else {
					DualstackClient dualImpl = (DualstackClient)this.clientImpl;
					dualImpl.setConnection(dSocket, dPacket, dPacket2, udpWriter, dataOutputStream);
				}
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
		case -9: {
			InternalPacket9LeaveZone zonePacket = (InternalPacket9LeaveZone) this.clientRepository.getPacket(-9);
			zonePacket.read(inputStream);
		    this.getObjectManager(zonePacket.getZoneId()).leaveZone(zonePacket.getZoneId(), this);
			this.clientRepository.freePacket(zonePacket);
			return;
		}
		case -8: {
			InternalPacket8DistributedObjectEdit dObjPacket = (InternalPacket8DistributedObjectEdit) this.clientRepository.getPacket(-8);
			dObjPacket.read(inputStream);
			this.editDistributedObject(dObjPacket);
			this.clientRepository.freePacket(dObjPacket);
			return;
		}
		case -7: {
			InternalPacket7DestroyDistributedObject dObjPacket = (InternalPacket7DestroyDistributedObject) this.clientRepository.getPacket(-7);
			dObjPacket.read(inputStream);
			this.onDistributedObjectDestroyed(dObjPacket.getClientZoneId(), dObjPacket.getDObjectId());
			this.dObjManager.removeDistributedObject(dObjPacket.getDObjectId());
			this.clientRepository.freePacket(dObjPacket);
			return;
		}
		case -6: {
			InternalPacket6DistributedObjectCreation dObjPacket = (InternalPacket6DistributedObjectCreation) this.clientRepository.getPacket(-6);
			dObjPacket.read(inputStream);
			parseDistributedObject(dObjPacket);
			this.clientRepository.freePacket(dObjPacket);
			return;
		}
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

	private void parseDistributedObject(InternalPacket6DistributedObjectCreation dObjPacket) {
		String[] text = dObjPacket.getObjectText().split(";");
		
		int position = 0;
		String name = text[position].split("=")[1];
		position++;
		long id = Long.parseLong(text[position].split("=")[1].trim());
		position++;
		int zId = Integer.parseInt(text[position].split("=")[1].trim());
		position++;
		
		DistributedObject dObject = this.clientRepository.createDistributedObject(name);
		dObject.setId(id);
		dObject.setZoneId(zId);
		
		this.configureDistributedObject(text, position, dObject);
		this.getObjectManager(zId).addDistributedObject(dObject);
		this.onDistributedObjectReceived(dObject.getZoneId(), dObject.getId());
	}
	
	private void editDistributedObject(InternalPacket8DistributedObjectEdit dObjPacket) {
	   String[] text = dObjPacket.getDObjectText().split(";");
		
		int position = 0;
		String name = text[position].split("=")[1];
		position++;
		long id = Long.parseLong(text[position].split("=")[1].trim());
		position++;
		int zId = Integer.parseInt(text[position].split("=")[1].trim());
		position++;
		
		DistributedObject dObject = this.getObjectManager(zId).getDistributedObject(id);
		if (dObject == null) {
			dObject = this.clientRepository.createDistributedObject(name);
			dObject.setId(id);
			dObject.setZoneId(zId);
			this.configureDistributedObject(text, position, dObject);
			this.getObjectManager(zId).addDistributedObject(dObject);
			this.onDistributedObjectReceived(zId, id);
			return;
		}
		this.configureDistributedObject(text, position, dObject);
		this.onDistributedObjectUpdated(zId, id);
	}

	private void configureDistributedObject(String[] text, int position, DistributedObject dObject) {
		int numBytes = Integer.parseInt(text[position].split("=")[1]);
		for (int index = 0; index < numBytes; index++) {
		    position++;
			dObject.setByteValue(text[position].split("=")[0], Byte.parseByte(text[position].split("=")[1]));
		}
		
		position++;
		int numByteArrays = Integer.parseInt(text[position].split("=")[1]);
		
		for (int index = 0; index < numByteArrays; index++) {
			position++;
			dObject.setByteArrayValue(text[position].split("=")[0], text[position].split("=")[1].getBytes());
		}
		
		position++;
		int numStrings = Integer.parseInt(text[position].split("=")[1]);
		
		for (int index = 0; index < numStrings; index++) {
			position++;
			dObject.setStringValue(text[position].split("=")[0], text[position].split("=")[1]);
		}
		
		position++;
		int numIntegers = Integer.parseInt(text[position].split("=")[1]);
		
		for (int index = 0; index < numIntegers; index++) {
			position++;
			dObject.setIntegerValue(text[position].split("=")[0], Integer.parseInt(text[position].split("=")[1]));
		}
		
		position++;
		int numDoubles = Integer.parseInt(text[position].split("=")[1]);
		
		for (int index = 0; index < numDoubles; index++) {
			position++;
			dObject.setDoubleValue(text[position].split("=")[0], Double.parseDouble(text[position].split("=")[1]));
		}
		
		position++;
		int numFloats = Integer.parseInt(text[position].split("=")[1]);
		
		for (int index = 0; index < numFloats; index++) {
			position++;
			dObject.setFloatValue(text[position].split("=")[0], Float.parseFloat(text[position].split("=")[1]));
		}
		
		position++;
		int numShorts = Integer.parseInt(text[position].split("=")[1]);
		
		for (int index = 0; index < numShorts; index++) {
			position++;
			dObject.setShortValue(text[position].split("=")[0], Short.parseShort(text[position].split("=")[1]));
		}
		
		position++;
		int numLongs = Integer.parseInt(text[position].split("=")[1]);
		
		for (int index = 0; index < numLongs; index++) {
			position++;
			dObject.setLongValue(text[position].split("=")[0], Long.parseLong(text[position].split("=")[1]));
		}
		
		position++;
		int numBooleans = Integer.parseInt(text[position].split("=")[1]);
		
		for (int index = 0; index < numBooleans; index++) {
			position++;
			dObject.setBooleanValue(text[position].split("=")[0], Boolean.parseBoolean(text[position].split("=")[1]));
		}
	}

	public abstract void onDistributedObjectReceived(int zoneId, long id);
	
	public abstract void onDistributedObjectUpdated(int zoneId, long id);
	
	public abstract void onDistributedObjectDestroyed(int zoneId, long dObjectId);
	
	public abstract void onZoneLeave(long zoneId, Long[] dObjectIds);

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
				this.clientImpl.close();
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
	
	public void registerDistributedObject(String type, Class<? extends DistributedObject> dobject) {
		this.clientRepository.registerDistributedObject(type, dobject);
	}
	
	public DistributedObject createDistributedObject(String type) {
		return this.clientRepository.createDistributedObject(type);
	}
	
	public ObjectManager getObjectManager(int zoneId) {
		if (zoneId == -1) {
			return this.dObjManager;
		} else {
			synchronized (this.clientZones) {
				ObjectManager cZone = this.clientZones.get(zoneId);
				if (cZone == null) {
					cZone = new ObjectManager();
					this.clientZones.put(zoneId, cZone);
				}
				return cZone;
			}
		}
	}
	
	public ObjectManager getGeneralObjectManager() {
		return this.getObjectManager(-1);
	}
}
