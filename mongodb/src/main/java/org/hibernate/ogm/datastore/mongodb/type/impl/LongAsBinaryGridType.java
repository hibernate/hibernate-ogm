/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.type.impl;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.WrappedGridTypeDescriptor;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class LongAsBinaryGridType extends AbstractGenericBasicType<Long> {

	public static final LongAsBinaryGridType INSTANCE = new LongAsBinaryGridType();

	public LongAsBinaryGridType() {
		super( WrappedGridTypeDescriptor.INSTANCE, LongAsBinaryTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return LongAsBinaryType.TYPE_NAME;
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}
}
