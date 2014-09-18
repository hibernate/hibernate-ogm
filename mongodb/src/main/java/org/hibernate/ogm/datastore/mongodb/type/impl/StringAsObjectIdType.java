/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.type.impl;

import org.bson.types.ObjectId;
import org.hibernate.annotations.Type;
import org.hibernate.type.StringType;

/**
 * A Hibernate ORM type (i.e. no grid type) used for storing {@code String}s as {@link ObjectId} in MongoDB. It is
 * required for users to be able to specify that specific mapping via {@link Type}. Internally the mapping is
 * implemented by {@link StringAsObjectIdGridType}.
 *
 * @author Gunnar Morling
 */
public class StringAsObjectIdType extends StringType {

	public StringAsObjectIdType() {
	}

	@Override
	public String getName() {
		return "string_objectid";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[]{ "objectid" };
	}
}
