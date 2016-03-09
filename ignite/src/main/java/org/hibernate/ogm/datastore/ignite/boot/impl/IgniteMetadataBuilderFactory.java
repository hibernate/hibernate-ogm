/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.boot.impl;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.spi.MetadataBuilderFactory;
import org.hibernate.boot.spi.MetadataBuilderImplementor;

/**
 * Implementation of MetadataBuilderFactory for building Ignite-specific metadata
 * @author Dmitriy Kozlov
 * @author Victor Kadachigov
 *
 */
public class IgniteMetadataBuilderFactory implements MetadataBuilderFactory {

	@Override
	public MetadataBuilderImplementor getMetadataBuilder(MetadataSources metadatasources, MetadataBuilderImplementor defaultBuilder) {
		return new IgniteMetadataBuilderImpl( defaultBuilder );
	}

}
