/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013-2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.mongodb.options;

import com.mongodb.WriteConcern;

/**
 * Write concern options for MongoDB. Represents the non-deprecated constants from {@link WriteConcern}.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public enum WriteConcernType {

	/**
	 * No exceptions are raised, even for network issues.
	 */
	ERRORS_IGNORED(WriteConcern.ERRORS_IGNORED),

	/**
	 * Write operations that use this write concern will wait for acknowledgement from the primary server before
	 * returning. Exceptions are raised for network issues, and server errors.
	 */
	ACKNOWLEDGED(WriteConcern.ACKNOWLEDGED),
	/**
	 * Write operations that use this write concern will return as soon as the message is written to the socket.
	 * Exceptions are raised for network issues, but not server errors.
	 */
	UNACKNOWLEDGED(WriteConcern.UNACKNOWLEDGED),

	/**
	 * Exceptions are raised for network issues, and server errors; the write operation waits for the server to flush
	 * the data to disk.
	 */
	FSYNCED(WriteConcern.FSYNCED),

	/**
	 * Exceptions are raised for network issues, and server errors; the write operation waits for the server to group
	 * commit to the journal file on disk.
	 */
	JOURNALED(WriteConcern.JOURNALED),

	/**
	 * Exceptions are raised for network issues, and server errors; waits for at least 2 servers for the write
	 * operation.
	 */
	REPLICA_ACKNOWLEDGED(WriteConcern.REPLICA_ACKNOWLEDGED),

	/**
	 * Exceptions are raised for network issues, and server errors; waits on a majority of servers for the write
	 * operation.
	 */
	MAJORITY(WriteConcern.MAJORITY);

	private WriteConcern writeConcern;

	private WriteConcernType(WriteConcern writeConcern) {
		this.writeConcern = writeConcern;
	}

	/**
	 * Returns the {@link WriteConcern} associated with this enum value.
	 */
	public WriteConcern getWriteConcern() {
		return writeConcern;
	}
}
