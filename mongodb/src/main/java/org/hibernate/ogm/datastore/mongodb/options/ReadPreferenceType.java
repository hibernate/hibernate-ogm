/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.options;

import com.mongodb.ReadPreference;

/**
 * Read preference options for MongoDB. Represents the defined strategies from {@link ReadPreference}.
 *
 * @author Gunnar Morling
 */
public enum ReadPreferenceType {

	/**
	 * Read from the primary node only.
	 */
	PRIMARY(ReadPreference.primary()),

	/**
	 * Read from the primary node if possible.
	 */
	PRIMARY_PREFERRED(ReadPreference.primaryPreferred()),

	/**
	 * Read from a secondary node only.
	 */
	SECONDARY(ReadPreference.secondary()),

	/**
	 * Read from a secondary node if possible, from primary otherwise.
	 */
	SECONDARY_PREFERRED(ReadPreference.secondaryPreferred()),

	/**
	 * Read from the nearest node.
	 */
	NEAREST(ReadPreference.nearest());

	private final ReadPreference readPreference;

	private ReadPreferenceType(ReadPreference readPreference) {
		this.readPreference = readPreference;
	}

	/**
	 * Returns the {@link ReadPreference} associated with this enum value.
	 */
	public ReadPreference getReadPreference() {
		return readPreference;
	}
}
