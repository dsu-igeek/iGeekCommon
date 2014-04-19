/*
 * Copyright 2002-2014 iGeek, Inc.
 * All Rights Reserved
 * @Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.@
 */
 
package com.igeekinc.util;

import java.io.IOException;

import junit.framework.TestCase;

public class VolumeTest extends TestCase
{
/*
	public void testTotalSpace() throws IOException
	{

	}
*/
	public void testFreeSpace() throws IOException
	{
		VolumeManager volumeManager = SystemInfo.getSystemInfo().getVolumeManager();
		volumeManager.getVolumeForPath("/");	// Force us to wait for at least one volume to be available
		Volume [] checkVolumes = volumeManager.getVolumes();
		for (int checkVolumeNum = 0; checkVolumeNum < checkVolumes.length; checkVolumeNum++)
		{
			Volume checkVolume = checkVolumes[checkVolumeNum];
			long apacheFreeSpace = org.apache.commons.io.FileSystemUtils.freeSpaceKb(checkVolume.getRoot().getAbsolutePath());
			long ourFreeSpace = checkVolume.freeSpace()/1024;

			System.out.println("Volume = "+checkVolume.getRoot().toString()+", ourFreeSpace = "+ourFreeSpace+", apacheFreeSpace = "+apacheFreeSpace);
			//assertEquals(apacheFreeSpace, (ourFreeSpace/1024));
		}
	}

}
