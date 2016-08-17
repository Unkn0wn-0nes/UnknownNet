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
package com.Unkn0wn0ne.unknownnet.tests;

import com.Unkn0wn0ne.unknownet.client.UnknownClient;
import com.Unkn0wn0ne.unknownet.client.distributed.DistributedObject;
import com.Unkn0wn0ne.unknownet.client.net.Packet;

public class DistributedTest extends UnknownClient{

	private boolean isRunning = true;

	public DistributedTest(boolean useSSL, String protocolVersion) {
		super(useSSL, protocolVersion);
		this.registerDistributedObject("DistributedClient", DistributedClient.class);
		this.registerDistributedObject("DistributedPlayer", DistributedPlayer.class);
		this.registerPacket(2, Packet2PlayerZone.class);
		String[] str = new String[1];
		str[0] = "d";
		//this.connectUDP("127.0.0.1", 4334, 4334, str);
		this.connectTCP("127.0.0.1", 4334, str);
		while (this.isRunning) {
			try {
				String msg = System.console().readLine();
				
				if (msg != null && msg.trim().equalsIgnoreCase("!joinPlayers")) {
					Packet2PlayerZone playerZone = (Packet2PlayerZone) this.createPacket(2);
					playerZone.setVariables(true);
					this.queuePacket(playerZone);
				} else if (msg != null && msg.trim().equalsIgnoreCase("!leavePlayers")) {
					Packet2PlayerZone playerZone = (Packet2PlayerZone) this.createPacket(2);
					playerZone.setVariables(false);
					this.queuePacket(playerZone);
				}
				Thread.sleep(100);
				
				System.gc();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	
	@Override
	public void onDistributedObjectReceived(int zoneId, long id) {
		DistributedObject dObj = this.getObjectManager(zoneId).getDistributedObject(id);
		System.out.println("DistributedObject received. Id: " + id);
		if (dObj instanceof DistributedClient) {
			System.out.println("DistributedClient received! Name: " + ((DistributedClient) dObj).getName());
		} else if (dObj instanceof DistributedPlayer) {
			DistributedPlayer dPlayer = (DistributedPlayer)dObj;
			System.out.println("DistributedPlayer received! (x:" + dPlayer.getX() + ", y:" + dPlayer.getY() + ")");
		}
	}

	@Override
	public void onDistributedObjectDestroyed(int zoneId,long dObjectId) {
		System.out.println("DistributedObject destroyed. Id: " + dObjectId);
	}

	@Override
	public void onConnectionSuccess() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectionFailed(String reason) {
		this.isRunning = false;
	}

	@Override
	public void onClientKicked(String reason) {
		this.isRunning = false;
	}

	@Override
	public void onPacketReceived(Packet packet) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onDistributedObjectUpdated(int zoneId, long id) {
		DistributedObject dObj = this.getObjectManager(zoneId).getDistributedObject(id);
		System.out.println("DistributedClient object updated! New Name: " + ((DistributedClient) dObj).getName());
	}


	@Override
	public void onZoneLeave(long zoneId, Long[] dObjectIds) {
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("Left Zone id:");
		strBuilder.append(zoneId);
		strBuilder.append(". Ids: ");
		
		for (long l : dObjectIds) {
			strBuilder.append(l);
			strBuilder.append(", ");
		}
		
		System.out.println(strBuilder.toString());
	}

}
