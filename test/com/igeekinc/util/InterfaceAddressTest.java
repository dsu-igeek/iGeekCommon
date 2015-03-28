/*
 * Copyright 2002-2014 iGeek, Inc.
 * All Rights Reserved
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.igeekinc.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.igeekinc.junitext.iGeekTestCase;

public class InterfaceAddressTest extends iGeekTestCase
{
	public void testBasic() throws UnknownHostException
	{
		InetAddress ifAddrIP = InetAddress.getByName("192.168.0.77");
		InterfaceAddressInfo ifAddr = new InterfaceAddressInfo(ifAddrIP, 24);
		InetAddress address1 = InetAddress.getByName("192.168.0.1");
		InetAddress address2 = InetAddress.getByName("192.168.1.1");
		
		assertTrue(ifAddr.sameNetwork(address1));
		assertFalse(ifAddr.sameNetwork(address2));
	}
}
