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

/**
 * Maps {@link Boolean} to {@code Y} or {@code N} strings, as Cassandra doesn't store {@link Character}s.
 *
 * @author Davide D'Alto
 */
public class CassandraYesNoType extends AbstractGenericBasicType<Boolean> {

	public static final CassandraYesNoType INSTANCE = new CassandraYesNoType();

	public CassandraYesNoType() {
		super( new TranslatingGridTypeDescriptor( String.class ), org.hibernate.type.YesNoType.INSTANCE.getJavaTypeDescriptor() );
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String getName() {
		return "cassandra_yes_no";
	}
}
