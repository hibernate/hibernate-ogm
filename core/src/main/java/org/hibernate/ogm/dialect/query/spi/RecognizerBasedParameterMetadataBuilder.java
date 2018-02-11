/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.query.spi;

import java.util.Map;

import org.hibernate.engine.query.spi.NamedParameterDescriptor;
import org.hibernate.engine.query.spi.OrdinalParameterDescriptor;
import org.hibernate.engine.query.spi.ParamLocationRecognizer;
import org.hibernate.engine.query.spi.ParameterParser.Recognizer;
import org.hibernate.query.internal.ParameterMetadataImpl;

/**
 * Base class for {@link ParameterMetadataBuilder}s based on ORM's {@link ParamLocationRecognizer} SPI.
 *
 * @author Gunnar Morling
 */
public abstract class RecognizerBasedParameterMetadataBuilder implements ParameterMetadataBuilder {

	@Override
	public ParameterMetadataImpl buildParameterMetadata(String nativeQuery) {
		ParamLocationRecognizer recognizer = new ParamLocationRecognizer( 0 );
		parseQueryParameters( nativeQuery, recognizer );
		recognizer.complete();

		final Map<Integer, OrdinalParameterDescriptor> ordinalDescriptors = recognizer.getOrdinalParameterDescriptionMap();
		final Map<String, NamedParameterDescriptor> namedDescriptors = recognizer.getNamedParameterDescriptionMap();

		return new ParameterMetadataImpl( ordinalDescriptors, namedDescriptors );
	}

	/**
	 * Parses the given native NoSQL query string, collecting the contained named parameters in the course of doing so.
	 *
	 * @param noSqlQuery the query to parse
	 * @param recognizer collects any named parameters contained in the given query
	 */
	protected abstract void parseQueryParameters(String noSqlQuery, Recognizer recognizer);
}
