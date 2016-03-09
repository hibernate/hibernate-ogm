/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.boot.impl;

import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.spi.AbstractDelegatingSessionFactoryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryBuilderImplementor;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.boot.OgmSessionFactoryBuilder;
import org.hibernate.ogm.datastore.ignite.impl.IgniteSessionFactoryImpl;

/**
 * Ignite-specific implementation of SessionFactoryBuilder, using delegation
 * @author Dmitriy Kozlov
 *
 */
public class IgniteSessionFactoryBuilderImpl extends AbstractDelegatingSessionFactoryBuilder<IgniteSessionFactoryBuilderImpl> implements OgmSessionFactoryBuilder {

	private final SessionFactoryBuilderImplementor delegate;
	private final MetadataImplementor metadata;

	public IgniteSessionFactoryBuilderImpl(MetadataImplementor metadata, SessionFactoryBuilderImplementor delegate) {
		super( delegate );

		this.metadata = metadata;
		this.delegate = delegate;
	}

	@Override
	protected IgniteSessionFactoryBuilderImpl getThis() {
		return this;
	}

	@Override
	public <T extends SessionFactoryBuilder> T unwrap(Class<T> type) {
		if ( type.isAssignableFrom( getClass() ) ) {
			return type.cast( this );
		}
		else {
			return delegate.unwrap( type );
		}
	}

	@Override
	public OgmSessionFactory build() {
		return new IgniteSessionFactoryImpl( new SessionFactoryImpl( metadata, delegate.buildSessionFactoryOptions() ) );
	}

}
