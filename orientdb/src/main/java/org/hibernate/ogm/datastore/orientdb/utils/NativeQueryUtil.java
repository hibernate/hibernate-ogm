/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.utils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.ogm.datastore.orientdb.logging.impl.Log;
import org.hibernate.ogm.datastore.orientdb.logging.impl.LoggerFactory;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQLParsingException;
import com.orientechnologies.orient.core.sql.executor.OResultSet;

/**
 * Util class for execute queries by native OrientDB API
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 * @see <a href="http://orientdb.com/docs/master/Java-Query-API.html">Java-Query-API</a>
 * @since 1.8
 */
public class NativeQueryUtil {

	private static final Log log = LoggerFactory.getLogger();

	public static List<ODocument> executeIdempotentQuery(ODatabaseDocumentTx db, StringBuilder query) {
		return executeIdempotentQuery( db, query.toString() );
	}

	public static  List<ODocument> executeIdempotentQuery(ODatabaseDocumentTx db, String query) {
		return executeIdempotentQueryWithParams( db, query, Collections.<String, Object>emptyMap() );
	}

	public static List<ODocument> executeIdempotentQueryWithParams(ODatabaseDocumentTx db, String query, Map<String, Object> queryParams) {
		List<ODocument> resultElements = null;
		try ( OResultSet resultSet = db.query( query, queryParams ) ) {
			resultElements = resultSet.elementStream()
					.map( element -> {
						return (ODocument) element;
					} )
					.collect( Collectors.toList() );
			log.debugf( "load documents: %d",resultElements.size() );
		}
		catch (OCommandSQLParsingException e1) {
			throw log.cannotParseQuery( query, e1 );
		}
		catch (OCommandExecutionException e2) {
			throw log.cannotExecuteQuery( query, e2 );
		}
		return resultElements;
	}

	public static Object executeNonIdempotentQuery(ODatabaseDocumentTx db, StringBuilder query) {
		return executeNonIdempotentQuery( db, query.toString() );
	}

	public static Object executeNonIdempotentQuery(ODatabaseDocumentTx db, String query) {
		log.debugf( "NonIdempotentQuery: %s", query );
		ODocument result  = null;
		try ( OResultSet resultSet = db.command( query ) ) {
			result  = (ODocument) resultSet.next().toElement();
		}
		catch (OCommandSQLParsingException e1) {
			throw log.cannotParseQuery( query, e1 );
		}
		catch (OCommandExecutionException e2) {
			throw log.cannotExecuteQuery( query, e2 );
		}
		return result;
	}
}
