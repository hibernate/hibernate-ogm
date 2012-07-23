/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.logging.mongodb.impl;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

import com.mongodb.MongoException;
import org.hibernate.HibernateException;
import org.hibernate.ogm.datastore.mongodb.Environment;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
@MessageLogger(projectCode = "OGM")
public interface Log extends org.hibernate.ogm.util.impl.Log {

	@LogMessage(level = INFO)
	@Message(id = 1201, value = "Connecting to MongoDB at %1$s:%2$d with a timeout set at %3$d millisecond(s)")
	void connectingToMongo(String host, int port, int timeout);

	@LogMessage(level = INFO)
	@Message(id = 1202, value = "Closing connection to MongoDB")
	void disconnectingFromMongo();

	@Message(id = 1203, value = "Unable to find or initialize a connection to the MongoDB server")
	HibernateException unableToInitializeMongoDB(@Cause RuntimeException e);

	@Message(id = 1204, value = "The value set for the configuration property '" + Environment.MONGODB_PORT + "' must be a number between 1 and 65535. Found '[%s]'.")
	HibernateException mongoPortIllegalValue(String value);

	@Message(id = 1205, value = "Could not resolve MongoDB hostname [%s]")
	HibernateException mongoOnUnknownHost(String hostname);

	@LogMessage(level = INFO)
	@Message(id = 1206, value = "Mongo database named [%s] is not defined. Creating it!")
	void creatingDatabase(String dbName);

	@LogMessage(level = INFO)
	@Message(id = 1207, value = "Connecting to Mongo database named [%s].")
	void connectingToMongoDatabase(String dbName);

	@Message(id = 1208, value = "The configuration property '" + Environment.MONGODB_DATABASE + "' was not set. Can't connect to MongoDB.")
	HibernateException mongoDbNameMissing();

	@Message(id = 1209, value = "The database named [%s] cannot be dropped")
	HibernateException unableToDropDatabase(@Cause MongoException e, String databaseName);

	@LogMessage(level = TRACE)
	@Message(id = 1210, value = "Removed [%d] associations")
	void removedAssociation(int nAffected);

	@LogMessage(level = INFO)
	@Message(id = 1211, value = "The configuration property '" + Environment.MONGODB_SAFE + "' is set to %b")
	void useSafe(boolean safe);

	@Message(id = 1212, value = "Unknown association storage strategy: [%s]. Supported values in enum %s" )
	HibernateException unknownAssociationStorageStrategy(String databaseName, Class<?> enumType);

	@Message(id = 1213, value = "MongoDB authentication failed with username [%s]" )
	HibernateException authenticationFailed(String username);

	@Message(id = 1214, value = "Unable to connect to MongoDB instance %1$s:%2$d" )
	HibernateException unableToConnectToDatastore(String host, int port, @Cause Exception e);

	@Message( id = 1215, value = "The value set for the configuration property" + Environment.MONGODB_TIMEOUT +" must be a number greater than 0. Found '[%s]'.")
	HibernateException mongoDBTimeOutIllegalValue(String value);
}
