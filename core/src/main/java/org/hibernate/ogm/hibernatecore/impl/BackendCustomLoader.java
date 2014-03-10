/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.hibernatecore.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.loader.custom.CustomLoader;
import org.hibernate.loader.custom.Return;
import org.hibernate.loader.custom.RootReturn;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.TupleIterator;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.loader.OgmLoader;
import org.hibernate.ogm.loader.OgmLoadingContext;
import org.hibernate.ogm.loader.nativeloader.BackendCustomQuery;
import org.hibernate.ogm.persister.OgmEntityPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.service.Service;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.Type;

/**
 * Extension point for a loader that executes native NoSQL queries.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class BackendCustomLoader extends CustomLoader {

	private final BackendCustomQuery customQuery;

	public BackendCustomLoader(BackendCustomQuery customQuery, SessionFactoryImplementor factory) {
		super( customQuery, factory );
		this.customQuery = customQuery;
	}

	/**
	 * Whether this query is a selection of a complete entity or not. Queries mixing scalar values and entire entities
	 * in one result are not supported atm.
	 */
	private boolean isEntityQuery() {
		for ( Return queryReturn : customQuery.getCustomQueryReturns() ) {
			if ( queryReturn instanceof RootReturn ) {
				return true;
			}
		}

		return false;
	}

	@Override
	protected List<?> list(SessionImplementor session, QueryParameters queryParameters, Set querySpaces, Type[] resultTypes) throws HibernateException {
		TupleIterator tuples = executeQuery( session, service( session, GridDialect.class ), queryParameters, resultTypes );
		try {
			if ( isEntityQuery() ) {
				return listOfEntities( session, resultTypes, tuples );
			}
			else {
				return listOfArrays( tuples );
			}
		}
		finally {
			tuples.close();
		}
	}

	private List<Object> listOfEntities(SessionImplementor session, Type[] resultTypes, TupleIterator tuples) {
		List<Object> results = new ArrayList<Object>();
		while ( tuples.hasNext() ) {
			Tuple tuple = tuples.next();
			for ( Type type : resultTypes ) {
				OgmLoader loader = createLoader( session, type.getReturnedClass() );
				results.add( entity( session, tuple, loader ) );
			}
		}
		return results;
	}

	private List<Object> listOfArrays(Iterator<Tuple> tuples) {
		List<Object> results = new ArrayList<Object>();
		while ( tuples.hasNext() ) {
			Tuple tuple = tuples.next();
			Object[] entry = new Object[tuple.getColumnNames().size()];
			int i = 0;
			for ( String column : tuple.getColumnNames() ) {
				entry[i++] = tuple.get( column );
			}
			results.add( entry );
		}
		return results;
	}

	private TupleIterator executeQuery(SessionImplementor session, GridDialect dialect, QueryParameters queryParameters , Type[] resultTypes) {
		Loadable[] entityPersisters = getEntityPersisters();
		EntityKeyMetadata[] metadatas = new EntityKeyMetadata[entityPersisters.length];
		for ( int i = 0; i < metadatas.length; i++ ) {
			metadatas[i] = metadata( session.getFactory(), resultTypes[i] );
		}
		return dialect.executeBackendQuery( customQuery, queryParameters, metadatas );
	}

	private <T extends Service> T service(SessionImplementor session, Class<T> serviceRole) {
		return serviceRegistry( session ).getService( serviceRole );
	}

	private ServiceRegistryImplementor serviceRegistry(SessionImplementor session) {
		return session.getFactory().getServiceRegistry();
	}

	private <T> T entity(SessionImplementor session, Tuple tuple, OgmLoader loader) {
		OgmLoadingContext ogmLoadingContext = new OgmLoadingContext();
		ogmLoadingContext.setTuples( Arrays.asList( tuple ) );
		@SuppressWarnings("unchecked")
		List<T> entities = (List<T>) loader.loadEntities( session, LockOptions.NONE, ogmLoadingContext );
		return entities.get( 0 );
	}

	private OgmLoader createLoader(SessionImplementor session, Class<?> entityClass) {
		OgmEntityPersister persister = (OgmEntityPersister) ( session.getFactory() ).getEntityPersister( entityClass.getName() );
		OgmLoader loader = new OgmLoader( new OgmEntityPersister[] { persister } );
		return loader;
	}

	private EntityKeyMetadata metadata(SessionFactoryImplementor sessionFactory, Type resultType) {
		OgmEntityPersister persister = (OgmEntityPersister) ( sessionFactory ).getEntityPersister( resultType.getName() );
		return new EntityKeyMetadata( persister.getTableName(), persister.getRootTableIdentifierColumnNames() );
	}
}
