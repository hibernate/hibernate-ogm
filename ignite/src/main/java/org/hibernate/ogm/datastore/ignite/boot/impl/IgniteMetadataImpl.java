/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.boot.impl;

import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.internal.SessionFactoryBuilderImpl;
import org.hibernate.boot.spi.AbstractDelegatingMetadata;
import org.hibernate.boot.spi.MetadataImplementor;

/**
 * Ignite-specific implementation of {@link MetadataImplementor} using delegation.
 * @author Dmitriy Kozlov
 * @author Victor Kadachigov
 *
 */
public class IgniteMetadataImpl extends AbstractDelegatingMetadata {

	public IgniteMetadataImpl(MetadataImplementor delegate) {
		super( delegate );
	}
	@Override
	public SessionFactoryBuilder getSessionFactoryBuilder() {
		final SessionFactoryBuilderImpl defaultBuilder = new SessionFactoryBuilderImpl( this );
		return new IgniteSessionFactoryBuilderImpl(this, defaultBuilder);
	}

}
