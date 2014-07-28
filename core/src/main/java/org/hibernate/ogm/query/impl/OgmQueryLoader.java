/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.query.impl;

import java.io.Serializable;
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
import org.hibernate.hql.internal.ast.QueryTranslatorImpl;
import org.hibernate.hql.internal.ast.tree.SelectClause;
import org.hibernate.loader.hql.QueryLoader;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.loader.OgmLoader;
import org.hibernate.ogm.loader.OgmLoadingContext;
import org.hibernate.ogm.persister.OgmEntityPersister;
import org.hibernate.ogm.query.spi.BackendQuery;
import org.hibernate.ogm.type.GridType;
import org.hibernate.ogm.type.TypeTranslator;
import org.hibernate.ogm.util.ClosableIterator;
import org.hibernate.service.Service;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.Type;

/**
 * A {@link QueryLoader} which loads the results of JP-QL queries translated into store-specific native queries or
 * Lucene queries.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 * @author Gunnar Morling
 */
public class OgmQueryLoader extends QueryLoader {

	private final BackendQuery query;
	private final boolean hasScalars;
	private final List<String> scalarColumns;
	private final Type[] queryReturnTypes;
	private final TypeTranslator typeTranslator;

	public OgmQueryLoader(QueryTranslatorImpl queryTranslator, SessionFactoryImplementor factory, SelectClause selectClause, BackendQuery query, List<String> scalarColumns) {
		super( queryTranslator, factory, selectClause );
		this.query = query;
		this.hasScalars = selectClause.isScalarSelect();
		this.scalarColumns = scalarColumns;
		this.queryReturnTypes = selectClause.getQueryReturnTypes();
		this.typeTranslator = factory.getServiceRegistry().getService( TypeTranslator.class );
	}

	@Override
	protected List<?> list(SessionImplementor session, QueryParameters queryParameters, Set<Serializable> querySpaces, Type[] resultTypes)
			throws HibernateException {

		ClosableIterator<Tuple> tuples = service( session, GridDialect.class ).executeBackendQuery( query, queryParameters );
		try {
			if ( hasScalars ) {
				return listOfArrays( session, tuples );
			}
			else {
				return listOfEntities( session, resultTypes, tuples );
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

	private List<Object> listOfArrays(SessionImplementor session, Iterator<Tuple> tuples) {
		List<Object> results = new ArrayList<Object>();
		while ( tuples.hasNext() ) {
			Tuple tuple = tuples.next();
			Object[] entry = new Object[queryReturnTypes.length];

			int i = 0;
			for ( Type type : queryReturnTypes ) {
				GridType gridType = typeTranslator.getType( type );
				entry[i] = gridType.nullSafeGet( tuple, scalarColumns.get( i ), session, null );
				i++;
			}

			if ( entry.length == 1 ) {
				results.add( entry[0] );
			}
			else {
				results.add( entry );
			}
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
