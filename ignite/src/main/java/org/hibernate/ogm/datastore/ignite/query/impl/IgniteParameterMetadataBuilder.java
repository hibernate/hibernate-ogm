package org.hibernate.ogm.datastore.ignite.query.impl;

import org.hibernate.engine.query.spi.ParameterParser;
import org.hibernate.engine.query.spi.ParameterParser.Recognizer;
import org.hibernate.ogm.datastore.ignite.logging.impl.Log;
import org.hibernate.ogm.datastore.ignite.logging.impl.LoggerFactory;
import org.hibernate.ogm.dialect.query.spi.RecognizerBasedParameterMetadataBuilder;

public class IgniteParameterMetadataBuilder extends	RecognizerBasedParameterMetadataBuilder {
	
	private static final Log log = LoggerFactory.getLogger();
	
	public static final IgniteParameterMetadataBuilder INSTANCE = new IgniteParameterMetadataBuilder();

	@Override
	protected void parseQueryParameters(String noSqlQuery, Recognizer recognizer) {
		
		ParameterParser.parse(noSqlQuery, recognizer );

	}

}
