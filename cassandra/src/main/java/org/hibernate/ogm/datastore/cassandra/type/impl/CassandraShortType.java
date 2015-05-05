/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra.type.impl;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;
import org.hibernate.type.descriptor.java.ShortTypeDescriptor;

/**
 * Cassandra doesn't do Short, so convert to Integer.
 *
 * @author Jonathan Halliday
 * @see TranslatingGridTypeDescriptor
 */
public class CassandraShortType extends AbstractGenericBasicType<Short> {
	public static CassandraShortType INSTANCE = new CassandraShortType();

	public CassandraShortType() {
		super( new TranslatingGridTypeDescriptor( Integer.class ), ShortTypeDescriptor.INSTANCE );
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] {getName(), short.class.getName(), Short.class.getName()};
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String getName() {
		return "cassandra_short";
	}

}
