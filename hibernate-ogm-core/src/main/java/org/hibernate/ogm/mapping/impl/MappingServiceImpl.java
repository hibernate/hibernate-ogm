/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.mapping.impl;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.mapping.spi.MappingService;

import java.util.List;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class MappingServiceImpl implements MappingService {
	private final MappingContext options;

	public MappingServiceImpl(MappingContext options) {
		this.options = options;
	}


	@Override
	public MappingServiceContext withSession(SessionImplementor session) {
		return new MappingServiceContextImpl(session);
	}

	public final class MappingServiceContextImpl implements MappingServiceContext {

		private final SessionImplementor session;

		public MappingServiceContextImpl(SessionImplementor session) {
			this.session = session;
		}

		@Override
		public List<Object> getGlobalOptions() {
			//FIXME continue here with filtering
			return session == null ?
					MappingServiceImpl.this.options.getGlobalOptions() :
					null;
		}

		@Override
		public List<Object> getEntityOptions(Class<?> entityType) {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public List<Object> getPropertyOptions(Class<?> entityType, String propertyName) {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}
	}
}
