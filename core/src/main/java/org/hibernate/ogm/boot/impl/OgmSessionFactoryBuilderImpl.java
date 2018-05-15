/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.boot.impl;

import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.spi.AbstractDelegatingSessionFactoryBuilderImplementor;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryBuilderImplementor;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.boot.OgmSessionFactoryBuilderImplementor;
import org.hibernate.ogm.hibernatecore.impl.OgmSessionFactoryImpl;

/**
 * {@link SessionFactoryBuilder} building the {@link OgmSessionFactory}.
 *
 * @author Gunnar Morling
 */
public class OgmSessionFactoryBuilderImpl extends AbstractDelegatingSessionFactoryBuilderImplementor<OgmSessionFactoryBuilderImplementor>
		implements OgmSessionFactoryBuilderImplementor {

	private final SessionFactoryBuilderImplementor delegate;
	private final MetadataImplementor metadata;

	public OgmSessionFactoryBuilderImpl(MetadataImplementor metadata, SessionFactoryBuilderImplementor delegate) {
		super( delegate );

		this.metadata = metadata;
		this.delegate = delegate;
	}

	@Override
	protected OgmSessionFactoryBuilderImplementor getThis() {
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
		OgmSessionFactoryOptions options = new OgmSessionFactoryOptions( delegate.buildSessionFactoryOptions() );

		// not overrides bootstrapContext
		return new OgmSessionFactoryImpl( new SessionFactoryImpl( null, metadata, options ) );
	}
}
