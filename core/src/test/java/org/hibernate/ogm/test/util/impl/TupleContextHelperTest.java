/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.util.impl;

import static org.fest.assertions.Assertions.assertThat;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.EntityMetadataInformation;
import org.hibernate.ogm.util.impl.TupleContextHelper;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test the {@link TupleContextHelper} utility class.
 * <p>
 * An {@link EntityMetadataInformation} will be null when there is more than one entity type involved.
 * The session might be {@code null} when starting the mass-indexer.
 *
 * @author Davide D'Alto
 */
public class TupleContextHelperTest extends OgmTestCase {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testTupleContextIsNotNull() throws Exception {
		EntityKeyMetadata keyMetadata = new DefaultEntityKeyMetadata( DontCare.class.getSimpleName(), new String[] { "id" } );
		EntityMetadataInformation metadata = new EntityMetadataInformation( keyMetadata, DontCare.class.getName() );
		TupleContext tupleContext = TupleContextHelper.tupleContext( (SessionImplementor) openSession(), metadata );
		assertThat( tupleContext ).isNotNull();
		assertThat( tupleContext.getTransactionContext() ).isNotNull();
	}

	@Test
	public void shouldThrowTupleContextNotAvailableException() throws Exception {
		thrown.expect( HibernateException.class );
		thrown.expectMessage( "OGM000087" );

		TupleContext tupleContext = TupleContextHelper.tupleContext( (SessionImplementor) openSession(), null );
		tupleContext.getTupleTypeContext().getAllRoles();
	}

	@Test
	public void testTransactionContextIsNotNull() throws Exception {
		TupleContext tupleContext = TupleContextHelper.tupleContext( (SessionImplementor) openSession(), null );
		assertThat( tupleContext.getTransactionContext() ).isNotNull();
	}

	@Test
	public void testTupleContextIsNull() throws Exception {
		// This can happen when we start the mass-indexer without a session on more than one entity type.
		TupleContext tupleContext = TupleContextHelper.tupleContext( null, null );
		assertThat( tupleContext ).isNull();
	}

	@Test
	public void shouldThrowTxIdIsNotAvailableException() throws Exception {
		thrown.expect( HibernateException.class );
		thrown.expectMessage( "OGM000086" );

		TupleContext tupleContext = TupleContextHelper.tupleContext( (SessionImplementor) openSession(), null );
		tupleContext.getTransactionContext().getTransactionId();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ DontCare.class };
	}

	@Entity
	private static class DontCare {

		@Id
		public Long id;
	}
}
