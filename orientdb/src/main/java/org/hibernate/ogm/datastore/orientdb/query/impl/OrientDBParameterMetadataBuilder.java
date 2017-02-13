/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.query.impl;

import org.hibernate.engine.query.spi.ParameterParser;
import org.hibernate.ogm.dialect.query.spi.RecognizerBasedParameterMetadataBuilder;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.RecoveringParseRunner;

/**
 * {@link org.hibernate.ogm.dialect.query.spi.ParameterMetadataBuilder} for native OrientDB queries. The implementation
 * is based on a <a href="http://parboiled.org">parboiled</a> grammar.
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
public class OrientDBParameterMetadataBuilder extends RecognizerBasedParameterMetadataBuilder {

	/**
	 * Parses the given native NoSQL query string, collecting the contained named parameters in the course of doing so.
	 *
	 * @param noSqlQuery the query to parse
	 * @param recognizer collects any named parameters contained in the given query
	 */
	@Override
	public void parseQueryParameters(String noSqlQuery, ParameterParser.Recognizer recognizer) {
		OrientDBQueryParser parser = Parboiled.createParser( OrientDBQueryParser.class, recognizer );
		new RecoveringParseRunner<ParameterParser.Recognizer>( parser.Query() ).run( noSqlQuery );
	}

}
