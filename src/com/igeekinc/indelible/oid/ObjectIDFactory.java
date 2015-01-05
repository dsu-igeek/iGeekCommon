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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.igeekinc.util.logging.ErrorLogMessage;

class ObjectIDMapping
{
	private Class<?> objectClass;
	private Class<? extends ObjectID> idClass;
	private ObjectIDConstructors constructors;
	
	public ObjectIDMapping(Class <?>objectClass, Class<? extends ObjectID>idClass)
	{
		this.objectClass = objectClass;
		constructors = new ObjectIDConstructors(idClass);
	}
	
	
	public boolean matches(Class<?> checkClass)
	{
		return objectClass.isAssignableFrom(checkClass);
	}
	
	public Class<?> getObjectClass()
	{
		return objectClass;
	}

	public Class<? extends ObjectID> getIdClass()
	{
		return idClass;
	}
	
	public ObjectID generateObjectID(GeneratorID generatorID)
	{
		return constructors.generateObjectID(generatorID);
	}
}

class ObjectIDConstructors
{
	private Class<? extends ObjectID> idClass;
	private Constructor<? extends ObjectID> idConstructor, stringConstructor, bytesConstructor;
	public ObjectIDConstructors(Class<? extends ObjectID>idClass)
	{
		this.idClass = idClass;
		try
		{
			idConstructor = idClass.getDeclaredConstructor(GeneratorID.class);
			stringConstructor = idClass.getDeclaredConstructor(String.class);
			bytesConstructor = idClass.getDeclaredConstructor(byte [].class, int.class);
		} catch (SecurityException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (NoSuchMethodException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		if (idConstructor == null)
			throw new IllegalArgumentException("Could not find constructor(GeneratorID) for "+idClass.getName());
	}

	
	public ObjectID generateObjectID(GeneratorID generatorID)
	{
		try
		{
			return idConstructor.newInstance(generatorID);
		} catch (IllegalArgumentException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (InstantiationException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (IllegalAccessException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (InvocationTargetException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		throw new InternalError("Could not create object id");
	}

	public ObjectID generateObjectID(String objectIDStr)
	{
		try
		{
			return stringConstructor.newInstance(objectIDStr);
		} catch (IllegalArgumentException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (InstantiationException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (IllegalAccessException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (InvocationTargetException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		throw new InternalError("Could not create object id");
	}
	
	public ObjectID generateObjectID(byte [] objectIDBytes, int offset)
	{
		try
		{
			return bytesConstructor.newInstance(objectIDBytes, offset);
		} catch (IllegalArgumentException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (InstantiationException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (IllegalAccessException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (InvocationTargetException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		throw new InternalError("Could not create object id");
	}

	public Class<? extends ObjectID> getIdClass()
	{
		return idClass;
	}
}

public class ObjectIDFactory
{
  GeneratorID generatorID;
  long        lastTime;
  int         uniquifier;

  private static ArrayList<ObjectIDMapping>mapping = new ArrayList<ObjectIDMapping>();
  private static HashMap<Byte, ObjectIDConstructors>constructorsByID = new HashMap<Byte, ObjectIDConstructors>();
  /**
   * Adds a new class mapping.  Should this be doing security checks??
   * @param objectClass
   * @param idClass
   */
  public static boolean addMapping(Class <?>objectClass, Class<? extends ObjectID>idClass)
  {
	  for (ObjectIDMapping checkMapping:mapping)
	  {
		  if (checkMapping.matches(objectClass))
			  return false;
	  }
	  mapping.add(new ObjectIDMapping(objectClass, idClass));
	  return true;
  }
  
  public static boolean addObjectIDClass(byte objectIDType, Class<? extends ObjectID>idClass)
  {
	  ObjectIDConstructors constructors = new ObjectIDConstructors(idClass);
	  if (!constructorsByID.containsKey(objectIDType))
	  {
		  constructorsByID.put(objectIDType, constructors);
		  return true;
	  }
	  return false;
  }
  
  public ObjectIDFactory(GeneratorID inGeneratorID)
  {
    generatorID = inGeneratorID;
    lastTime = 0;
    uniquifier = 0;
  }

  protected synchronized ObjectID setTimeAndUniquifier(ObjectID target)
  {
    long curTime;
    curTime = System.currentTimeMillis();
    if (curTime == lastTime)
    {
      uniquifier++;
    }
    else
    {
      uniquifier = 0;
    }
    target.setTimestamp(curTime);
    target.setUniquifier(uniquifier);
    lastTime = curTime;
    return target;
  }
  
  public ObjectID getNewOID(Class<?> objectClass)
  {
      ObjectID returnID = null;
      for (ObjectIDMapping checkMapping:mapping)
	  {
		  if (checkMapping.matches(objectClass))
		  {
			  returnID = checkMapping.generateObjectID(generatorID);
			  break;
		  }
	  }
      if (returnID == null)
          throw new InternalError("Unrecognized class for object id "+objectClass.getName()); //$NON-NLS-1$
      returnID = setTimeAndUniquifier(returnID);
      return returnID;
  }
  
  public ObjectID getNewOID(byte oidType)
  {
	  ObjectIDConstructors constructors = constructorsByID.get(oidType);
      if (constructors != null)
    	  return constructors.generateObjectID(generatorID);
      else
    	  return null;
  }
  
  public static ObjectID reconstituteFromString(String objectIDStr)
  {
      ObjectID.validateString(objectIDStr);
      try
      {
          byte type = ObjectID.getTypeFromString(objectIDStr);
          ObjectIDConstructors constructors = constructorsByID.get(type);
          if (constructors != null)
        	  return constructors.generateObjectID(objectIDStr);
          else
        	  return null;
          /*
          switch(type)
          {
          case ObjectID.kExecutionPlanOIDType:
              return new ExecutionPlanID(objectIDStr);
          case ObjectID.kOperationSetOIDType:
              return new OperationSetID(objectIDStr);
          case ObjectID.kMediaSetOIDType:
              return new MediaSetID(objectIDStr);
          case ObjectID.kIndelibleRuleSetOIDType:
              return new IndelibleRuleSetID(objectIDStr);
          case ObjectID.kCASCollectionOIDType:
              return new CASCollectionID(objectIDStr);
          case ObjectID.kCASSegmentOIDType:
              return new CASSegmentID(objectIDStr);
          case ObjectID.kFileOperationQueueOIDType:
              return new FileOperationQueueID(objectIDStr);
          case ObjectID.kMediaOIDType:
              return new MediaID(objectIDStr);
          case ObjectID.kQueueOIDType:
              return new QueueID(objectIDStr);
          case ObjectID.kInventoryOIDType:
              return new InventoryID(objectIDStr);
          case ObjectID.kExecutionPlanStatusOIDType:
              return new ExecutionPlanStatusID(objectIDStr);
          case ObjectID.kServerOIDType:
              return new EntityID(objectIDStr);
          case ObjectID.kDataMoverSessionOIDType:
              return new DataMoverSessionID(objectIDStr);
          case ObjectID.kNetworkDataDescriptorOIDType:
              return new NetworkDataDescriptorID(objectIDStr);
          case ObjectID.kIndelibleFSOIDType:
              return new IndelibleFSObjectID(objectIDStr);
          case ObjectID.kCASStoreOIDType:
        	  return new CASStoreID(objectIDStr);
          default:
              return null;
          }
          */
      }
      catch (NumberFormatException e)
      {
          throw new IllegalArgumentException("ID must be hexadecimal ascii"); //$NON-NLS-1$
      }
  }
  
  public static ObjectID reconstituteFromBytes(byte [] objectIDBytes)
  {
      return reconstituteFromBytes(objectIDBytes, 0, objectIDBytes.length);
  }
  public static ObjectID reconstituteFromBytes(byte [] objectIDBytes, int offset, int len)
  {
      if (len < ObjectID.kTotalBytes)
          throw new IllegalArgumentException(len+" is less than required "+ObjectID.kTotalBytes);
      byte type = objectIDBytes[offset + ObjectID.kTypeOffset];
      ObjectIDConstructors constructors = constructorsByID.get(type);
      if (constructors != null)
    	  return constructors.generateObjectID(objectIDBytes, offset);
      else
    	  return null;
      /*
      switch(type)
      {
      case ObjectID.kExecutionPlanOIDType:
          return new ExecutionPlanID(objectIDBytes, offset);
      case ObjectID.kOperationSetOIDType:
          return new OperationSetID(objectIDBytes, offset);
      case ObjectID.kMediaSetOIDType:
          return new MediaSetID(objectIDBytes, offset);
      case ObjectID.kIndelibleRuleSetOIDType:
          return new IndelibleRuleSetID(objectIDBytes, offset);
      case ObjectID.kCASCollectionOIDType:
          return new CASCollectionID(objectIDBytes, offset);
      case ObjectID.kCASSegmentOIDType:
          return new CASSegmentID(objectIDBytes, offset);
      case ObjectID.kFileOperationQueueOIDType:
          return new FileOperationQueueID(objectIDBytes, offset);
      case ObjectID.kMediaOIDType:
          return new MediaID(objectIDBytes, offset);
      case ObjectID.kQueueOIDType:
          return new QueueID(objectIDBytes, offset);
      case ObjectID.kInventoryOIDType:
          return new InventoryID(objectIDBytes, offset);
      case ObjectID.kExecutionPlanStatusOIDType:
          return new ExecutionPlanStatusID(objectIDBytes, offset);
      case ObjectID.kServerOIDType:
          return new EntityID(objectIDBytes, offset);
      case ObjectID.kDataMoverSessionOIDType:
          return new DataMoverSessionID(objectIDBytes, offset);
      case ObjectID.kNetworkDataDescriptorOIDType:
          return new NetworkDataDescriptorID(objectIDBytes, offset);
      case ObjectID.kIndelibleFSOIDType:
          return new IndelibleFSObjectID(objectIDBytes, offset);
      case ObjectID.kCASStoreOIDType:
    	  return new CASStoreID(objectIDBytes, offset);
      default:
          return null;
      }
      */
  }
}