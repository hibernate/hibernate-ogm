/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.query.spi;

import org.hibernate.engine.query.spi.ParameterMetadata;

/**
 * Implementations return metadata about the parameters contained in given native queries of the corresponding NoSQL
 * store. Implementations may support named parameters, ordinal parameters or both, based on the capabilities of the
 * underlying store.
 *
 * @author Gunnar Morling
 */
public interface ParameterMetadataBuilder {

	/**
	 * Retrieves meta-data about the named and ordinal parameters contained in the given native NoSQL query.
	 *
	 * @param nativeQuery the query to analyze
	 * @return meta-data about the parameters contained in the given native query
	 */
	ParameterMetadata buildParameterMetadata(String nativeQuery);
}
