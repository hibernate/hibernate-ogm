/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.test.dialect.impl;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import org.hibernate.ogm.datastore.infinispan.dialect.impl.AssociationKeyExternalizer;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link AssociationKeyExternalizer}.
 *
 * @author Gunnar Morling
 */
public class AssociationKeyExternalizerTest {

	private ExternalizerTestHelper<AssociationKey, AssociationKeyExternalizer> externalizerHelper;

	@Before
	public void setupMarshallerFactory() {
		externalizerHelper = ExternalizerTestHelper.getInstance( AssociationKeyExternalizer.INSTANCE );
	}

	@Test
	public void shouldSerializeAndDeserializeAssociationKey() throws Exception {
		String[] columnNames = { "foo", "bar", "baz" };
		AssociationKeyMetadata keyMetadata = new AssociationKeyMetadata.Builder()
				.table( "Foobar" )
				.columnNames( columnNames )
				.build();

		Object[] values = { 123, "Hello", 456L };

		// given
		AssociationKey key = new AssociationKey( keyMetadata, values, null );

		// when
		byte[] bytes = externalizerHelper.marshall( key );
		AssociationKey unmarshalledKey = externalizerHelper.unmarshall( bytes );

		// then
		assertThat( unmarshalledKey.getTable() ).isEqualTo( key.getTable() );
		assertThat( unmarshalledKey.getColumnNames() ).isEqualTo( key.getColumnNames() );
		assertThat( unmarshalledKey.getColumnValues() ).isEqualTo( key.getColumnValues() );

		assertTrue( key.equals( unmarshalledKey ) );
		assertTrue( unmarshalledKey.equals( key ) );
		assertThat( unmarshalledKey.hashCode() ).isEqualTo( key.hashCode() );
	}
}
