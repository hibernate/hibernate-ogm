/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.infinispan.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.ogm.datastore.infinispan.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispan.logging.impl.LoggerFactory;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.storedprocedure.ProcedureQueryParameters;
import org.hibernate.ogm.util.impl.CollectionHelper;
import org.hibernate.ogm.util.impl.ReflectionHelper;
import org.hibernate.ogm.util.impl.TupleExtractor;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * @author The Viet Nguyen &amp;ntviet18@gmail.com&amp;
 */
public class InfinispanEmbeddedStoredProceduresManager {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private static final String STORED_PROCEDURES_CACHE_NAME = "___stored_procedures";

	/**
	 * Returns the result of a stored procedure executed on the backend.
	 *
	 * @param embeddedCacheManager embedded cache manager
	 * @param storedProcedureName name of stored procedure
	 * @param queryParameters parameters passed for this query
	 * @param classLoaderService the class loader service
	 *
	 * @return a {@link ClosableIterator} with the result of the query
	 */
	public ClosableIterator<Tuple> callStoredProcedure(EmbeddedCacheManager embeddedCacheManager, String storedProcedureName, ProcedureQueryParameters queryParameters, ClassLoaderService classLoaderService ) {
		validate( queryParameters );
		Cache<String, String> cache = embeddedCacheManager.getCache( STORED_PROCEDURES_CACHE_NAME, true );
		String className = cache.getOrDefault( storedProcedureName, storedProcedureName );
		Callable<?> callable = instantiate( storedProcedureName, className, classLoaderService );
		setParams( storedProcedureName, queryParameters, callable );
		Object res = execute( storedProcedureName, embeddedCacheManager, callable );
		return extractResultSet( storedProcedureName, res );
	}

	private void validate(ProcedureQueryParameters queryParameters) {
		List<Object> positionalParameters = queryParameters.getPositionalParameters();
		if ( positionalParameters != null && positionalParameters.size() > 0 ) {
			throw log.dialectDoesNotSupportPositionalParametersForStoredProcedures( getClass() );
		}
	}

	private Object execute(String storedProcedureName, EmbeddedCacheManager embeddedCacheManager, Callable<?> callable) {
		AtomicReference<Object> ref = new AtomicReference<>();
		try {
			return embeddedCacheManager.executor()
					.submitConsumer( ecm -> execute( storedProcedureName, callable ), ( a, r, e ) -> {
						if ( e != null ) {
							if ( e instanceof HibernateException ) {
								throw (HibernateException) e;
							}
							throw log.cannotExecuteStoredProcedure( storedProcedureName, e );
						}
						ref.compareAndSet( null, r );
					} )
					.thenCompose( v -> CompletableFuture.supplyAsync( ref::get ) )
					.get();
		}
		catch ( Exception e ) {
			throw log.cannotExecuteStoredProcedure( storedProcedureName, e );
		}
	}

	private static Callable<?> instantiate(String storedProcedureName, String className, ClassLoaderService classLoaderService ) {
		try {
			Class<?> clazz = classLoaderService.classForName( className );
			return (Callable<?>) clazz.newInstance();
		}
		catch (ClassLoadingException e) {
			throw log.procedureWithResolvedNameDoesNotExist( className, e );
		}
		catch (Exception e) {
			throw log.cannotInstantiateStoredProcedure( storedProcedureName, className, e );
		}
	}

	private static void setParams( String storedProcedureName, ProcedureQueryParameters queryParameters, Callable<?> callable ) {
			Map<String, Object> params = queryParameters.getNamedParameters();
		for ( Map.Entry<String, Object> entry : params.entrySet() ) {
			try {
				ReflectionHelper.setField( callable, entry.getKey(), entry.getValue() );
			}
			catch (Exception e) {
				throw log.cannotSetStoredProcedureParameter( storedProcedureName, entry.getKey(), entry.getValue(), e );
			}
		}
	}

	private static Object execute(String storedProcedureName, Callable<?> callable) {
		try {
			return callable.call();
		}
		catch (Exception e) {
			throw log.cannotExecuteStoredProcedure( storedProcedureName, e );
		}
	}

	private ClosableIterator<Tuple> extractResultSet(String storedProcedureName, Object res) {
		try {
			return CollectionHelper.newClosableIterator( TupleExtractor.extractTuplesFromObject( res ) );
		}
		catch (Exception e) {
			throw log.cannotExtractStoredProcedureResultSet( storedProcedureName, res, e );
		}
	}
}
