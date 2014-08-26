package com.Unkn0wn0ne.unknownnet.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;

import com.Unkn0wn0ne.unknownnet.server.net.Packet;
import com.Unkn0wn0ne.unknownnet.server.net.ServerRepository;

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
	private boolean isAllowingClients = false;
	
	public UnknownServer() {
		
	}
	
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
				Thread.sleep(50);
			} catch (InterruptedException e) {
				
			}
			this.mainLoop();
		}
	}
	
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

	private void handleNewClient(Socket socket) {
		UnknownClient client = new UnknownClient(socket, this);
		
		if (!serverGuard.verifyClient(client)) {
			return;
		}
		
		this.numSessionClients++;
		client.setId(this.numSessionClients);
		client.start();
	}
	
	public abstract boolean handleNewConnection(UnknownClient client, String[] loginData);
	
	public void setClientAsAdministrator(UnknownClient client) {
		if (client.getState() == -1) {
			logger.warning("Internal/UnknownServer: Tried to set non-authenicated client as an administrator. This functionality is not supported for security reasons. Ignoring request");
		}
		client.setState(1);
	}

	public void freeClientFromSandbox(UnknownClient unknownClient) {
		this.numClients++;
		synchronized (this.connectedClients) {
			this.connectedClients.add(unknownClient);
		}
	}

	protected ServerRepository getRepository() {
		return this.serverRepository;
	}

	public String getProtocolVersion() {
		return this.configManager.getProtocolVersion();
	}

	public void removeClient(UnknownClient unknownClient) {
		handleClientLeaving(unknownClient);
	}

	public void handleClientLeaving(UnknownClient unknownClient) {
		this.numClients--;
		synchronized (this.connectedClients) {
			this.connectedClients.remove(unknownClient);
		}
		this.onClientLeave(unknownClient);
	}
	
	public abstract void onClientLeave(UnknownClient client);
	
	public abstract void onPacketReceived(UnknownClient client, Packet packet);
	
	public void registerPacket(int id, Class<? extends Packet> packet) {
		this.serverRepository.registerPacket(id, packet);
	}
	
	public List<UnknownClient> getConnectedClients() {
		synchronized (this.connectedClients) {
			return this.connectedClients;
		}
	}
	
	public Packet createPacket(int id) throws com.Unkn0wn0ne.unknownnet.server.net.errors.ProtocolViolationException {
		return this.serverRepository.getPacket(id);
	}

	protected ServerGuard getSeverGuard() {
		return this.serverGuard;
	}
	
	public int getMaxClients() {
		return this.maxClients;
	}
	
	public void setMaxClients(int numMax) {
		this.maxClients = numMax;
	}
	
	public boolean isServerAllowingClients() {
		return this.isAllowingClients;
	}
	
	public void setAllowingClients(boolean allow) {
		this.isAllowingClients = allow;
	}
}
