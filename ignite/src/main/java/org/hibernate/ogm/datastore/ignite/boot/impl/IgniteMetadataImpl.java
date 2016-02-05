package org.hibernate.ogm.datastore.ignite.boot.impl;

import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.internal.SessionFactoryBuilderImpl;
import org.hibernate.boot.spi.AbstractDelegatingMetadata;
import org.hibernate.boot.spi.MetadataImplementor;

/**
 * Ignite-specific implementation of {@link MetadataImplementor} using delegation.
 * 
 * @author Dmitriy Kozlov
 * @author Victor Kadachigov
 *
 */
public class IgniteMetadataImpl extends AbstractDelegatingMetadata {

	public IgniteMetadataImpl(MetadataImplementor delegate) {
		super(delegate);
	}
	
	@Override
	public SessionFactoryBuilder getSessionFactoryBuilder() {
		final SessionFactoryBuilderImpl defaultBuilder = new SessionFactoryBuilderImpl( this );
		return new IgniteSessionFactoryBuilderImpl(this, defaultBuilder);
	}

}
