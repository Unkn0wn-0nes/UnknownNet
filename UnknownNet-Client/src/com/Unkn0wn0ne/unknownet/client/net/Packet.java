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
package com.Unkn0wn0ne.unknownet.client.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Packet - An abstract class that allows you to create packets for use on the UnknownNet network
 * @author Unkn0wn0ne
 */
public abstract class Packet {
	
	/**
	 * Gets the id of the packet. This should remain the same for all of packets of the same type.
	 * @return
	 */
	public abstract int getId();
	
	/**
	 * Called when the packet's data is to be sent over the network
	 * @param dataStream The DataOutputStream you can use to write data to the network
	 * @throws IOException If an IO error occurred while writing data
	 */
	public abstract void write(DataOutputStream dataStream) throws IOException;
	
	/**
	 * Called when the packet is being received. Allows you to read your data and handle it from the network.
	 * @param dataStream The DataInputStream you can use to read your packets data
	 * @throws IOException If an IO error occurred while reading the data
	 */
	public abstract void read(DataInputStream dataStream) throws IOException;
	
	/**
	 * UnknowNet has a focus on attempting to reduce allocations so that the GC isn't causing framerate loss or slowing down the network threads. This method simply acts as a constructor for your packet object. Allow variables should be assigned here.
	 * @param vars
	 */
	public abstract void setVariables(Object... vars);
	
	/**
	 * Gets the PACKET_PRIORITY of the packet. This can range from High or Normal for implementation packets and internal UnknownNet packets carry the INTERNAL priority.
	 * @return A value from the PACKET_PRIORITY enum
	 */
	public abstract PACKET_PRIORITY getPriority();
	
	public abstract PACKET_PROTOCOL getProtocol();
	
	/**
	 * Internal method. Do not call.
	 * Used by UnknownNet internals to write the packet id out before writing the data so the server can make sense of it.
	 */
	public void _write(DataOutputStream dataStream) throws IOException{
		dataStream.writeInt(this.getId());
		this.write(dataStream);
	}
	
	public enum PACKET_PROTOCOL {
		TCP,
		
		UDP
	}
	
	/**
	 * Used by UnknownNet to determine how quickly the packet needs to be sent
	 * @author John
	 */
	public enum PACKET_PRIORITY {
		
		/**
		 * For use ONLY in internal UnknownNet packets, these are sent out after HIGH but before NORMAL packets
		 */
		INTERNAL,
		
		/**
		 * High priority packets, these are sent out before INTERNAL and NORMAL packets
		 */
		HIGH,
		
		/**
		 * Normal packet, sent after HIGH and INTERNAL packets
		 */
		NORMAL;
	}
}
