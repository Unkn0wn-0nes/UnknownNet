package com.Unkn0wn0ne.unknownnet.server.net;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.Unkn0wn0ne.unknownnet.server.net.errors.ProtocolViolationException;

public class ServerRepository {

	private Logger logger = Logger.getLogger("UnknownNet");
	private ConcurrentHashMap<Integer, Class<? extends Packet>> registeredPackets = new ConcurrentHashMap<Integer, Class<? extends Packet>>();
	
	public void init() {
		logger.info("Internal/ServerRepository: Init");
		registerPacket(-1, InternalPacket1Kick.class);
		registerPacket(-2, InternalPacket2Handshake.class);
		registerPacket(-3, InternalPacket3KeepAlive.class);
		registerPacket(-4, InternalPacket4AdministrativeAction.class);
		registerPacket(-5, InternalPacket5Hello.class);
	}
	
	public void registerPacket(int id, Class<? extends Packet> packet){
		logger.info("Internal/ServerRepository: Registering packet with id '" + id + "' to class '" + packet.getName() + "'");
		this.registeredPackets.put(id, packet);
	}
	
	public Packet getPacket(int id) throws ProtocolViolationException {
		if (this.registeredPackets.get(id) == null) {
			logger.severe("Internal/ServerRepository: Protocol security violation, attempted to access a packet with non-existant id '" + id + "' ejecting client");
			throw new ProtocolViolationException("A protocol security violation has occurred. Attempted to access a packet with a non-existant id '" + id + "'");
		}
		try {
			return this.registeredPackets.get(id).newInstance();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
