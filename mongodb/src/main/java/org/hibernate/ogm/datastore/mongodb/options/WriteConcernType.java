/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.options;

import com.mongodb.WriteConcern;

import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;

/**
 * Write concern options for MongoDB. Represents the non-deprecated constants from {@link WriteConcern}.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public enum WriteConcernType {

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
	MAJORITY(WriteConcern.MAJORITY),

	/**
	 * A custom {@link WriteConcern} implementation is specified.
	 */
	CUSTOM( null );

	private static Log log = LoggerFactory.getLogger();

	private final WriteConcern writeConcern;

	WriteConcernType(WriteConcern writeConcern) {
		this.writeConcern = writeConcern;
	}

	/**
	 * Get the write concern
	 *
	 * @return the {@link WriteConcern} associated with this enum value; {@code null} in the case of {@link #CUSTOM}.
	 */
	public WriteConcern getWriteConcern() {
		if ( this.name().equals( "ERRORS_IGNORED" ) ) {
			// Do a hard fail as we don't have a proper replacement
			// Looks like the nicest thing to do to the user
			throw log.writeConcernDeprecated( "ERRORS_IGNORED" );
		}
		return writeConcern;
	}
}
