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
import com.Unkn0wn0ne.unknownet.client.net.InternalPacket2Handshake;
import com.Unkn0wn0ne.unknownet.client.net.InternalPacket3KeepAlive;
import com.Unkn0wn0ne.unknownet.client.net.Packet;
import com.Unkn0wn0ne.unknownet.client.net.Packet.PACKET_PRIORITY;

/**
 * UnknownClient - Abstract class for connecting to an UnknownNet server.
 * @author Unkn0wn0ne
 */
public abstract class UnknownClient implements Runnable{

	public Logger logger = Logger.getLogger("UnknownNet");
	
	private ClientRepository clientRepository = new ClientRepository();
	
	private String ipAddress = "";
	private int port = 4334;
	private boolean useSSL = false;
	private String protocolVersion = "unknownserver-dev";
	private boolean useTCP = false;
	
	private Socket socket = null;
	private DatagramSocket dSocket = null;
	
	private DataInputStream dataInputStream = null;
	private DataOutputStream dataOutputStream = null;
	private DatagramPacket dPacket = null;
	private ByteArrayOutputStream udpWriter = null;
	private ByteArrayInputStream udpReader = null;
	private int uid = -1;
	
	private Queue<Packet> internalsToBeSent = new LinkedList<Packet>();
	private Queue<Packet> highsToBeSent = new LinkedList<Packet>();
	private Queue<Packet> lowsToBeSent = new LinkedList<Packet>();
	
	private Packet packet = null;
	private Packet internal = null;
	private InternalPacket3KeepAlive keepAlivePacket = null;
	
	private long lastReceivedKeepAlive;
	
	private String[] loginParams = null;

	private boolean shouldDisconnect = false;
	
	/**
	 * Creates an UnknownClient object for use in connecting to an UnknownNet server.
	 * @param useSSL Whether or not to connect to the server using SSL (Secure Socket Layer)
	 * @param protocolVersion The version of your custom protocol, used by the server to verify your client is up to date.
	 * @param useTCP Whether or not the client is using TCP. If set to false, the client uses UDP
	 */
	public UnknownClient(boolean useSSL, String protocolVersion, boolean useTCP) {
		this.useSSL = useSSL;
		this.protocolVersion = protocolVersion;
		this.useTCP = useTCP;
		
		this.clientRepository.init();
		try {
			this.keepAlivePacket = (InternalPacket3KeepAlive)this.clientRepository.getPacket(-3);
		} catch (ProtocolViolationException e) {
			this.logger.severe("Internal/UnknownClient: ProtocolViolationException while creating keep alive packet, this should never happen.");
			this.keepAlivePacket = new InternalPacket3KeepAlive();
		}
	}
	
	/**
	 * Connects the client to a UnknownNet-based server
	 * @param ip The IP address or hostname of the server to connect to
	 * @param port The port that the server is running on
	 * @param loginData A string array full of data that will be sent to the server for authentication purposes. This can be null and what this contains is completely up to your implementation
	 */
	public void connect(String ip, int port, String[] loginData) {
		this.ipAddress = ip;
		this.port = port;
		this.loginParams = loginData;
		new Thread(this).start();
	}
	
