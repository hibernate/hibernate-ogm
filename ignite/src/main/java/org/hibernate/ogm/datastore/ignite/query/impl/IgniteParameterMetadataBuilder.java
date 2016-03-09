/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.query.impl;

import org.hibernate.engine.query.spi.ParameterParser;
import org.hibernate.engine.query.spi.ParameterParser.Recognizer;
import org.hibernate.ogm.datastore.ignite.logging.impl.Log;
import org.hibernate.ogm.datastore.ignite.logging.impl.LoggerFactory;
import org.hibernate.ogm.dialect.query.spi.RecognizerBasedParameterMetadataBuilder;

public class IgniteParameterMetadataBuilder extends	RecognizerBasedParameterMetadataBuilder {

	public static final IgniteParameterMetadataBuilder INSTANCE = new IgniteParameterMetadataBuilder();

	private static final Log log = LoggerFactory.getLogger();

	@Override
	protected void parseQueryParameters(String noSqlQuery, Recognizer recognizer) {

		ParameterParser.parse( noSqlQuery, recognizer );

	}

}
