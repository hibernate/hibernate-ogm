package org.hibernate.ogm.test.loader;

import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.datastore.impl.MapTupleSnapshot;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.loader.OgmLoader;
import org.hibernate.ogm.loader.OgmLoadingContext;
import org.hibernate.ogm.persister.OgmEntityPersister;
import org.hibernate.ogm.test.simpleentity.OgmTestCase;
import org.hibernate.persister.entity.EntityPersister;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.test.utils.TestHelper.extractEntityTuple;

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

		EntityKey key = new EntityKey( "Feeling", new String[]{ "UUID" }, new Object[]{ feeling.getUUID() } );
		Map<String, Object> entityTuple = (Map<String, Object>) extractEntityTuple( sessions, key );
		final Tuple tuple = new Tuple( new MapTupleSnapshot( entityTuple ) );

		EntityPersister persister = ( (SessionFactoryImplementor) session.getSessionFactory() ).getEntityPersister( Feeling.class.getName() );
		OgmLoader loader = new OgmLoader( new OgmEntityPersister[] { (OgmEntityPersister) persister } );
		OgmLoadingContext ogmLoadingContext = new OgmLoadingContext();
		List<Tuple> tuples = new ArrayList<Tuple>();
		tuples.add( tuple );
		ogmLoadingContext.setTuples(tuples);
		List<Object> entities = loader.loadEntities( (SessionImplementor) session, LockOptions.NONE, ogmLoadingContext );
		assertThat(entities.size()).isEqualTo( 1 );
		assertThat( ( (Feeling) entities.get(0) ).getName() ).isEqualTo( "Moody" );

		session.close();
	}
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Feeling.class
		};
	}
}
