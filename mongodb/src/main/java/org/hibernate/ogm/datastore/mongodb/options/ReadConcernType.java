/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.options;

import com.mongodb.ReadConcern;

/**
 * Read preference options for MongoDB. Represents the defined strategies from {@link ReadConcern}.
 *
 * @author Aleksandr Mylnikov
 */
public enum ReadConcernType {

	/**
	 *  Default read concern. Means that there is no replica set
	 */
	DEFAULT(ReadConcern.DEFAULT),

	/**
	 * The query returns data from the instance with no guarantee that the data has been written to
	 * a majority of the replica set members (i.e. may be rolled back).
	 */
	LOCAL(ReadConcern.LOCAL),

	/**
	 * The query returns data that has been acknowledged by a majority of the replica set members.
	 * The documents returned by the read operation are durable, even in the event of failure.
	 */
	MAJORITY(ReadConcern.MAJORITY),

	/**
	 * The query returns data that reflects all successful majority-acknowledged writes that completed
	 * prior to the start of the read operation. The query may wait for concurrently executing writes
	 * to propagate to a majority of replica set members before returning results.
	 */
	LINEARIZABLE(ReadConcern.LINEARIZABLE);

	private final ReadConcern readConcern;

	ReadConcernType(ReadConcern readConcern) {
		this.readConcern = readConcern;
	}

	public ReadConcern getReadConcern() {
		return readConcern;
	}
}
