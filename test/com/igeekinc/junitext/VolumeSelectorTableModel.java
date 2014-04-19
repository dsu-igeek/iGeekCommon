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
 
package com.igeekinc.junitext;

import java.beans.PropertyChangeListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.table.AbstractTableModel;

import com.igeekinc.util.ClientFile;
import com.igeekinc.util.FilePath;
import com.igeekinc.util.SystemInfo;
import com.igeekinc.util.Volume;
import com.igeekinc.util.VolumeManager;

class VolumeInfoComparator implements Comparator
{
    public int compare(Object o1, Object o2)
    {
        VolumeInfo v1, v2;
        v1 = (VolumeInfo)o1;
        v2 = (VolumeInfo)o2;
        String s1 = v1.getVolume().getFsType()+v1.getVolume().getVolumeName();
        String s2 = v2.getVolume().getFsType()+v2.getVolume().getVolumeName();
        return s1.compareTo(s2);
    }    
}

public class VolumeSelectorTableModel extends AbstractTableModel
{
    /**
     * 
     */
    private static final long serialVersionUID = -8824378610204427575L;
    private VolumeManager volumeManager;
    private VolumeInfo [] volumeInfo;
    
    public VolumeSelectorTableModel()
    {
        volumeInfo = new VolumeInfo[0];
        volumeManager = SystemInfo.getSystemInfo().getVolumeManager();
        volumeManager.addPropertyChangeListener(new PropertyChangeListener(){
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                update();
            };
        });
        update();
    }
    
    synchronized void update()
    {
        Volume [] volumes = volumeManager.getVolumes();
        VolumeInfo [] newVolumeInfo = new VolumeInfo[volumes.length];
        for (int curVolumeNum = 0; curVolumeNum < volumes.length; curVolumeNum++)
        {
            VolumeInfo curVolumeInfo = null;
            for (int oldVolumeNum = 0; oldVolumeNum < volumeInfo.length; oldVolumeNum++)
            {
                if (volumeInfo[oldVolumeNum].getVolume().equals(volumes[curVolumeNum]))
                {
                    curVolumeInfo = volumeInfo[oldVolumeNum];
                    break;
                }
            }
            if (curVolumeInfo == null)
            {
                curVolumeInfo = new VolumeInfo();
                curVolumeInfo.setVolume(volumes[curVolumeNum]);
                curVolumeInfo.setSelected(false);
                curVolumeInfo.setSelectedPath(volumes[curVolumeNum].getRoot().getFilePath());
            }
            newVolumeInfo[curVolumeNum] = curVolumeInfo;
        }
        Arrays.sort(newVolumeInfo, new VolumeInfoComparator());
        volumeInfo = newVolumeInfo;
        fireTableDataChanged();
    }
    
    public synchronized int getRowCount()
    {
        return volumeInfo.length;
    }

    public int getColumnCount()
    {
        return 4;
    }

    public synchronized Object getValueAt(int rowIndex, int columnIndex)
    {
        if (rowIndex >= 0 && rowIndex < volumeInfo.length)
        {
            switch(columnIndex)
            {
            case 0:
                return(volumeInfo[rowIndex].getVolume().getVolumeName());
            case 1:
                return(volumeInfo[rowIndex].getVolume().getFsType());
            case 2:
                return(volumeInfo[rowIndex].getSelectedPath().toString());
            case 3:
                return new Boolean(volumeInfo[rowIndex].isSelected());
            }
        }
        return null;
    }

    public String getColumnName(int columnIndex)
    {
        switch(columnIndex)
        {
        case 0:
            return("Volume Name");
        case 1:
            return("FS Type");
        case 2:
            return("Selected Path");
        case 3:
            return ("Selected");
        }
        return null;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        switch(columnIndex)
        {
        case 0:
            return false;
        case 1:
            return false;
        case 2:
            return true;
        case 3:
            return true;
        }
        return false;
    }

    public synchronized void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
        if (rowIndex < volumeInfo.length)
        {
            switch(columnIndex)
            {
            case 0:
                break;
            case 1:
                break;
            case 2:
                volumeInfo[rowIndex].setSelectedPath(FilePath.getFilePath((String)aValue));
                break;
            case 3:
                volumeInfo[rowIndex].setSelected(((Boolean)aValue).booleanValue());
            }
        }
    }
    
    public VolumeInfo [] getSelectedVolumes()
    {
        ArrayList returnVolumesList = new ArrayList();
        for (int curVolumeNum = 0; curVolumeNum < volumeInfo.length; curVolumeNum++)
        {
            if (volumeInfo[curVolumeNum].isSelected())
                returnVolumesList.add(volumeInfo[curVolumeNum]);
        }
        VolumeInfo [] returnVolumes = new VolumeInfo[returnVolumesList.size()];
        returnVolumes = (VolumeInfo [])returnVolumesList.toArray(returnVolumes);
        return returnVolumes;
    }
    
    public void selectPath(String pathToSelect) throws IOException
    {
        ClientFile fileForPath = SystemInfo.getSystemInfo().getClientFileForPath(pathToSelect);
        if (fileForPath == null)
            throw new FileNotFoundException("Couldn't find file for " + pathToSelect);
        Volume volumeForPath = fileForPath.getVolume();
        for (int curVolumeNum = 0; curVolumeNum < volumeInfo.length; curVolumeNum++)
        {
            VolumeInfo curVolumeInfo = volumeInfo[curVolumeNum];
            if (curVolumeInfo.getVolume().equals(volumeForPath))
            {
                curVolumeInfo.setSelected(true);
                curVolumeInfo.setSelectedPath(fileForPath.getFilePath());
                break;
            }
        }
    }
}
