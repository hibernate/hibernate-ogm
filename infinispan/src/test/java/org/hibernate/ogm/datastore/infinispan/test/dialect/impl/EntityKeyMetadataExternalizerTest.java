/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.datastore.infinispan.test.dialect.impl;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import org.hibernate.ogm.datastore.infinispan.dialect.impl.EntityKeyMetadataExternalizer;
import org.hibernate.ogm.grid.EntityKeyMetadata;
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
