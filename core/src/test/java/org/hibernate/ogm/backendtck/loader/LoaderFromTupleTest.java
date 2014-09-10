/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.loader;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.TestHelper.extractEntityTuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.datastore.map.impl.MapTupleSnapshot;
import org.hibernate.ogm.loader.impl.OgmLoader;
import org.hibernate.ogm.loader.impl.OgmLoadingContext;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.persister.entity.EntityPersister;
import org.junit.Test;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
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

		transaction = session.beginTransaction();
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
		transaction.commit();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Feeling.class };
	}
}
