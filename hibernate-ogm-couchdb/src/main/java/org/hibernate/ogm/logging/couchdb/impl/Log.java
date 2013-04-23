/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.logging.couchdb.impl;

import org.hibernate.HibernateException;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;
import org.jboss.resteasy.client.ClientResponseFailure;
import org.jboss.resteasy.client.exception.ResteasyClientException;

import static org.jboss.logging.Logger.Level.INFO;

/**
 * @author Andrea Boriero <dreborier@gmail.com/>
 */
@MessageLogger(projectCode = "OGM")
public interface Log extends org.hibernate.ogm.util.impl.Log {

	@Message(id = 1301, value = "An error occurred increasing the value of the key")
	HibernateException errorCalculatingNextValue(@Cause ClientResponseFailure exception);

	@Message(id = 1302, value = "An error occurred creating CouchDB Document, http response status code %03d, error %s , cause  %s")
	HibernateException errorCreatingDocument(int status, String error, String cause);

	@Message(id = 1303, value = "An error occurred deleting CouchDB Document, http response status code %03d, error %s , cause  %s")
	HibernateException errorDeletingDocument(int status, String error, String reason);

	@Message(id = 1304, value = "Unable to connect to CouchDB")
	HibernateException couchDBConnectionProblem(@Cause ResteasyClientException exception);

	@Message(id = 1305, value = "An error occurred dropping the database, http response status code %03d")
	HibernateException errorDroppingDatabase(int status);

	@Message(id = 1306, value = "An error occurred retrieving entity with id %s, http response status code %03d")
	HibernateException errorRetrievingEntity(String entityId, int status);

	@Message(id = 1307, value = "An error occurred retrieving association with id %s, http response status code %03d")
	HibernateException errorRetrievingAssociation(String id, int status);

	@Message(id = 1308, value = "An error occurred retrieving the number of associations stored into CouchDb, http response status code %03d")
	HibernateException unableToRetrieveTheNumberOfAssociations(int status);

	@Message(id = 1309, value = "An error occurred retrieving the number of entities stored into CouchDb, http response status code %03d")
	HibernateException unableToRetrieveTheNumberOfEntities(int status);

	@Message(id = 1310, value = "An error occurred retrieving tuples for table %s, http response status code %03d")
	HibernateException unableToRetrieveTheTupleByEntityKeyMetadata(String tableName, int status);

	@Message(id = 1311, value = "An error occurred retrieving integral, http response status code %03d")
	HibernateException errorRetrievingIntegral(int status);

	@Message(id = 1312, value = "An error occurred retrieving the list of databases, http response status code %03d")
	HibernateException unableToRetrieveTheListOfDatabase(int status);

	@Message(id = 1313, value = "An error occurred retrieving database %s, http response status code %03d, error %s, reason %s")
	HibernateException errorCreatingDatabase(String dataBaseName, int status, String error, String reason);

	@LogMessage(level = INFO)
	@Message(id = 1314, value = "Connecting to CouchDB at %s")
	void connectingToCouchDB(String databaseUrl);
}
