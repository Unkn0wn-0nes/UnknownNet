package com.Unkn0wn0ne.unknownet.client.net;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.Unkn0wn0ne.unknownet.client.errors.ProtocolViolationException;


public class ClientRepository {
	private Logger logger = Logger.getLogger("UnknownNet");
	private ConcurrentHashMap<Integer, Packet> registeredPackets = new ConcurrentHashMap<Integer, Packet>();
	
	public void init() {
		logger.info("Internal/ClientRepository: Init");
		
		//Register our internal packets
		registerPacket(-1, InternalPacket1Kick.class);
		registerPacket(-2, InternalPacket2Handshake.class);
		registerPacket(-3, InternalPacket3KeepAlive.class);
	}
	
	public void registerPacket(int id, Class<? extends Packet> packet){
		logger.info("Internal/ClientRepository: Registering packet with id '" + id + "' to class '" + packet.getName() + "'");
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
			logger.severe("Internal/ClientRepository: Protocol violation, attempted to access a packet with non-existant id '" + id + "' disconnecting. (Did you register the packet using UnknownClient.registerPacket?)");
			throw new ProtocolViolationException("A protocol violation has occurred. Attempted to access a packet with a non-existant id '" + id + "' (Did you register the packet using UnknownClient.registerPacket?)");
		}
		return this.registeredPackets.get(id);
	}
}
