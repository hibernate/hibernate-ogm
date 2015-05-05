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
import org.hibernate.type.descriptor.java.PrimitiveByteArrayTypeDescriptor;

/**
 * Handle byte[] via Cassandra's ByteBuffer support.
 *
 * @author Jonathan Halliday
 */
public class CassandraPrimitiveByteArrayType extends AbstractGenericBasicType<byte[]> {
	public static CassandraPrimitiveByteArrayType INSTANCE = new CassandraPrimitiveByteArrayType();

	public CassandraPrimitiveByteArrayType() {
		super( BufferingGridTypeDescriptor.INSTANCE, PrimitiveByteArrayTypeDescriptor.INSTANCE );
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String getName() {
		return "cassandra_materialized_blob";
	}
}
