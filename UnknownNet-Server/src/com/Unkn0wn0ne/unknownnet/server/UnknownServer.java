package com.Unkn0wn0ne.unknownnet.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;

import com.Unkn0wn0ne.unknownnet.server.net.Packet;
import com.Unkn0wn0ne.unknownnet.server.net.ServerRepository;

/**
 * UnknownServer - An abstract class that manages the various functions to keep an UnknownServer alive and functional
 * @author Unkn0wn0ne
 */
public abstract class UnknownServer implements Runnable {

	public Logger logger = Logger.getLogger("UnknownNet");
	
	private ConfigurationManager configManager = new ConfigurationManager();
	private ServerRepository serverRepository = new ServerRepository();
	private ServerGuard serverGuard = new ServerGuard();
	
	private boolean isRunning = false;
	private List<UnknownClient> connectedClients = new CopyOnWriteArrayList<UnknownClient>();
	
	private int maxClients = configManager.getMaxClients();
	private int numClients = 0;
	
	private int numSessionClients = 0;
	private boolean isAllowingClients = true;
	
	private long sleep = 50;
	
	/**
	 * Creates an UnknownServer with the main thread loop being called every 50 milliseconds
	 */
	public UnknownServer() {
		this.logger.addHandler(new ConsoleHandler());
		this.logger.addHandler(new FileLogHandler());
	}
	
	/**
	 * Creates an UnknownServer with the main thread sleeping with the specified sleep time
	 * @param mainSleep How long the main server thread should sleep before calling {@link UnknownServer#mainLoop()} again
	 */
	public UnknownServer(long mainSleep) {
		this();
		this.sleep = mainSleep;
	}
	
	/**
	 * Starts the server functions
	 */
	public void startServer() {
		this.serverRepository.init();
		this.isRunning = true;
		new Thread(this).start();
		new Thread(new Runnable() {
			@Override
			public void run() {
				doKeepAliveLoop();
			}	
		}).start();
		while (this.isRunning) {
			try {
				Thread.sleep(this.sleep);
			} catch (InterruptedException e) {
				
			}
			this.mainLoop();
		}
	}
	
	/**
	 * Internal method. Do not call
	 * Runs a loop that sends a keep alive packet to a client every 30 seconds
	 */
	private void doKeepAliveLoop() {
		while (this.isRunning) {
			try {
				Thread.sleep(30000);
			} catch (InterruptedException e) {
			}
			
			synchronized (this.connectedClients) {
				for (UnknownClient client : this.connectedClients) {
					if (client.hasBeenEjected()) {
						this.connectedClients.remove(client);
					}
					client.sendKeepAlive();
				}
			}
		}
	}

	/**
	 * Called to keep the server main thread alive by default every 50 milliseconds, but this can be adjusted by using the UnknownServer(long sleepMain) constructor
	 */
	public abstract void mainLoop();
	
	@Override
	public void run() {
		ServerSocket serv_socket = null;
		if (!configManager.useSSL()) {
			logger.warning("Internal/UnknownServer: Server not configured to use SSL. This can pose as a security issue on production servers");
			try {
				serv_socket = new ServerSocket(configManager.getServerPort());
			} catch (IOException e) {
				logger.severe("Internal/UnknownServer: Failed to create server socket, an IOException ocurred.");
				e.printStackTrace();
				logger.severe("Internal/UnknownServer: Shutting down server.");
				System.exit(1);
			}
			
		} else {
			ServerSocketFactory sslSocketFactory = SSLServerSocketFactory.getDefault();
			try {
				serv_socket = sslSocketFactory.createServerSocket(configManager.getServerPort());
			} catch (IOException e) {
				logger.severe("Internal/UnknownServer: Failed to create SSL server socket, an IOException ocurred.");
				e.printStackTrace();
				logger.severe("Internal/UnknownServer: Shutting down server.");
				System.exit(1);
			}
		}
		
		
		while (!serv_socket.isClosed() && this.isRunning ) {
			try {
				Socket socket = serv_socket.accept();
				socket.setTcpNoDelay(this.configManager.getTCPNoDelay());
				socket.setKeepAlive(this.configManager.getKeepAlive());
				socket.setTrafficClass(this.configManager.getTrafficClass());
				
				if (this.numClients >= this.maxClients) {
					// Server is completely full, silently disconnect the client
					this.silentlyDisconnect(socket, "The server is full.");
					continue;
				} else if (!this.isAllowingClients) {
					this.silentlyDisconnect(socket, "The server is currently not accepting new connections at the moment.");
					return;
				}
				logger.info("Internal/UnknownServer: Client connection from '" + socket.getInetAddress().getHostAddress() + "'");
				handleNewClient(socket);
			} catch (IOException e) {
				logger.warning("Internal/UnknownServer: Failed to accept client, an IOException has occurred.");
			}
		}
		
		try {
			serv_socket.close();
		} catch (IOException e) {
			logger.severe("Internal/UnknownServer: Failed to close server socket, aborting...");
		}
	}

	
	/**
	 * Internal method. Do not call.
	 * Disconnects the client without logging any notice of the client connecting or being disconnected
	 * @param socket The socket of the connection you'd wish to disconnect
	 * @param string The message to be sent to the client
	 */
	private void silentlyDisconnect(Socket socket, String string) {
		try {
			DataOutputStream stream = new DataOutputStream(socket.getOutputStream());
			
			// Send a fake handshake failed packet
			stream.writeInt(-2);
			stream.writeBoolean(false);
			
			// Send a fake kick packet
			stream.writeInt(-1);
			stream.writeUTF(string);
			
			// Close the socket
			socket.close();
		} catch (IOException e) {
			try {
				socket.close();
			} catch (IOException e1) {
				// Ignore and let the server GC the object
			}
		}
		
	}

