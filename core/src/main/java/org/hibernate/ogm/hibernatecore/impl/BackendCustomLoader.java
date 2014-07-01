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
import org.hibernate.loader.custom.ScalarReturn;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.loader.OgmLoader;
import org.hibernate.ogm.loader.OgmLoadingContext;
import org.hibernate.ogm.loader.nativeloader.BackendCustomQuery;
import org.hibernate.ogm.persister.OgmEntityPersister;
import org.hibernate.ogm.util.ClosableIterator;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.Service;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.Type;

/**
 * Extension point for a loader that executes native NoSQL queries.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class BackendCustomLoader extends CustomLoader {

	private static final Log log = LoggerFactory.make();
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
		ClosableIterator<Tuple> tuples = service( session, GridDialect.class ).executeBackendQuery( customQuery, queryParameters );
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

	// At the moment we only support the case where one entity type is returned
	private List<Object> listOfEntities(SessionImplementor session, Type[] resultTypes, ClosableIterator<Tuple> tuples) {
		List<Object> results = new ArrayList<Object>();
		Class<?> returnedClass = resultTypes[0].getReturnedClass();
		while ( tuples.hasNext() ) {
			Tuple tuple = tuples.next();
			OgmLoader loader = createLoader( session, returnedClass );
			results.add( entity( session, tuple, loader ) );
		}
		return results;
	}

	private List<Object> listOfArrays(Iterator<Tuple> tuples) {
		List<Object> results = new ArrayList<Object>();
		while ( tuples.hasNext() ) {
			Tuple tuple = tuples.next();
			Object[] entry = null;
			if ( !customQuery.getCustomQueryReturns().isEmpty() ) {
				entry = new Object[customQuery.getCustomQueryReturns().size()];
				int i = 0;
				for ( Return column : customQuery.getCustomQueryReturns() ) {
					entry[i++] = tuple.get( ( (ScalarReturn) column ).getColumnAlias() );
				}
			}
			else {
				// TODO OGM-564 As a temporary work-around, retrieving the names from the actual result in case there
				// are no query returns defined (no result mapping has been given for a native query). Actually we
				// should drive this based on the selected columns as otherwise the order might not be correct and/or
				// null values will not show up
				entry = new Object[tuple.getColumnNames().size()];
				int i = 0;
				for ( String column : tuple.getColumnNames() ) {
					entry[i++] = tuple.get( column );
				}
			}

			results.add( entry );
		}
		return results;
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
}
