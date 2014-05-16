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
package org.hibernate.ogm.query;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.query.spi.NamedParameterDescriptor;
import org.hibernate.engine.query.spi.OrdinalParameterDescriptor;
import org.hibernate.engine.query.spi.ParamLocationRecognizer;
import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.engine.query.spi.ParameterParser.Recognizer;
import org.hibernate.ogm.query.spi.ParameterMetadataBuilder;

/**
 * Base class for {@link ParameterMetadataBuilder}s based on ORM's {@link ParamLocationRecognizer} SPI.
 *
 * @author Gunnar Morling
 */
public abstract class RecognizerBasedParameterMetadataBuilder implements ParameterMetadataBuilder {

	@Override
	public ParameterMetadata buildParameterMetadata(String nativeQuery) {
		ParamLocationRecognizer recognizer = new ParamLocationRecognizer();
		parseQueryParameters( nativeQuery, recognizer );

		final int size = recognizer.getOrdinalParameterLocationList().size();
		final OrdinalParameterDescriptor[] ordinalDescriptors = new OrdinalParameterDescriptor[ size ];
		for ( int i = 0; i < size; i++ ) {
			final Integer position = recognizer.getOrdinalParameterLocationList().get( i );
			ordinalDescriptors[i] = new OrdinalParameterDescriptor( i, null, position );
		}

		final Map<String, NamedParameterDescriptor> namedParamDescriptorMap = new HashMap<String, NamedParameterDescriptor>();
		final Map<String, ParamLocationRecognizer.NamedParameterDescription> map = recognizer.getNamedParameterDescriptionMap();
		for ( final String name : map.keySet() ) {
			final ParamLocationRecognizer.NamedParameterDescription description = map.get( name );
			namedParamDescriptorMap.put(
					name,
					new NamedParameterDescriptor(
							name,
							null,
							description.buildPositionsArray(),
							description.isJpaStyle()
					)
			);
		}

		return new ParameterMetadata( ordinalDescriptors, namedParamDescriptorMap );
	}

	/**
	 * Parses the given native NoSQL query string, collecting the contained named parameters in the course of doing so.
	 *
	 * @param noSqlQuery the query to parse
	 * @param recognizer collects any named parameters contained in the given query
	 */
	protected abstract void parseQueryParameters(String noSqlQuery, Recognizer recognizer);
}
