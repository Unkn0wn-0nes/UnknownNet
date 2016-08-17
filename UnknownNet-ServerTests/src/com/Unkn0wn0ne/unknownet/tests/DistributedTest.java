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
package com.Unkn0wn0ne.unknownet.tests;

import java.util.Random;

import com.Unkn0wn0ne.unknownnet.server.UnknownClient;
import com.Unkn0wn0ne.unknownnet.server.UnknownServer;
import com.Unkn0wn0ne.unknownnet.server.distributed.ClientZone;
import com.Unkn0wn0ne.unknownnet.server.net.Packet;

public class DistributedTest extends UnknownServer{

	private int numClients = 0;
	private ClientZone generalZone = new ClientZone(100);
	private ClientZone playerZone = new ClientZone(200);
	
	private Random random = new Random();
	
	public DistributedTest() {
		super();
		this.registerPacket(2, Packet2PlayerZone.class);
		for (int x = 0; x < 25; x++) {
			playerZone.spawnDistributedObject(new DistributedPlayer(random.nextFloat(), random.nextFloat()));
		}
		this.startServer();
	}
	
	@Override
	public void mainLoop() {
		if (this.getConnectedClients().size() <= 0) {
			return;
		}
		int x = random.nextInt(9000);
		
		if (x == 43) {
			int y = random.nextInt(this.getConnectedClients().size());
			
			UnknownClient c = this.getConnectedClients().get(y);
			DistributedClient dc = (DistributedClient) c.getClientTag();
			dc.setStringValue("cName", "Client-1337");
			generalZone.updateObject(dc.getId());
		}
	}

	@Override
	public boolean handleNewConnection(UnknownClient client, String[] loginData) {
		this.numClients++;
		generalZone.addClient(client);
		DistributedClient c = new DistributedClient("Client-" + numClients);
		client.setObjectTag(c);
		generalZone.spawnDistributedObject(c);
		return true;
	}

	@Override
	public void onClientLeave(UnknownClient client) {
		this.generalZone.removeClient(client);
		this.generalZone.destroyObject(((DistributedClient)client.getClientTag()).getId());
	}
 
	@Override
	public void onPacketReceived(UnknownClient client, Packet packet) {
		if (packet instanceof Packet2PlayerZone) {
			boolean isJoining = ((Packet2PlayerZone)packet).isJoining();
			
			if (isJoining) {
				this.playerZone.addClient(client);
			} else {
				this.playerZone.removeClient(client);
			}
		}
	}
}
