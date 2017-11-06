/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.index.impl;

import org.hibernate.ogm.options.shared.IndexOption;
import org.hibernate.ogm.util.impl.StringHelper;

/**
 * Supported MongoDB index types. Defined as a "_type" entry in {@link IndexOption}.
 *
 * @author Guillaume Smet
 */
public enum MongoDBIndexType {

	NORMAL(null),

	/**
	 * Full text index.
	 */
	TEXT("text"),

	/**
	 * Geospatial index on a 2D sphere.
	 */
	TWODSPHERE("2dsphere"),

	/**
	 * Geospatial index on a 2D plan.
	 */
	TWOD("2d");

	private String type;

	private MongoDBIndexType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public static MongoDBIndexType from(String type) {
		if ( StringHelper.isEmpty( type ) ) {
			return NORMAL;
		}
		if ( TEXT.type.equals( type ) ) {
			return TEXT;
		}
		else if ( TWODSPHERE.type.equals( type ) ) {
			return TWODSPHERE;
		}
		else if ( TWOD.type.equals( type ) ) {
			return TWOD;
		}
		throw new IllegalArgumentException( "Unsupported MongoDB index type " + type );
	}
}
