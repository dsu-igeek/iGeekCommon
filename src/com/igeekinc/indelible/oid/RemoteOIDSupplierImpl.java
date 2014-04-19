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
 
package com.igeekinc.indelible.oid;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RemoteOIDSupplierImpl extends UnicastRemoteObject implements RemoteOIDSupplier
{
    private static final long serialVersionUID = 1956300292067873432L;
    ObjectIDFactory localFactory;
    public RemoteOIDSupplierImpl(ObjectIDFactory localFactory) throws RemoteException
    {
        this.localFactory = localFactory;
    }
    public ObjectID setTimeAndUniquifier(ObjectID target)
            throws RemoteException
    {
        return localFactory.setTimeAndUniquifier(target);
    }
    
    public GeneratorID getGeneratorID() throws RemoteException
    {
        return localFactory.generatorID;
    }
    
}
