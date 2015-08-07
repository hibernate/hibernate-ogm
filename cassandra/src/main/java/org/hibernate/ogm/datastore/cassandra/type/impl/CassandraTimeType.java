/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra.type.impl;

import java.util.Date;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;
import org.hibernate.type.descriptor.java.DateTypeDescriptor;

/**
 * Starting with 2.2 cassandra has a native 'time' (with no days) cql column type. yay.
 * The driver won't convert convert to/from java.util.Date though, so we need to wire up a custom TypeDescriptor.
 *
 * @author Jonathan Halliday
 */
public class CassandraTimeType extends AbstractGenericBasicType<Date> {

	public static CassandraTimeType INSTANCE = new CassandraTimeType();

	public CassandraTimeType() {
		super( TimeGridTypeDescriptor.INSTANCE, DateTypeDescriptor.INSTANCE );
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String getName() {
		return "cassandra_time";
	}
}
