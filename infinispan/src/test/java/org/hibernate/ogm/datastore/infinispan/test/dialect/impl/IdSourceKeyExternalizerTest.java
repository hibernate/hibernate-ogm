/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.test.dialect.impl;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import org.hibernate.ogm.datastore.infinispan.dialect.impl.IdSourceKeyExternalizer;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link IdSourceKeyExternalizer}.
 *
 * @author Gunnar Morling
 */
public class IdSourceKeyExternalizerTest {

	private ExternalizerTestHelper<IdSourceKey, IdSourceKeyExternalizer> externalizerHelper;

	@Before
	public void setupMarshallerFactory() {
		externalizerHelper = ExternalizerTestHelper.getInstance( IdSourceKeyExternalizer.INSTANCE );
	}

	@Test
	public void shouldSerializeAndDeserializeRowKey() throws Exception {
		IdSourceKeyMetadata keyMetadata = IdSourceKeyMetadata.forTable( "Hibernate_Sequences", "sequence_name", "next_val" );

		// given
		IdSourceKey key = IdSourceKey.forTable( keyMetadata, "Foo_Sequence" );

		// when
		byte[] bytes = externalizerHelper.marshall( key );
		IdSourceKey unmarshalledKey = externalizerHelper.unmarshall( bytes );

		// then
		assertThat( unmarshalledKey.getTable() ).isEqualTo( key.getTable() );
		assertThat( unmarshalledKey.getColumnNames() ).isEqualTo( key.getColumnNames() );
		assertThat( unmarshalledKey.getColumnValues() ).isEqualTo( key.getColumnValues() );

		assertTrue( key.equals( unmarshalledKey ) );
		assertTrue( unmarshalledKey.equals( key ) );
		assertThat( unmarshalledKey.hashCode() ).isEqualTo( key.hashCode() );
	}
}
