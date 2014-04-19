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

import com.igeekinc.util.FilePath;
import com.igeekinc.util.Volume;

public class VolumeInfo
{
    private Volume volume;
    private boolean selected;
    private FilePath selectedPath;
    public boolean isSelected()
    {
        return selected;
    }
    public void setSelected(boolean selected)
    {
        this.selected = selected;
    }
    public FilePath getSelectedPath()
    {
        return selectedPath;
    }
    public void setSelectedPath(FilePath selectedPath)
    {
        this.selectedPath = selectedPath;
    }
    public Volume getVolume()
    {
        return volume;
    }
    public void setVolume(Volume volume)
    {
        this.volume = volume;
    }
    
    public String toString()
    {
    	return selectedPath.toString() + "("+volume.getFsType()+")";
    }
}
