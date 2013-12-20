/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.type.impl;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.type.TypeTranslator;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;

/**
 * Inialize {@link TypeTranslator}.
 *
 * This is a {@linl SessionFactoryServiceInitiator} since it depends on {@link DatastoreServices}
 * which itself is a {@code SessionFactoryServiceInitiator}.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class TypeTranslatorInitiator implements SessionFactoryServiceInitiator<TypeTranslator> {

	public static final TypeTranslatorInitiator INSTANCE = new TypeTranslatorInitiator();

	@Override
	public Class<TypeTranslator> getServiceInitiated() {
		return TypeTranslator.class;
	}

	@Override
	public TypeTranslator initiateService(SessionFactoryImplementor sessionFactory, Configuration configuration, ServiceRegistryImplementor registry) {
		return createService( registry );
	}

	@Override
	public TypeTranslator initiateService(SessionFactoryImplementor sessionFactory, MetadataImplementor metadata, ServiceRegistryImplementor registry) {
		return createService( registry );
	}

	private TypeTranslator createService(ServiceRegistryImplementor registry) {
		GridDialect dialect = registry.getService( GridDialect.class );
		return new TypeTranslatorImpl( dialect );
	}

}
