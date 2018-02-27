/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.hibernatecore.impl;

import org.hibernate.Session;
import org.hibernate.engine.spi.AbstractDelegatingSessionBuilderImplementor;
import org.hibernate.engine.spi.SessionBuilderImplementor;
import org.hibernate.engine.spi.SessionOwner;
import org.hibernate.event.spi.EventSource;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.engine.spi.OgmSessionBuilderImplementor;
import org.hibernate.ogm.jpa.impl.OgmExceptionMapper;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.AfterCompletionAction;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.ExceptionMapper;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.ManagedFlushChecker;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class OgmSessionBuilderDelegator extends AbstractDelegatingSessionBuilderImplementor<OgmSessionBuilderImplementor>
		implements OgmSessionBuilderImplementor {

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

	@Override
	public OgmSessionBuilderImplementor owner(SessionOwner sessionOwner) {
		if ( sessionOwner.getExceptionMapper() != null ) {
			sessionOwner = new OgmExceptionMapperSessionOwner( sessionOwner );
		}

		builder.owner( sessionOwner );
		return this;
	}

	/**
	 * {@link SessionOwner} which injects OGM's {@link ExceptionMapper}.
	 *
	 * @author Gunnar Morling
	 */
	private static class OgmExceptionMapperSessionOwner extends ForwardingSessionOwner {

		public OgmExceptionMapperSessionOwner(SessionOwner delegate) {
			super( delegate );
		}

		@Override
		public ExceptionMapper getExceptionMapper() {
			return new OgmExceptionMapper( super.getExceptionMapper() );
		}
	}

	/**
	 * Delegating {@link SessionOwner}.
	 *
	 * @author Gunnar Morling
	 */
	private abstract static class ForwardingSessionOwner implements SessionOwner {

		private final SessionOwner delegate;

		public ForwardingSessionOwner(SessionOwner delegate) {
			this.delegate = delegate;
		}

		@Override
		public boolean shouldAutoCloseSession() {
			return delegate.shouldAutoCloseSession();
		}

		@Override
		public ExceptionMapper getExceptionMapper() {
			return delegate.getExceptionMapper();
		}

		@Override
		public AfterCompletionAction getAfterCompletionAction() {
			return delegate.getAfterCompletionAction();
		}

		@Override
		public ManagedFlushChecker getManagedFlushChecker() {
			return delegate.getManagedFlushChecker();
		}
	}
}
