/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.query.spi;

import org.hibernate.engine.query.spi.ParameterMetadata;

/**
 * Default implementation of {@link ParameterMetadataBuilder} which returns no parameter at all.
 *
 * @author Gunnar Morling
 */
public class NoOpParameterMetadataBuilder implements ParameterMetadataBuilder {

	public static final NoOpParameterMetadataBuilder INSTANCE = new NoOpParameterMetadataBuilder();

	private static final ParameterMetadata NO_PARAMETERS = new ParameterMetadata( null, null );

	private NoOpParameterMetadataBuilder() {
	}

	@Override
	public ParameterMetadata buildParameterMetadata(String nativeQuery) {
		return NO_PARAMETERS;
	}
}
