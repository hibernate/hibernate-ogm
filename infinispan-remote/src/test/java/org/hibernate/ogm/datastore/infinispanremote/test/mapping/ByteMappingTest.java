/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.test.mapping;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.datastore.infinispanremote.utils.InfinispanRemoteTestHelper.fetchProtoStreamPayload;

import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.ProtostreamPayload;
import org.hibernate.ogm.datastore.infinispanremote.utils.InfinispanRemoteServerRunner;
import org.hibernate.ogm.utils.OgmTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the mappings of byte type into Infinispan.
 *
 * @author Fabio Massimo Ercoli
 */
@RunWith(InfinispanRemoteServerRunner.class)
public class ByteMappingTest extends OgmTestCase {

	private static final int ENTITY_ID = 7;
	private static final byte FIELD_VALUE = 3;

	@Before
	public void test() {
		ByteEntity entity = new ByteEntity();
		entity.setId( 7 );
		entity.setCounter( FIELD_VALUE );

		inTransaction( session -> session.persist( entity ) );
	}

	@Test
	public void testByteMapping() {
		ProtostreamPayload payload = fetchProtoStreamPayload( sessionFactory, "ByteEntity", "id", ENTITY_ID );
		assertThat( payload.getColumnValue( "counter" ) ).isEqualTo( FIELD_VALUE );

		// fetching Entity with Hibernate too
		inTransaction( session -> {
			ByteEntity byteEntity = session.get( ByteEntity.class, ENTITY_ID );
			assertThat( byteEntity.getCounter() ).isEqualTo( FIELD_VALUE );
		} );
	}

	@After
	public void after() {
		deleteAll( ByteEntity.class, ENTITY_ID );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ByteEntity.class };
	}
}
