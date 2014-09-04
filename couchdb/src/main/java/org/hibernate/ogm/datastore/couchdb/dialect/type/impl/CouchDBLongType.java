/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.dialect.type.impl;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.StringMappedGridTypeDescriptor;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;
import org.hibernate.type.descriptor.java.LongTypeDescriptor;

/**
 * Type for storing {@code long}s in CouchDB. They are stored as strings to avoid precision issues with large numbers
 * (e.g. {@link Long#MAX_VALUE} can't be properly displayed as numeric type in CouchDB's Futon console).
 *
 * @author Andrea Boriero &lt;dreborier@gmail.com&gt;
 */
public class CouchDBLongType extends AbstractGenericBasicType<Long> {

	public static final CouchDBLongType INSTANCE = new CouchDBLongType();

	public CouchDBLongType() {
		super( StringMappedGridTypeDescriptor.INSTANCE, LongTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "couchdb_long";
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}
}
