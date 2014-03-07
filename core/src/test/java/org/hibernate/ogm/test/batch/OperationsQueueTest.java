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
package org.hibernate.ogm.test.batch;

import java.util.Arrays;
import java.util.Collections;

import org.fest.assertions.Assertions;
import org.hibernate.HibernateException;
import org.hibernate.ogm.datastore.spi.SessionContext;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.datastore.spi.TupleTypeContext;
import org.hibernate.ogm.dialect.batch.OperationsQueue;
import org.hibernate.ogm.dialect.batch.RemoveTupleOperation;
import org.hibernate.ogm.dialect.batch.UpdateTupleOperation;
import org.hibernate.ogm.grid.AssociationKeyMetadata;
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
 * @author Davide D'Alto <davide@hibernate.org>
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
				new TupleTypeContext(
						Collections.<String>emptyList(),
						OptionsContextImpl.forEntity( Arrays.<OptionValueSource>asList( new AnnotationOptionValueSource() ), Object.class ),
						Collections.<AssociationKeyMetadata>emptyList()
				),
				new SessionContext()
		);
	}
}
