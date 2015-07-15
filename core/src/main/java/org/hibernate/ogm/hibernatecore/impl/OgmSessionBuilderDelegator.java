/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.hibernatecore.impl;

import org.hibernate.Session;
import org.hibernate.engine.spi.ForwardingSessionBuilderImplementor;
import org.hibernate.engine.spi.SessionBuilderImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.engine.spi.OgmSessionBuilderImplementor;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class OgmSessionBuilderDelegator extends ForwardingSessionBuilderImplementor implements OgmSessionBuilderImplementor {

	private final SessionBuilderImplementor builder;
	private final OgmSessionFactory factory;

	public OgmSessionBuilderDelegator(SessionBuilderImplementor sessionBuilder, OgmSessionFactory factory) {
		super( sessionBuilder );

		this.builder = sessionBuilder;
		this.factory = factory;
	}

	@Override
	public OgmSession openSession() {
		Session session = builder.openSession();
		return new OgmSessionImpl( factory, (EventSource) session );
	}
}