	/**
	 * Internal method.
	 * Handles the client connection along with sending packets
	 */
	@Override
	public void run() {
		if (this.useTCP) {
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
				this.dataInputStream = new DataInputStream(this.socket.getInputStream());
				this.dataOutputStream = new DataOutputStream(this.socket.getOutputStream());
				
				InternalPacket2Handshake handshakePacket = (InternalPacket2Handshake) this.clientRepository.getPacket(-2);
				handshakePacket.setVariables(this.protocolVersion, (this.loginParams != null) ? true : false, this.loginParams);
				handshakePacket._write(this.dataOutputStream);
				int id = this.dataInputStream.readInt();
				handshakePacket.read(this.dataInputStream);
				
				if (handshakePacket.getResponse() == false) {
					// A getResponse() of false mandates a reason, so a InternalPacket1Kick will be sent explaining the reason
					id = this.dataInputStream.readInt();
					InternalPacket1Kick disconnectPacket = (InternalPacket1Kick)this.clientRepository.getPacket(-1);
					disconnectPacket.read(this.dataInputStream);
					String reason = disconnectPacket.getReason();
					
					logger.info("Internal/UnknownClient: Server is kicking us out! Message: " + reason);
					this.socket.close();
					this.onConnectionFailed(reason);
					return;
				}
			} catch (ProtocolViolationException e) {
				logger.severe("Internal/UnknownClient: Failed to connect to server; a ProtocolViolationException has occurred.  (Message: " + e.getMessage() + ")");
				this.onConnectionFailed("Failed to connect to server; an ProtocolViolationException has occurred. (Message: " + e.getMessage() + ")");
				e.printStackTrace();
				return;
			} catch (IOException e) {
				logger.severe("Internal/UnknownClient: Failed to connect to server; an IOException has occurred.  (Message: " + e.getMessage() + ")");
				this.onConnectionFailed("Failed to connect to server; an IOException has occurred. (Message: " + e.getMessage() + ")");
				e.printStackTrace();
				return;
			}
			
			logger.info("Internal/UnknownClient: Connection to " + ipAddress + ":" + port + " succeeded.");
			
			new Thread(new Runnable() {
				@Override
				public void run() {
					UnknownClient.this.doReadLoop();
				}
			}).start();
			
			while (this.socket.isConnected()) {
				while (!this.highsToBeSent.isEmpty()) {
					try {
						this.highsToBeSent.poll()._write(this.dataOutputStream);
					} catch (IOException e) {
						// TODO
					}
				}
				
				while (!this.internalsToBeSent.isEmpty()) {
					try {
						this.internal = this.internalsToBeSent.poll();
						this.internal._write(this.dataOutputStream);
						
						if (this.internal instanceof InternalPacket1Kick) {
							this.socket.close();
							return;
						}
					} catch (IOException e) {
						// TODO
					}
				}
				
				while (!this.lowsToBeSent.isEmpty()) {
					try {
						this.lowsToBeSent.poll()._write(dataOutputStream);
					} catch (IOException e) {
						// TODO
					}
				}
				
				try {
					Thread.sleep(25);
				} catch (InterruptedException e) {
					logger.warning("Internal/UnknownClient: InterruptedEception occurred while sleeping client writing thread. Ignoring.");
				}
			}
		} else {
			this.logger.info("Internal/UnknownClient: Running with UDP.");
			
			if (this.useSSL) {
				this.logger.warning("Internal/UnknownClient: SSL over UDP is not supported in UnknownNet. Running without SSL.");
			}
			
			this.logger.info("Internal/UnknownClient: Connecting to " + this.ipAddress + ":" + this.port + " via datagram socket.");
			
			try {
				this.dSocket = new DatagramSocket();
				this.dSocket.connect(InetAddress.getByName(this.ipAddress), this.port);
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
			
			try {
				this.dPacket = new DatagramPacket(new byte[this.dSocket.getReceiveBufferSize()], this.dSocket.getReceiveBufferSize());
				InternalPacket2Handshake handshakePacket = (InternalPacket2Handshake) this.clientRepository.getPacket(-2);
				handshakePacket.setVariables(this.protocolVersion, (this.loginParams != null) ? true : false, this.loginParams);
				handshakePacket._write(this.dataOutputStream);
				
				this.dataOutputStream.flush();
				
				this.dSocket.receive(this.dPacket);
				
				this.udpReader = new ByteArrayInputStream(this.dPacket.getData());
				this.dataInputStream = new DataInputStream(this.udpReader);
				
				int id = this.dataInputStream.readInt();
				handshakePacket.read(this.dataInputStream);
				
				if (handshakePacket.getResponse() == false) {
						// A getResponse() of false mandates a reason, so a InternalPacket1Kick will be sent explaining the reason
				        closeAndLoad();
						id = this.dataInputStream.readInt();
						InternalPacket1Kick disconnectPacket = (InternalPacket1Kick)this.clientRepository.getPacket(-1);
						disconnectPacket.read(this.dataInputStream);
						String reason = disconnectPacket.getReason();
						logger.info("Internal/UnknownClient: Server is kicking us out! Message: " + reason);
					this.socket.close();
					this.onConnectionFailed(reason);
					return;
				}
				this.uid = this.dataInputStream.readInt();
				} catch (ProtocolViolationException e) {
					logger.severe("Internal/UnknownClient: Failed to connect to server; a ProtocolViolationException has occurred.  (Message: " + e.getMessage() + ")");
					this.onConnectionFailed("Failed to connect to server; an ProtocolViolationException has occurred. (Message: " + e.getMessage() + ")");
					e.printStackTrace();
					return;
				} catch (IOException e) {
					logger.severe("Internal/UnknownClient: Failed to connect to server; an IOException has occurred.  (Message: " + e.getMessage() + ")");
					this.onConnectionFailed("Failed to connect to server; an IOException has occurred. (Message: " + e.getMessage() + ")");
					e.printStackTrace();
					return;
				}
			
			logger.info("Internal/UnknownClient: Connection to " + ipAddress + ":" + port + " succeeded.");
			
			new Thread(new Runnable() {
				@Override
				public void run() {
					UnknownClient.this.doUDPReadLoop();
				}
				
			}).start();
			while (true) {
				try {
					Thread.sleep(25);
				} catch (InterruptedException e) {

				}
				
				while (!this.highsToBeSent.isEmpty()) {
					try {
						this.dataOutputStream.writeInt(this.uid);
						this.highsToBeSent.poll()._write(this.dataOutputStream);
						this.dataOutputStream.flush();
						this.dPacket.setData(this.udpWriter.toByteArray());
						this.dPacket.setLength(this.dPacket.getData().length);
						this.dSocket.send(this.dPacket);
						this.udpWriter.reset();
					} catch (IOException e) {
						// TODO
					}
				}
				
				while (!this.internalsToBeSent.isEmpty()) {
					try {
						this.dataOutputStream.writeInt(this.uid);
						this.internal = this.internalsToBeSent.poll();
						this.internal._write(this.dataOutputStream);
						this.dataOutputStream.flush();
						this.dPacket.setData(this.udpWriter.toByteArray());
						this.dPacket.setLength(this.dPacket.getData().length);
						this.dSocket.send(this.dPacket);
						this.udpWriter.reset();
						if (this.internal instanceof InternalPacket1Kick) {
							this.shouldDisconnect = true;
							this.dSocket.close();
							return;
						}
					} catch (IOException e) {
						// TODO
					}
				}
				
				while (!this.lowsToBeSent.isEmpty()) {
					try {
						this.dataOutputStream.writeInt(this.uid);
						this.lowsToBeSent.poll()._write(dataOutputStream);
						this.dataOutputStream.flush();
						this.dPacket.setData(this.udpWriter.toByteArray());
						this.dPacket.setLength(this.dPacket.getData().length);
						this.dSocket.send(this.dPacket);
						this.udpWriter.reset();
					} catch (IOException e) {
						// TODO
					}
				}
				
			}
		}
	}

