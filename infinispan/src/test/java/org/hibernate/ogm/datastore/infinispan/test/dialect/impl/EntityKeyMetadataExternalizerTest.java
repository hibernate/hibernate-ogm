/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.test.dialect.impl;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import org.hibernate.ogm.datastore.infinispan.dialect.impl.EntityKeyMetadataExternalizer;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link EntityKeyMetadata}.
 *
 * @author Gunnar Morling
 */
public class EntityKeyMetadataExternalizerTest {

	private ExternalizerTestHelper<EntityKeyMetadata, EntityKeyMetadataExternalizer> externalizerHelper;

	@Before
	public void setupMarshallerFactory() {
		externalizerHelper = ExternalizerTestHelper.getInstance( EntityKeyMetadataExternalizer.INSTANCE );
	}

	@Test
	public void shouldSerializeAndDeserializeEntityKey() throws Exception {
		// given
		String[] columnNames = { "foo", "bar", "baz" };
		EntityKeyMetadata keyMetadata = new EntityKeyMetadata( "Foobar", columnNames );

		// when
		byte[] bytes = externalizerHelper.marshall( keyMetadata );
		EntityKeyMetadata unmarshalledMetadata = externalizerHelper.unmarshall( bytes );

		// then
		assertThat( unmarshalledMetadata.getClass() ).isEqualTo( EntityKeyMetadata.class );
		assertThat( unmarshalledMetadata.getTable() ).isEqualTo( keyMetadata.getTable() );
		assertThat( unmarshalledMetadata.getColumnNames() ).isEqualTo( keyMetadata.getColumnNames() );

		assertTrue( keyMetadata.equals( unmarshalledMetadata ) );
		assertTrue( unmarshalledMetadata.equals( keyMetadata ) );
		assertThat( unmarshalledMetadata.hashCode() ).isEqualTo( keyMetadata.hashCode() );
	}
}
