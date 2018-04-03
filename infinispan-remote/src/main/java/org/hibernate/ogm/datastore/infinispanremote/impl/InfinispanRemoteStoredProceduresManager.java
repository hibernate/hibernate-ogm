/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.ogm.datastore.infinispanremote.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.LoggerFactory;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.storedprocedure.ProcedureQueryParameters;
import org.hibernate.ogm.util.impl.CollectionHelper;
import org.hibernate.ogm.util.impl.TupleExtractor;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;

/**
 * @author The Viet Nguyen &amp;ntviet18@gmail.com&amp;
 */
public class InfinispanRemoteStoredProceduresManager {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private static final Pattern REFERENCE_ERROR_REGEXP = Pattern.compile( ".*\"([a-zA-Z_$]+)\" is not defined in <eval>.*" );

	/**
	 * Returns the result of a stored procedure executed on the backend.
	 *
	 * @param provider data store provider
	 * @param storedProcedureName name of stored procedure
	 * @param queryParameters parameters passed for this query
	 *
	 * @return a {@link ClosableIterator} with the result of the query
	 */
	public ClosableIterator<Tuple> callStoredProcedure( InfinispanRemoteDatastoreProvider provider, String storedProcedureName, ProcedureQueryParameters queryParameters ) {
		validate( queryParameters );
		RemoteCache<Object, Object> executor = provider.getScriptExecutorCache();
		Object res = execute( executor, storedProcedureName, queryParameters );
		return extractResultSet( storedProcedureName, res );
	}

	private void validate(ProcedureQueryParameters queryParameters) {
		List<Object> positionalParameters = queryParameters.getPositionalParameters();
		if ( positionalParameters != null && positionalParameters.size() > 0 ) {
			throw log.dialectDoesNotSupportPositionalParametersForStoredProcedures( getClass() );
		}
	}

	private Object execute( RemoteCache<Object, Object> executor, String storedProcedureName, ProcedureQueryParameters queryParameters ) {
		Map<String, Object> namedParameters = queryParameters.getNamedParameters();
		try {
			return executor.execute( storedProcedureName, namedParameters );
		}
		catch (Exception e) {
			String msg = e.getMessage();
			if ( e instanceof HotRodClientException && msg != null ) {
				// parse undefined task exception, e.g. ISPN027002: Unknown task 'storedProcedureName'"
				if ( msg.contains( "ISPN027002" ) ) {
					throw log.procedureWithResolvedNameDoesNotExist( storedProcedureName, e );
				}
				Matcher matcher = REFERENCE_ERROR_REGEXP.matcher( msg );
				if ( matcher.find() ) {
					String param = matcher.group( 1 );
					if ( param != null && !param.isEmpty() ) {
						throw log.cannotSetStoredProcedureParameter( storedProcedureName, param, namedParameters.get( param ), e );
					}
				}
			}
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
