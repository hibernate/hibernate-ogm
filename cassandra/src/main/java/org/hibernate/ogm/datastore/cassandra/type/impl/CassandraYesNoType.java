/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra.type.impl;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.StringMappedGridTypeDescriptor;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;

/**
 * Maps {@link Boolean} to {@code Y} or {@code N} strings.
 *
 * @author Davide D'Alto
 */
public class CassandraYesNoType extends AbstractGenericBasicType<Boolean> {

	public static final CassandraYesNoType INSTANCE = new CassandraYesNoType();

	public CassandraYesNoType() {
		super( StringMappedGridTypeDescriptor.INSTANCE, org.hibernate.type.YesNoType.INSTANCE.getJavaTypeDescriptor() );
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String getName() {
		return "yes_no";
	}
}
