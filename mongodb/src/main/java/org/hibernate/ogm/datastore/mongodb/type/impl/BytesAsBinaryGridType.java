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
public class BytesAsBinaryGridType extends AbstractGenericBasicType<byte[]> {

	public static final BytesAsBinaryGridType INSTANCE = new BytesAsBinaryGridType();

	public BytesAsBinaryGridType() {
		super( WrappedGridTypeDescriptor.INSTANCE, BytesAsBsonBinaryTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return BytesAsBinaryType.TYPE_NAME;
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}
}