	private void closeAndLoad() throws IOException {
	    this.dataInputStream.close();
		this.udpReader = null;
	    this.dataInputStream = null;
	    
	    this.dSocket.receive(this.dPacket);
	    
	    this.udpReader = new ByteArrayInputStream(this.dPacket.getData());
	    this.dataInputStream = new DataInputStream(this.udpReader);
	}

	/**
	 * Internal method
	 * This loop runs in a separate thread and handles reading the packet ids from the stream and than calling handlePacketReceive
	 */
	protected void doReadLoop() {
		while (this.socket.isConnected()) {
			try {
				if (this.dataInputStream.available() > 0) {
					int id = this.dataInputStream.readInt();
					this.handlePacketReceive(id);
				}
			} catch (IOException e) {
				// TODO
			} catch (ProtocolViolationException e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(25);
			} catch (InterruptedException e) {
				logger.warning("Internal/UnknownClient: InterruptedEception occurred while sleeping client reading thread. Ignoring.");
			}
		}
	}

	protected void doUDPReadLoop() {
		while (!this.shouldDisconnect ) {
			try {
				Thread.sleep(25);
			} catch (InterruptedException e) {
				
			}
			
			try {
				this.closeAndLoad();
				int id = this.dataInputStream.readInt();
				this.handlePacketReceive(id);
			} catch (IOException e) {
				
			} catch (ProtocolViolationException e) {
				
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
	private void handlePacketReceive(int id) throws ProtocolViolationException, IOException {
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
			kickPacket.read(this.dataInputStream);
			logger.info("Internal/UnknownClient: Server is kicking us out! Message: " + kickPacket.getReason());
			this.onClientKicked(kickPacket.getReason());
			return;
		}
		default: {
			this.packet = this.clientRepository.getPacket(id);
			this.packet.read(this.dataInputStream);
			this.onPacketReceived(this.packet);
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
	 * @param packet The packet that has been received
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
	public void leaveServer() {
		try {
			InternalPacket1Kick disconnectPacket = (InternalPacket1Kick) this.clientRepository.getPacket(-1);
			disconnectPacket.setVariables("Leaving");
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
