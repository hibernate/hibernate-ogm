/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.test.dialect.impl;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.common.externalizer.impl.ExternalizerIds;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.kind.externalizer.impl.EntityKeyExternalizer;
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit test for {@link EntityKeyExternalizer}.
 *
 * @author Gunnar Morling
 */
public class EntityKeyExternalizerTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private ExternalizerTestHelper<EntityKey, EntityKeyExternalizer> externalizerHelper;

	@Before
	public void setupMarshallerFactory() {
		externalizerHelper = ExternalizerTestHelper.getInstance( EntityKeyExternalizer.INSTANCE );
	}

	@Test
	public void shouldSerializeAndDeserializeEntityKey() throws Exception {
		String[] columnNames = { "foo", "bar", "baz" };
		EntityKeyMetadata keyMetadata = new DefaultEntityKeyMetadata( "Foobar", columnNames );
		Object[] values = { 123, "Hello", 456L };

		// given
		EntityKey key = new EntityKey( keyMetadata, values );

		// when
		byte[] bytes = externalizerHelper.marshall( key );
		EntityKey unmarshalledKey = externalizerHelper.unmarshall( bytes );

		// then
		assertThat( unmarshalledKey.getTable() ).isEqualTo( key.getTable() );
		assertThat( unmarshalledKey.getColumnNames() ).isEqualTo( key.getColumnNames() );
		assertThat( unmarshalledKey.getColumnValues() ).isEqualTo( key.getColumnValues() );

		assertTrue( key.equals( unmarshalledKey ) );
		assertTrue( unmarshalledKey.equals( key ) );
		assertThat( unmarshalledKey.hashCode() ).isEqualTo( key.hashCode() );
	}

	@Test
	public void shouldRaiseErrorWhenUnmarshallingBytesWithUnknownVersion() throws Exception {
		ExternalizerTestHelper<EntityKey, FutureEntityKeyExternalizer> futureExternalizer = ExternalizerTestHelper.getInstance( new FutureEntityKeyExternalizer() );

		String[] columnNames = { "foo", "bar", "baz" };
		EntityKeyMetadata keyMetadata = new DefaultEntityKeyMetadata( "Foobar", columnNames );
		Object[] values = { 123, "Hello", 456L };

		EntityKey key = new EntityKey( keyMetadata, values );

		// given
		byte[] bytes = futureExternalizer.marshall( key );

		// then
		thrown.expect( HibernateException.class );
		thrown.expectMessage( "OGM001101" );

		// when
		externalizerHelper.unmarshall( bytes );
	}

	/**
	 * A faked future version of {@link EntityKeyExternalizer}.
	 */
	private static class FutureEntityKeyExternalizer implements AdvancedExternalizer<EntityKey> {

		@Override
		public void writeObject(ObjectOutput output, EntityKey object) throws IOException {
			// A future version
			output.writeInt( 5 );
		}

		@Override
		public EntityKey readObject(ObjectInput input) throws IOException, ClassNotFoundException {
			throw new UnsupportedOperationException( "should not be invoked" );
		}

		@Override
		public Set<Class<? extends EntityKey>> getTypeClasses() {
			return Collections.<Class<? extends EntityKey>>singleton( EntityKey.class );
		}

		@Override
		public Integer getId() {
			return ExternalizerIds.PER_KIND_ENTITY_KEY;
		}
	}
}
