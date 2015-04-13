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
import org.hibernate.type.descriptor.java.ByteTypeDescriptor;

/**
 * Cassandra doesn't do Byte, so convert to Integer.
 *
 * @author Jonathan Halliday
 * @see TranslatingGridTypeDescriptor
 */
public class CassandraByteType extends AbstractGenericBasicType<Byte> {

	public static final CassandraByteType INSTANCE = new CassandraByteType();

	public CassandraByteType() {
		super( new TranslatingGridTypeDescriptor( Integer.class ), ByteTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "cassandra_byte";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}
}
