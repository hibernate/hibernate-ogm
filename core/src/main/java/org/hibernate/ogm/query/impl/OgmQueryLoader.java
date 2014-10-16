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
import org.hibernate.ogm.dialect.query.spi.BackendQuery;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.query.spi.QueryableGridDialect;
import org.hibernate.ogm.loader.impl.OgmLoader;
import org.hibernate.ogm.loader.impl.OgmLoadingContext;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.type.spi.TypeTranslator;
import org.hibernate.type.Type;

/**
 * A {@link QueryLoader} which loads the results of JP-QL queries translated into store-specific native queries or
 * Lucene queries.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 * @author Gunnar Morling
 */
public class OgmQueryLoader extends QueryLoader {

	private final OgmQueryLoaderContext<?> loaderContext;
	private final boolean hasScalars;
	private final List<String> scalarColumns;
	private final Type[] queryReturnTypes;
	private final TypeTranslator typeTranslator;

	public OgmQueryLoader(QueryTranslatorImpl queryTranslator, SessionFactoryImplementor factory, SelectClause selectClause, BackendQuery<?> query, List<String> scalarColumns) {
		super( queryTranslator, factory, selectClause );

		this.loaderContext = getLoaderContext( query, factory );
		this.hasScalars = selectClause.isScalarSelect();
		this.scalarColumns = scalarColumns;
		this.queryReturnTypes = selectClause.getQueryReturnTypes();
		this.typeTranslator = factory.getServiceRegistry().getService( TypeTranslator.class );
	}

	@SuppressWarnings("unchecked")
	private static <T extends Serializable> OgmQueryLoaderContext<T> getLoaderContext(BackendQuery<?> query, SessionFactoryImplementor factory) {
		QueryableGridDialect<T> gridDialect = factory.getServiceRegistry().getService( QueryableGridDialect.class );
		return new OgmQueryLoaderContext<T>( gridDialect, (BackendQuery<T>) query );
	}

	@Override
	protected List<?> list(SessionImplementor session, QueryParameters queryParameters, Set<Serializable> querySpaces, Type[] resultTypes)
			throws HibernateException {

		ClosableIterator<Tuple> tuples = loaderContext.executeQuery( queryParameters );
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

	private <E> E entity(SessionImplementor session, Tuple tuple, OgmLoader loader) {
		OgmLoadingContext ogmLoadingContext = new OgmLoadingContext();
		ogmLoadingContext.setTuples( Arrays.asList( tuple ) );
		@SuppressWarnings("unchecked")
		List<E> entities = (List<E>) loader.loadEntities( session, LockOptions.NONE, ogmLoadingContext );
		return entities.get( 0 );
	}

	private OgmLoader createLoader(SessionImplementor session, Class<?> entityClass) {
		OgmEntityPersister persister = (OgmEntityPersister) ( session.getFactory() ).getEntityPersister( entityClass.getName() );
		OgmLoader loader = new OgmLoader( new OgmEntityPersister[] { persister } );
		return loader;
	}

	/**
	 * Extracted as separate class for the sole purpose of capturing the type parameter {@code T} without exposing it to
	 * the callers which don't actually need it.
	 *
	 * @author Gunnar Morling
	 */
	private static class OgmQueryLoaderContext<T extends Serializable> {
		private final QueryableGridDialect<T> gridDialect;
		private final BackendQuery<T> query;

		public OgmQueryLoaderContext(QueryableGridDialect<T> gridDialect, BackendQuery<T> query) {
			this.gridDialect = gridDialect;
			this.query = query;
		}

		public ClosableIterator<Tuple> executeQuery(QueryParameters queryParameters) {
			return gridDialect.executeBackendQuery( query, queryParameters );
		}
	}
}
