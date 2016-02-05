package org.hibernate.ogm.datastore.ignite.loader.criteria;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.loader.custom.CustomLoader;
import org.hibernate.loader.custom.Return;
import org.hibernate.loader.custom.RootReturn;
import org.hibernate.loader.custom.ScalarReturn;
import org.hibernate.ogm.datastore.ignite.dialect.criteria.spi.CriteriaGridDialect;
import org.hibernate.ogm.datastore.ignite.loader.criteria.impl.CriteriaCustomQuery;
import org.hibernate.ogm.datastore.ignite.loader.impl.IgniteLoader;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.loader.impl.OgmLoader;
import org.hibernate.ogm.loader.impl.OgmLoadingContext;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.type.spi.TypeTranslator;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.Type;

/**
 * Extension point for a loader that executes criteria queries.
 * 
 * @author Dmitriy Kozlov
 *
 */
public class OgmCriteriaLoader extends CustomLoader {

	private final CriteriaCustomQuery criteriaQuery;
	private final TypeTranslator typeTranslator;
	private final OgmCriteriaLoaderContext loaderContext;
	private final SqlStatementLogger statementLogger;
	
	public OgmCriteriaLoader(CriteriaCustomQuery criteriaQuery, SessionFactoryImplementor factory) throws HibernateException {
		super(criteriaQuery, factory);
		this.criteriaQuery = criteriaQuery;
		this.typeTranslator = factory.getServiceRegistry().getService( TypeTranslator.class );
		this.loaderContext = getLoaderContext(criteriaQuery, factory);
		this.statementLogger = factory.getServiceRegistry().getService( JdbcServices.class ).getSqlStatementLogger();

	}
	
	private static OgmCriteriaLoaderContext getLoaderContext(CriteriaCustomQuery criteriaQuery, SessionFactoryImplementor factory) {
		CriteriaGridDialect gridDialect = factory.getServiceRegistry().getService( CriteriaGridDialect.class );
		if (gridDialect == null)
			throw new HibernateException("Criteria not supported for current dialect ");
		return new OgmCriteriaLoaderContext(gridDialect, criteriaQuery);
	}
	
	/**
	 * Whether this query is a selection of a complete entity or not. Queries mixing scalar values and entire entities
	 * in one result are not supported atm.
	 */
	protected static boolean isEntityQuery(List<Return> queryReturns) {
		for ( Return queryReturn : queryReturns ) {
			if ( queryReturn instanceof RootReturn ) {
				return true;
			}
		}

		return false;
	}
	
	@Override
	protected List<?> list(SessionImplementor session, QueryParameters queryParameters, Set<Serializable> querySpaces, Type[] resultTypes)
			throws HibernateException {
		
		statementLogger.logStatement(getSQLString());

		ClosableIterator<Tuple> tuples = loaderContext.executeQuery();
		try {
			if (isEntityQuery(criteriaQuery.getCustomQueryReturns()))
				return listOfEntities( session, resultTypes, tuples );
			else
				return getTransformedResult(listOfArrays(session, tuples), criteriaQuery.getCriteria().getResultTransformer());
		}
		finally {
			tuples.close();
		}
		
	}
	
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
	
	private <T> T entity(SessionImplementor session, Tuple tuple, OgmLoader loader) {
		OgmLoadingContext ogmLoadingContext = new OgmLoadingContext();
		ogmLoadingContext.setTuples( Arrays.asList( tuple ) );
		@SuppressWarnings("unchecked")
		List<T> entities = (List<T>) loader.loadEntitiesFromTuples( session, LockOptions.NONE, ogmLoadingContext );
		return entities.get( 0 );
	}

	private OgmLoader createLoader(SessionImplementor session, Class<?> entityClass) {
		OgmEntityPersister persister = (OgmEntityPersister) ( session.getFactory() ).getEntityPersister( entityClass.getName() );
		OgmLoader loader = new IgniteLoader( new OgmEntityPersister[] { persister }, -1 /* vk: FIXME: where is batchSize value??? */ );
		return loader;
	}
	
	private List getTransformedResult(List result, ResultTransformer resultTransformer){
		return resultTransformer.transformList(result);
	}
	
	private List<Object> listOfArrays(SessionImplementor session, Iterator<Tuple> tuples) {
		List<Object> results = new ArrayList<Object>();
		while ( tuples.hasNext() ) {
			Tuple tuple = tuples.next();
			Object[] entry = null;
			String[] aliases = null;
			if ( !criteriaQuery.getCustomQueryReturns().isEmpty() ) {
				entry = new Object[criteriaQuery.getCustomQueryReturns().size()];
				aliases = new String[criteriaQuery.getCustomQueryReturns().size()];
				int i = 0;
				for ( Return queryReturn : criteriaQuery.getCustomQueryReturns() ) {
					ScalarReturn scalarReturn = (ScalarReturn) queryReturn;
					Type type = scalarReturn.getType();

					aliases[i] = scalarReturn.getColumnAlias();
					if ( type != null ) {
						GridType gridType = typeTranslator.getType( type );
						entry[i++] = gridType.nullSafeGet( tuple, scalarReturn.getColumnAlias(), session, null );
					}
					else {
						entry[i++] = tuple.get( scalarReturn.getColumnAlias() );
					}
				}
			}
			else {
				// TODO OGM-564 As a temporary work-around, retrieving the names from the actual result in case there
				// are no query returns defined (no result mapping has been given for a native query). Actually we
				// should drive this based on the selected columns as otherwise the order might not be correct and/or
				// null values will not show up
				entry = new Object[tuple.getColumnNames().size()];
				aliases = new String[tuple.getColumnNames().size()];
				int i = 0;
				for ( String column : tuple.getColumnNames() ) {
					aliases[i] = column;
					entry[i++] = tuple.get( column );
				}
			}

//			if ( entry.length == 1 ) {
//				results.add( entry[0] );
//			}
//			else {
			ResultTransformer resultTransformer = criteriaQuery.getCriteria().getResultTransformer();
			if (resultTransformer != null)
				results.add( resultTransformer.transformTuple(entry, aliases));
			else
				results.add(entry);
//			}
		}

		return results;
	}
	
	/**
	 * Extracted as separate class for the sole purpose of capturing the type parameter {@code T} without exposing it to
	 * the callers which don't actually need it.
	 *
	 * @author Gunnar Morling
	 */
	private static class OgmCriteriaLoaderContext {
		private final CriteriaGridDialect gridDialect;
		private final CriteriaCustomQuery criteriaQuery;
		private final EntityKeyMetadata keyMetadata;

		public OgmCriteriaLoaderContext(CriteriaGridDialect gridDialect, CriteriaCustomQuery criteriaQuery) {
			this.gridDialect = gridDialect;
			this.criteriaQuery = criteriaQuery;
			this.keyMetadata = criteriaQuery.getSingleEntityKeyMetadata();
		}

		public ClosableIterator<Tuple> executeQuery() {
			if (isEntityQuery(criteriaQuery.getCustomQueryReturns()))
				return gridDialect.executeCriteriaQuery( criteriaQuery, keyMetadata );
			else
				return gridDialect.executeCriteriaQueryWithProjection(criteriaQuery, keyMetadata);
		}
		
	}

}
