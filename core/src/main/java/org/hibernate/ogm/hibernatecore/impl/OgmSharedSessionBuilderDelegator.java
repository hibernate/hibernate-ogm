/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.hibernatecore.impl;

import org.hibernate.SharedSessionBuilder;
import org.hibernate.engine.spi.AbstractDelegatingSharedSessionBuilder;
import org.hibernate.event.spi.EventSource;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.OgmSessionFactory;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class OgmSharedSessionBuilderDelegator extends AbstractDelegatingSharedSessionBuilder<SharedSessionBuilder> {
	private final SharedSessionBuilder builder;
	private final OgmSessionFactory factory;

	public OgmSharedSessionBuilderDelegator(SharedSessionBuilder builder, OgmSessionFactory factory) {
		super( builder );

		this.builder = builder;
		this.factory = factory;
	}

	@Override
	public OgmSession openSession() {
		return new OgmSessionImpl( factory, (EventSource) builder.openSession() );
	}
}
