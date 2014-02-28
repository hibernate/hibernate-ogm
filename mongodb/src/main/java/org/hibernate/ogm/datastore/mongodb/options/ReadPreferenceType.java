/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
