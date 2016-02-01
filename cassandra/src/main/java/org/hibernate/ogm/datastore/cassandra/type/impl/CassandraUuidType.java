/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra.type.impl;

import java.util.UUID;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.PassThroughGridTypeDescriptor;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;
import org.hibernate.type.descriptor.java.UUIDTypeDescriptor;

/**
 * Maps {@link UUID} to Cassandra's native {@code uuid} type.
 *
 * @author Gunnar Morling
 */
public class CassandraUuidType extends AbstractGenericBasicType<UUID> {

	public static final CassandraUuidType INSTANCE = new CassandraUuidType();

	public CassandraUuidType() {
		super( PassThroughGridTypeDescriptor.INSTANCE, UUIDTypeDescriptor.INSTANCE );
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String getName() {
		return "cassandra_uuid";
	}
}
