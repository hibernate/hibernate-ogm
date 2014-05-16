/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.PassThroughGridTypeDescriptor;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayTypeDescriptor;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class PrimitiveByteArrayType extends AbstractGenericBasicType<byte[]> {
	public static PrimitiveByteArrayType INSTANCE = new PrimitiveByteArrayType();

	public PrimitiveByteArrayType() {
		super( PassThroughGridTypeDescriptor.INSTANCE, PrimitiveByteArrayTypeDescriptor.INSTANCE );
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String getName() {
		return "materialized_blob";
	}
}
