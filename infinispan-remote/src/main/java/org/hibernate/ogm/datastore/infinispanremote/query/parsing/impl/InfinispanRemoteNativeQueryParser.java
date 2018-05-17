/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl;

import java.lang.invoke.MethodHandles;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.ogm.datastore.infinispanremote.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.infinispanremote.query.impl.InfinispanRemoteQueryDescriptor;

/**
 * Utility class to extract information we need about the Ickle native query
 *
 * @author Davide D'Alto
 * @author Fabio Massimo Ercoli
 */

public class InfinispanRemoteNativeQueryParser {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private static final Pattern PROJECTIONS_PATTERN = Pattern.compile( "^\\s*select\\s+(.*?)\\s+from\\s+(.*?)", Pattern.CASE_INSENSITIVE );
	private static final Pattern FROM_PATTERN = Pattern.compile( "^\\s*(select\\s+(.*?)\\s+)?(from\\s+.*?)(\\s+where\\s*.*)?\\s*", Pattern.CASE_INSENSITIVE );

	private final String nativeQuery;

	public InfinispanRemoteNativeQueryParser(String nativeQuery) {
		this.nativeQuery = nativeQuery;
	}

	/**
	 * Creates an instance of {@link InfinispanRemoteQueryDescriptor}, as a result of the parse of a given native query
	 *
	 * @return Query descriptor
	 */
	public InfinispanRemoteQueryDescriptor parse() {
		String fromClause = fromClause();
		validateFromClause( fromClause );
		String cacheName = cacheName( fromClause );
		String entityAlias = entityAlias( fromClause );
		String[] projections = projections( entityAlias );
		return new InfinispanRemoteQueryDescriptor( cacheName, nativeQuery, projections );
	}

	private String[] projections(String entityAlias) {
		Matcher matcher = PROJECTIONS_PATTERN.matcher( nativeQuery );
		if ( matcher.matches() ) {
			String selectClause = matcher.group( 1 );
			String[] split = selectClause.split( "\\s*,\\s*" );
			if ( split.length > 1 ) {
				return split;
			}
			if ( split.length == 1 && !split[0].equals( entityAlias ) ) {
				// We exclude the case when the projection is the entity.
				// Example: SELECT f FROM Flower f
				return split;
			}
		}
		return null;
	}

	private String cacheName(String fromClause) {
		String[] fromWords = fromClause.split( "\\s+" );
		String[] cacheFullName = fromWords[0].split( "\\." );
		String cacheName = cacheFullName[cacheFullName.length - 1];
		return cacheName;
	}

	private String entityAlias(String fromClause) {
		String[] fromWords = fromClause.split( "\\s+" );
		if ( fromWords.length > 1 ) {
			return fromWords[1];
		}
		return null;
	}

	private String fromClause() {
		Matcher fromMatcher = FROM_PATTERN.matcher( nativeQuery );
		if ( fromMatcher.matches() ) {
			String clause = fromMatcher.group( 3 );
			// Remove the initial part: "from "
			return clause.substring( 4 ).trim();
		}

		throw log.missingFromClauseInNativeQuery( nativeQuery );
	}

	private void validateFromClause(String clause) {
		String[] split = clause.split( "\\s*,\\s*" );
		if ( split.length > 1 ) {
			throw log.multipleEntitiesInFromClause( split, nativeQuery );
		}
	}
}
