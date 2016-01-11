/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.type.converter;

import java.util.Arrays;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.descriptor.impl.GridTypeDescriptor;
import org.hibernate.ogm.type.descriptor.impl.GridValueBinder;
import org.hibernate.ogm.type.descriptor.impl.GridValueExtractor;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayTypeDescriptor;

/**
 * Grid types which stores a byte array by tripling it.
 *
 * @author Gunnar Morling
 *
 */
public class TriplingByteArrayGridType extends AbstractGenericBasicType<byte[]> {

	public static TriplingByteArrayGridType INSTANCE = new TriplingByteArrayGridType();

	public TriplingByteArrayGridType() {
		super( TriplingByteArrayGridTypeDescriptor.INSTANCE, PrimitiveByteArrayTypeDescriptor.INSTANCE );
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String getName() {
		return "tripled_blob";
	}

	public static class TriplingByteArrayGridTypeDescriptor implements GridTypeDescriptor {

		static TriplingByteArrayGridTypeDescriptor INSTANCE = new TriplingByteArrayGridTypeDescriptor();

		@Override
		public <X> GridValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new GridValueBinder<X>() {

				@Override
				public void bind(Tuple resultset, X value, String[] names) {
					int length = ( (byte[]) value ).length;
					byte[] persisted = new byte[length * 3];

					System.arraycopy( value, 0, persisted, 0, length );
					System.arraycopy( value, 0, persisted, length, length );
					System.arraycopy( value, 0, persisted, 2 * length, length );

					resultset.put( names[0], persisted );
				}
			};
		}

		@Override
		public <X> GridValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new GridValueExtractor<X>() {

				@Override
				public X extract(Tuple resultset, String name) {
					byte[] value = (byte[]) resultset.get( name );
					return value != null ? (X) Arrays.copyOfRange( value, 0, value.length / 3 ) : null;
				}
			};
		}
	}
}
