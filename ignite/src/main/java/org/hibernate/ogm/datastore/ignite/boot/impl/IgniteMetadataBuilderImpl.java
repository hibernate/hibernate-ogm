package org.hibernate.ogm.datastore.ignite.boot.impl;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.spi.AbstractDelegatingMetadataBuilderImplementor;
import org.hibernate.boot.spi.MetadataBuilderImplementor;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.MetadataImplementor;

/**
 * Ignite-specific implementation of {@link MetadataBuilderImplementor} using delegation.
 * 
 * @author Dmitriy Kozlov
 * @author Victor Kadachigov
 *
 */
public class IgniteMetadataBuilderImpl extends AbstractDelegatingMetadataBuilderImplementor<IgniteMetadataBuilderImpl> {

	public IgniteMetadataBuilderImpl(MetadataBuilderImplementor delegate) {
		super(delegate);
	}

	@Override
	public MetadataImplementor build() {
		return new IgniteMetadataImpl((MetadataImplementor)getDelegate().build());
	}

	@Override
	public MetadataBuildingOptions getMetadataBuildingOptions() {
		return getDelegate().getMetadataBuildingOptions();
	}

	@Override
	public <T extends MetadataBuilder> T unwrap(Class<T> type) {
		if ( type.isAssignableFrom( getClass() ) ) {
			return type.cast( this );
		}
		else {
			return getDelegate().unwrap( type );
		}
	}

	@Override
	protected IgniteMetadataBuilderImpl getThis() {
		return this;
	}

}
