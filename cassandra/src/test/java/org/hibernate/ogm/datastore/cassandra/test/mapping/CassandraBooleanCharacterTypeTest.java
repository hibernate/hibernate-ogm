/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra.test.mapping;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hibernate.ogm.datastore.cassandra.type.impl.CassandraTrueFalseType;
import org.hibernate.ogm.datastore.cassandra.type.impl.CassandraYesNoType;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.descriptor.impl.GridValueBinder;
import org.hibernate.ogm.type.descriptor.impl.GridValueExtractor;
import org.junit.Test;

/**
 * Tests that 'yes_no' and 'true_false' types are correctly mapped to single-character strings.
 *
 * @author Nicola Ferraro
 */
public class CassandraBooleanCharacterTypeTest {

	@Test
	public void testYesNoTypeMapping() {

		CassandraYesNoType type = new CassandraYesNoType();
		GridValueBinder<Boolean> binder = type.getGridTypeDescriptor().getBinder( type.getJavaTypeDescriptor() );
		GridValueExtractor<Boolean> extractor = type.getGridTypeDescriptor().getExtractor( type.getJavaTypeDescriptor() );

		Tuple resultSet = mock( Tuple.class );

		binder.bind( resultSet, true, new String[]{ "column" } );
		verify( resultSet ).put( "column", "Y" );

		binder.bind( resultSet, false, new String[]{ "column" } );
		verify( resultSet ).put( "column", "N" );

		when( resultSet.get( anyString() ) ).thenReturn( "Y" );
		assertTrue( extractor.extract( resultSet, "column" ) );

		when( resultSet.get( anyString() ) ).thenReturn( "N" );
		assertFalse( extractor.extract( resultSet, "column" ) );
	}

	@Test
	public void testTrueFalseTypeMapping() {

		CassandraTrueFalseType type = new CassandraTrueFalseType();
		GridValueBinder<Boolean> binder = type.getGridTypeDescriptor().getBinder( type.getJavaTypeDescriptor() );
		GridValueExtractor<Boolean> extractor = type.getGridTypeDescriptor().getExtractor( type.getJavaTypeDescriptor() );

		Tuple resultSet = mock( Tuple.class );

		binder.bind( resultSet, true, new String[]{ "column" } );
		verify( resultSet ).put( "column", "T" );

		binder.bind( resultSet, false, new String[]{ "column" } );
		verify( resultSet ).put( "column", "F" );

		when( resultSet.get( anyString() ) ).thenReturn( "T" );
		assertTrue( extractor.extract( resultSet, "column" ) );

		when( resultSet.get( anyString() ) ).thenReturn( "F" );
		assertFalse( extractor.extract( resultSet, "column" ) );
	}

}
