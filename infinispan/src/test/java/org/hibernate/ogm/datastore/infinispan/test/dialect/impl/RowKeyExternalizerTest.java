/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.test.dialect.impl;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import org.hibernate.ogm.datastore.infinispan.dialect.impl.RowKeyExternalizer;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link RowKeyExternalizer}.
 *
 * @author Gunnar Morling
 */
public class RowKeyExternalizerTest {

	private ExternalizerTestHelper<RowKey, RowKeyExternalizer> externalizerHelper;

	@Before
	public void setupMarshallerFactory() {
		externalizerHelper = ExternalizerTestHelper.getInstance( RowKeyExternalizer.INSTANCE );
	}

	@Test
	public void shouldSerializeAndDeserializeRowKey() throws Exception {
		String[] columnNames = { "foo", "bar", "baz" };
		Object[] values = { 123, "Hello", 456L };

		// given
		RowKey key = new RowKey( columnNames, values );

		// when
		byte[] bytes = externalizerHelper.marshall( key );
		RowKey unmarshalledKey = externalizerHelper.unmarshall( bytes );

		// then
		assertThat( unmarshalledKey.getColumnNames() ).isEqualTo( key.getColumnNames() );
		assertThat( unmarshalledKey.getColumnValues() ).isEqualTo( key.getColumnValues() );

		assertTrue( key.equals( unmarshalledKey ) );
		assertTrue( unmarshalledKey.equals( key ) );
		assertThat( unmarshalledKey.hashCode() ).isEqualTo( key.hashCode() );
	}
}
