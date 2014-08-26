package com.Unkn0wn0ne.unknownnet.server.net;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.Unkn0wn0ne.unknownnet.server.net.errors.ProtocolViolationException;

public class ServerRepository {

	private Logger logger = Logger.getLogger("UnknownNet");
	private ConcurrentHashMap<Integer, Packet> registeredPackets = new ConcurrentHashMap<Integer, Packet>();
	
	public void init() {
		logger.info("Internal/ServerRepository: Init");
		registerPacket(-1, InternalPacket1Kick.class);
		registerPacket(-2, InternalPacket2Handshake.class);
		registerPacket(-3, InternalPacket3KeepAlive.class);
		registerPacket(-4, InternalPacket4AdministrativeAction.class);
	}
	
	public void registerPacket(int id, Class<? extends Packet> packet){
		logger.info("Internal/ServerRepository: Registering packet with id '" + id + "' to class '" + packet.getName() + "'");
		Packet nPacket;
		try {
			nPacket = packet.newInstance();
			this.registeredPackets.put(id, nPacket);
		} catch (InstantiationException e) {
			logger.severe("Failed to register packet id '" + id + "' An InstantiationException has occurred.");
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			logger.severe("Failed to register packet id '" + id + "' An IllegalAccessException has occurred.");
			e.printStackTrace();
		}
	}
	
	public Packet getPacket(int id) throws ProtocolViolationException {
		if (this.registeredPackets.get(id) == null) {
			logger.severe("Internal/ServerRepository: Protocol security violation, attempted to access a packet with non-existant id '" + id + "' ejecting client");
			throw new ProtocolViolationException("A protocol security violation has occurred. Attempted to access a packet with a non-existant id '" + id + "'");
		}
		return this.registeredPackets.get(id);
	}
}
