/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.type;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.descriptor.impl.GridTypeDescriptor;
import org.hibernate.ogm.type.descriptor.impl.GridValueBinder;
import org.hibernate.ogm.type.descriptor.impl.GridValueExtractor;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.UUIDTypeDescriptor;

import java.util.UUID;

/**
* @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
*/
public class ExplodingType extends AbstractGenericBasicType<UUID> {

	public static final ExplodingType INSTANCE = new ExplodingType();

	public ExplodingType() {
		super( ExplodingTypeDescriptor.INSTANCE, UUIDTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "uuid";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public int getColumnSpan(Mapping mapping) {
		return 1;
	}

	@Override
	public String toString(UUID value) throws HibernateException {
		throw new RuntimeException( "Exploding type" );
	}

	@Override
	public UUID fromStringValue(String string) throws HibernateException {
		throw new RuntimeException( "Exploding type" );
	}

	static class ExplodingTypeDescriptor implements GridTypeDescriptor {
		public static ExplodingTypeDescriptor INSTANCE = new ExplodingTypeDescriptor();

		@Override
		public <X> GridValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new GridValueBinder<X>() {
				@Override
				public void bind(Tuple resultset, X value, String[] names) {
					throw new RuntimeException( "Exploding type" );
				}
			};
		}

		@Override
		public <X> GridValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new GridValueExtractor<X>() {
				@Override
				public X extract(Tuple resultset, String name) {
					throw new RuntimeException( "Exploding type" );
				}
			};
		}
	}
}