	/**
	 * Internal Method. Do not call
	 * Handles a new socket connection and starts the various tasks for preparing the client to be authenticated
	 * @param socket 
	 */
	private void handleNewClient(Socket socket) {
		UnknownClient client = new UnknownClient(socket, this);
		
		if (!serverGuard.verifyClient(client)) {
			return;
		}
		
		this.numSessionClients++;
		client.setId(this.numSessionClients);
		client.start();
	}
	
	/**
	 * Called when a client has connected to the server
	 * @param client The UnknownClient instance for this client
	 * @param loginData The custom array of login data sent by the client for authentication, may be null depending on your client-side implementation
	 * @return True to allow the client to join the server and leave the authentication sandbox, false to kick the client for failing authentication
	 */
	public abstract boolean handleNewConnection(UnknownClient client, String[] loginData);
	
	/**
	 * Sets the specified clients state to administrator (1). Allows it to perform administrative actions.
	 * For security purposes the client must be authenticated before this method can be called.
	 * @param client
	 */
	public void setClientAsAdministrator(UnknownClient client) {
		if (client.getState() == -1) {
			logger.warning("Internal/UnknownServer: Tried to set non-authenicated client as an administrator. This functionality is not supported for security reasons. Ignoring request");
		}
		client.setState(1);
	}

	/**
	 * Internal method. Do not call
	 * Frees the specified client from the authentication sandbox.
	 * @param unknownClient The client to be freed
	 */
	protected void freeClientFromSandbox(UnknownClient unknownClient) {
		this.numClients++;
		synchronized (this.connectedClients) {
			this.connectedClients.add(unknownClient);
		}
	}

	/**
	 * Internal Method. Do not Call
	 * Gets the ServerRepository instance for this server
	 * @return 
	 */
	protected ServerRepository getRepository() {
		return this.serverRepository;
	}

	/**
	 * Gets the protocol version that the server is using.
	 * @return The protocol version that the server is using
	 */
	public String getProtocolVersion() {
		return this.configManager.getProtocolVersion();
	}

	/**
	 * Internal method. Do not call.
	 * @param unknownClient The client that has either disconnected or been kicked
	 */
	protected void handleClientLeaving(UnknownClient unknownClient) {
		this.numClients--;
		synchronized (this.connectedClients) {
			this.connectedClients.remove(unknownClient);
		}
		this.onClientLeave(unknownClient);
	}
	
	/**
	 * Called when a client has disconnected or was ejected
	 * @param client
	 */
	public abstract void onClientLeave(UnknownClient client);
	
	/**
	 * Called when a packet has been received
	 * @param client The client the packet was received from
	 * @param packet The packet that was received
	 */
	public abstract void onPacketReceived(UnknownClient client, Packet packet);
	
	/**
	 * Registers a packet for use on the network. This only has to be called once per packet type
	 * @param id The packet id for the packet type you'd like to register
	 * @param packet The packet class you'd like to register.
	 */
	public void registerPacket(int id, Class<? extends Packet> packet) {
		this.serverRepository.registerPacket(id, packet);
	}
	
	/**
	 * Gets a list of all the clients currently connected.
	 * @return A list of all the clients currently connected
	 */
	public List<UnknownClient> getConnectedClients() {
		synchronized (this.connectedClients) {
			return this.connectedClients;
		}
	}
	
	/**
	 * Gets a packet object using the id provided
	 * @param id The id of the packet you'd like to get
	 * @return A packet object that is type specified
	 * @throws com.Unkn0wn0ne.unknownnet.server.net.errors.ProtocolViolationException If a protocol violation occurred
	 */
	public Packet createPacket(int id) throws com.Unkn0wn0ne.unknownnet.server.net.errors.ProtocolViolationException {
		return this.serverRepository.getPacket(id);
	}

	/**
	 * Gets the {@link ServerGuard} instance for the server
	 * @return The server guard instance for this server
	 */
	public ServerGuard getSeverGuard() {
		return this.serverGuard;
	}
	
	/**
	 * Gets the number of clients that the server will allow to be connection simultaneously
	 * @return the number of clients the server is currently allowing to be connected simultaneously
	 */
	public int getMaxClients() {
		return this.maxClients;
	}
	
	/**
	 * Sets the max number of client connections the server will allow to be connect simultaneously
	 * @param numMax The number of client connections that can be connected simultaneously
	 */
	public void setMaxClients(int numMax) {
		this.maxClients = numMax;
	}
	
	/**
	 * Gets if the server is currently not allowing clients
	 * @return if the server is allowing clients
	 */
	public boolean isServerAllowingClients() {
		return this.isAllowingClients;
	}
	
	/**
	 * Set whether or not the server should allow new clients to connect. An example of this function would be to prevent new clients if the server was near going down for maintenance  
	 * @param allow if the server is allowing new clients to connect
	 */
	public void setAllowingClients(boolean allow) {
		this.isAllowingClients = allow;
	}
}
