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
package com.Unkn0wn0ne.unknownnet.server.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.Unkn0wn0ne.unknownnet.server.util.PoolableObject;

/**
 * Packet - The basic form of communication an UnknownNet network.
 * Packets are used as ways of transferring and receiving basic types of data (strings, floats, bytes, integers, etc) over the network.
 * @author John [Unkn0wn0ne]
 */
public abstract class Packet implements PoolableObject{
	
	private int numClients = 1;
	
	/**
	 * This function is used to identify specific types of packets
	 * @return A number specific to the packet's type. This number should not be in use by any other packet type.
	 */
	public abstract int getId();
	
	/**
	 * Called when a packet's data is going to be sent over the network.
	 * @param dataStream The {@link DataOutputStream} used to send data over the network
	 * @throws IOException
	 */
	public abstract void write(DataOutputStream dataStream) throws IOException;
	
	/**
	 * Called when a packet's data needs to be retrieved from the network.
	 * @param dataStream The {@link DataInputStream} used to retrieve data from the network
	 * @throws IOException If an I/O error occurs.
	 */
	public abstract void read(DataInputStream dataStream) throws IOException;
	
	/**
	 * @return The {@link PACKET_PRIORITY} used to determine when to send the packet
	 */
	public abstract PACKET_PRIORITY getPriority();
	
	/**
	 * This function is only used in the 'Dualstack (TCP + UDP)' configuration
	 * @return The {@link PACKET_PROTOCOL} used for sending the packet over.
	 */
	public abstract PACKET_PROTOCOL getProtocol();
	
	/** 
	 * Internal Method. Do not call.
	 * Used for sending some internal data used to classify packets and then calls {@link Packet#write(DataOutputStream)}
	 * @param dataStream The DataOutputStream used for writing the packet
	 * @throws IOException If a network I/O error occurs.
	 */
	public void _write(DataOutputStream dataStream) throws IOException{
		dataStream.writeInt(this.getId());
		this.write(dataStream);
	}
	
	@Override
	public void resetVariables() {
		this.numClients = 1;
		this.clearVariables();
	}
	
	/**
	 * Called to reset the packet's variables before it is reinserted into the pool. UnknownNet manages this automatically.
	 */
	public abstract void clearVariables();
	
	/**
	 * PACKET_PROTOCOL is used in the 'Dualstack (TCP + UDP)' configuration to determine what protocol to send the packet over
	 */
	public enum PACKET_PROTOCOL {
		TCP,
		
		UDP;
	}
	
	/**
	 * PACKET_PRIORITY handles the importance of a packet as to what order it is sent in.
	 * In UnknownNet, the packets are sent in this order by priority:
	 *    - HIGH -> INTERNAL -> NORMAL
	 */
	public enum PACKET_PRIORITY {
		
		INTERNAL,
		
		HIGH,
		
		NORMAL;
	}
	
	/**
	 * If you are sending a packet to more than one client, you must specify how many clients you are sending it to.
	 * @param numClients The number of clients you are sending the packet to
	 */
	public void setRecipentCount(int numClients) {
		synchronized (this) {
			this.numClients = numClients;
		}
	}
	
	/**
	 * @return The number of clients you are sending the packet to
	 */
	public int getRecipentCount() {
		synchronized (this) {
			return this.numClients;
		}
	}
}
