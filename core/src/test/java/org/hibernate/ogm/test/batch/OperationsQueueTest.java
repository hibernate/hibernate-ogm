/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.batch;

import java.util.Arrays;
import java.util.Collections;

import org.fest.assertions.Assertions;
import org.hibernate.HibernateException;
import org.hibernate.ogm.datastore.spi.AssociatedEntitiesMetadata;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.dialect.batch.OperationsQueue;
import org.hibernate.ogm.dialect.batch.RemoveTupleOperation;
import org.hibernate.ogm.dialect.batch.UpdateTupleOperation;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.options.navigation.impl.OptionsContextImpl;
import org.hibernate.ogm.options.navigation.source.impl.AnnotationOptionValueSource;
import org.hibernate.ogm.options.navigation.source.impl.OptionValueSource;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the methods in the {@link OperationsQueue}
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class OperationsQueueTest {

	private OperationsQueue queue;

	@Before
	public void init() {
		queue = new OperationsQueue();
	}

	@Test(expected = HibernateException.class)
	public void testAddCauseExceptionWhenQueueIsClosed() throws Exception {
		queue.close();
		queue.add( new RemoveTupleOperation( null, getEmptyTupleContext() ) );
	}

	@Test(expected = HibernateException.class)
	public void testAddUpdateTupleCauseExceptionWhenQueueIsClosed() throws Exception {
		queue.close();
		queue.add( new UpdateTupleOperation( null, null, getEmptyTupleContext() ) );
	}

	@Test(expected = HibernateException.class)
	public void testPollCauseExceptionWhenQueueIsClosed() throws Exception {
		queue.close();
		queue.poll();
	}

	@Test
	public void testContainsKeyWhenAddingUpdateTupleOperation() throws Exception {
		EntityKey key = entityKey();
		UpdateTupleOperation expected = new UpdateTupleOperation( null, key, getEmptyTupleContext() );
		queue.add( expected );

		Assertions.assertThat( queue.contains( key ) ).isTrue();
	}

	@Test
	public void testContainsKeyIsFalseWhenAddingRemoveTupleOperation() throws Exception {
		EntityKey key = entityKey();
		RemoveTupleOperation expected = new RemoveTupleOperation( key, getEmptyTupleContext() );
		queue.add( expected );

		Assertions.assertThat( queue.contains( key ) ).isFalse();
	}

	@Test
	public void testAddRemoveTupleOperation() throws Exception {
		EntityKey key = entityKey();
		RemoveTupleOperation expected = new RemoveTupleOperation( key, getEmptyTupleContext() );
		queue.add( expected );

		Assertions.assertThat( expected ).isEqualTo( queue.poll() );
	}

	@Test
	public void testAddUpdateTupleOperation() throws Exception {
		EntityKey key = entityKey();
		UpdateTupleOperation expected = new UpdateTupleOperation( null, key, getEmptyTupleContext() );
		queue.add( expected );

		Assertions.assertThat( expected ).isEqualTo( queue.poll() );
	}

	@Test
	public void testEmptyQueueSize() throws Exception {
		Assertions.assertThat( 0 ).isEqualTo( queue.size() );
	}

	@Test
	public void testQueueSizeWhenAddingUpdateTupleOperation() throws Exception {
		queue.add( new UpdateTupleOperation( null, entityKey(), getEmptyTupleContext() ) );

		Assertions.assertThat( 1 ).isEqualTo( queue.size() );
	}

	@Test
	public void testQueueSizeWhenAddingRemoveTupleOperation() throws Exception {
		queue.add( new RemoveTupleOperation( entityKey(), getEmptyTupleContext() ) );

		Assertions.assertThat( 1 ).isEqualTo( queue.size() );
	}

	private EntityKey entityKey() {
		EntityKeyMetadata keyMetadata = new EntityKeyMetadata( "MetadataTable", new String[] {} );
		EntityKey key = new EntityKey( keyMetadata, new Object[] {} );
		return key;
	}

	private TupleContext getEmptyTupleContext() {
		return new TupleContext(
				Collections.<String>emptyList(),
				AssociatedEntitiesMetadata.EMPTY_INSTANCE,
				OptionsContextImpl.forEntity( Arrays.<OptionValueSource>asList( new AnnotationOptionValueSource() ), Object.class )
		);
	}
}
