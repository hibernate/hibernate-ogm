/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.query.spi;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.query.spi.NamedParameterDescriptor;
import org.hibernate.engine.query.spi.OrdinalParameterDescriptor;
import org.hibernate.engine.query.spi.ParamLocationRecognizer;
import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.engine.query.spi.ParameterParser.Recognizer;

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
