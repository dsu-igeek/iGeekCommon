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
 
package com.igeekinc.testutils;

import java.util.Date;

public class DateDifferentReason extends FileCompareExceptionReason
{
    private Date date1, date2;
    
    public DateDifferentReason(Date date1, Date date2)
    {
        this.date1 = date1;
        this.date2 = date2;
    }
    
    public Date getDate1()
    {
        return date1;
    }
    
    public Date getDate2()
    {
        return date2;
    }
    
    public String toString()
    {
        return("Dates differ, "+date1.toString()+" != "+date2.toString());
    }
}
