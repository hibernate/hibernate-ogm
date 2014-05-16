/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.datastore.neo4j.query.impl;

import org.hibernate.engine.query.spi.ParameterParser.Recognizer;
import org.hibernate.ogm.query.RecognizerBasedParameterMetadataBuilder;
import org.hibernate.ogm.query.spi.ParameterMetadataBuilder;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.RecoveringParseRunner;

/**
 * {@link ParameterMetadataBuilder} for native Neo4j queries. The implementation is based on a <a
 * href="http://parboiled.org">parboiled</a> grammar.
 *
 * @author Gunnar Morling
 */
public class Neo4jParameterMetadataBuilder extends RecognizerBasedParameterMetadataBuilder {

	@Override
	public void parseQueryParameters(String nativeQuery, Recognizer journaler) {
		QueryParser parser = Parboiled.createParser( QueryParser.class, journaler );
		new RecoveringParseRunner<Recognizer>( parser.Query() ).run( nativeQuery );
	}
}
