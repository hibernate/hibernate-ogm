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

import org.hibernate.ogm.datastore.orientdb.logging.impl.Log;
import org.hibernate.ogm.datastore.orientdb.logging.impl.LoggerFactory;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.function.OFunction;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import org.hibernate.ogm.datastore.orientdb.constant.OrientDBConstant;

/**
 * Util class for execute queries by native OrientDB API
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
public class NativeQueryUtil {

	private static final Log log = LoggerFactory.getLogger();

	public static List<ODocument> executeIdempotentQuery(ODatabaseDocumentTx db, StringBuilder query) {
		return executeIdempotentQuery( db, query.toString() );
	}

	public static List<ODocument> executeIdempotentQuery(ODatabaseDocumentTx db, String query) {
		return executeIdempotentQueryWithParams( db, query, Collections.<String, Object>emptyMap() );
	}

	public static List<ODocument> executeIdempotentQueryWithParams(ODatabaseDocumentTx db, String query, Map<String, Object> queryParams) {
		return db.command( new OSQLSynchQuery<ODocument>( query ) ).execute( queryParams );
	}

	public static Object executeNonIdempotentQuery(ODatabaseDocumentTx db, StringBuilder query) {
		return executeNonIdempotentQuery( db, query.toString() );
	}

	public static Object executeNonIdempotentQuery(ODatabaseDocumentTx db, String query) {
		OFunction executeQuery = db.getMetadata().getFunctionLibrary().getFunction( OrientDBConstant.EXECUTE_QUERY_FUNC );
		if ( executeQuery == null ) {
			db.getMetadata().reload();
			executeQuery = db.getMetadata().getFunctionLibrary().getFunction( OrientDBConstant.EXECUTE_QUERY_FUNC );
		}
		return executeQuery.execute( query );
	}
}
