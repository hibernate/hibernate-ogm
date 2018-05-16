/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.ogm.datastore.infinispanremote.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.infinispanremote.query.impl.InfinispanRemoteQueryDescriptor;

/**
 * Utility class to extract the cache name and projections from an Infinispan Server native query.
 *
 * @author Fabio Massimo Ercoli
 */
public class InfinispanRemoteNativeQueryParser {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final SelectAndFromExtractor extractor = new SelectAndFromExtractor();
	private final String nativeQuery;

	private String select = null;
	private String from = null;
	private String alias = null;
	private List<String> projections = new ArrayList<>();

	public InfinispanRemoteNativeQueryParser(String nativeQuery) {
		this.nativeQuery = nativeQuery;
	}

	/**
	 * Creates an instance of {@link InfinispanRemoteQueryDescriptor},
	 * as a result of the parse of a given native query
	 *
	 * @return Query descriptor
	 */
	public InfinispanRemoteQueryDescriptor parse() {
		clearStatus();
		extractor.extractSelectAndFrom();

		if ( from == null || from.isEmpty() ) {
			throw log.errorOnParsingNativeQuery( nativeQuery );
		}

		String[] fromWords = from.split( "\\s" );
		if ( fromWords.length > 2 ) {
			throw log.errorOnParsingNativeQuery( nativeQuery );
		}

		if ( fromWords.length == 2 ) {
			alias = fromWords[1];
		}
		String[] cacheFullName = fromWords[0].split( "\\." );
		String cacheName = cacheFullName[cacheFullName.length - 1];

		makeProjection();
		return new InfinispanRemoteQueryDescriptor( cacheName, nativeQuery, projections );
	}

	private void clearStatus() {
		select = null;
		from = null;
		alias = null;
		projections.clear();
	}

	private void makeProjection() {
		if ( select.isEmpty() ) {
			projections = Collections.emptyList();
			return;
		}

		String[] projectors = select.split( "\\s*,\\s*" );
		for ( int i = 0; i < projectors.length; i++ ) {
			addProjector( projectors[i] );
		}
	}

	private void addProjector(String projector) {
		if ( alias == null ) {
			projections.add( projector );
			return;
		}
		if ( alias.equals( projector ) ) {
			return;
		}
		if ( !projector.startsWith( alias + "." ) ) {
			throw log.errorOnParsingNativeQuery( nativeQuery );
		}

		projections.add( projector.substring( alias.length() + 1 ) );
	}

	private final class SelectAndFromExtractor {

		private Pattern fromPattern = Pattern.compile( "^\\s*from\\s+(.*)\\s*", Pattern.CASE_INSENSITIVE );
		private Pattern fromWherePattern = Pattern.compile( "^\\s*from\\s+(.*?)\\s+where\\s+(.*)\\s*", Pattern.CASE_INSENSITIVE );
		private Pattern selectPattern = Pattern.compile( "\\s*select\\s*(.*?)\\s+from\\s+(.*)\\s*", Pattern.CASE_INSENSITIVE );
		private Pattern selectWherePattern = Pattern.compile( "\\s*select\\s*(.*?)\\s+from\\s+(.*?)\\s*where\\s*(.*)\\s*", Pattern.CASE_INSENSITIVE );

		private void extractSelectAndFrom() {
			Matcher fromMatcher = fromPattern.matcher( nativeQuery );
			Matcher fromWhereMatcher = fromWherePattern.matcher( nativeQuery );
			Matcher selectMatcher = selectPattern.matcher( nativeQuery );
			Matcher selectWhereMatcher = selectWherePattern.matcher( nativeQuery );

			boolean matchFrom = fromMatcher.find();
			boolean matchFromWhere = fromWhereMatcher.find();
			boolean matchSelect = selectMatcher.find();
			boolean matchSelectWhere = selectWhereMatcher.find();

			if ( !matchFrom && !matchSelect ) {
				throw log.errorOnParsingNativeQuery( nativeQuery );
			}

			if ( matchFrom ) {
				select = "";
				from = ( matchFromWhere ) ? fromWhereMatcher.group( 1 ) : fromMatcher.group( 1 );
			}
			else {
				select = selectMatcher.group( 1 );
				from = ( matchSelectWhere ) ? selectWhereMatcher.group( 2 ) : selectMatcher.group( 2 );
			}
		}
	}
}
