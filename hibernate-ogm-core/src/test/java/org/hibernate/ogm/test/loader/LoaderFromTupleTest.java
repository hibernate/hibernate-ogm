/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.test.loader;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.test.utils.TestHelper.extractEntityTuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.datastore.impl.MapTupleSnapshot;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.loader.OgmLoader;
import org.hibernate.ogm.loader.OgmLoadingContext;
import org.hibernate.ogm.persister.OgmEntityPersister;
import org.hibernate.ogm.test.simpleentity.OgmTestCase;
import org.hibernate.persister.entity.EntityPersister;
import org.junit.Test;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class LoaderFromTupleTest extends OgmTestCase {
	@Test
	public void testLoadingFromTuple() throws Exception {
		final Session session = openSession();

		Transaction transaction = session.beginTransaction();
		Feeling feeling = new Feeling();
		feeling.setName( "Moody" );
		session.persist( feeling );
		transaction.commit();

		session.clear();

		EntityKey key = new EntityKey( new EntityKeyMetadata( "Feeling", new String[] { "UUID" } ), new Object[] { feeling.getUUID() } );
		Map<String, Object> entityTuple = extractEntityTuple( sessions, key );
		final Tuple tuple = new Tuple( new MapTupleSnapshot( entityTuple ) );

		EntityPersister persister = ( (SessionFactoryImplementor) session.getSessionFactory() )
				.getEntityPersister( Feeling.class.getName() );
		OgmLoader loader = new OgmLoader( new OgmEntityPersister[] { (OgmEntityPersister) persister } );
		OgmLoadingContext ogmLoadingContext = new OgmLoadingContext();
		List<Tuple> tuples = new ArrayList<Tuple>();
		tuples.add( tuple );
		ogmLoadingContext.setTuples( tuples );
		List<Object> entities = loader.loadEntities( (SessionImplementor) session, LockOptions.NONE, ogmLoadingContext );
		assertThat( entities.size() ).isEqualTo( 1 );
		assertThat( ( (Feeling) entities.get( 0 ) ).getName() ).isEqualTo( "Moody" );

		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Feeling.class };
	}
}
