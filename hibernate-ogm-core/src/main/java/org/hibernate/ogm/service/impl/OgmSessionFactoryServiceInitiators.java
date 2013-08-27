/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.service.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.ogm.datastore.impl.DatastoreProviderInitiator;
import org.hibernate.ogm.datastore.impl.DatastoreServicesInitiator;
import org.hibernate.ogm.dialect.impl.GridDialectFactoryInitiator;
import org.hibernate.ogm.type.impl.TypeTranslatorInitiator;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;

/**
 * Central definition of the standard set of initiators defined by OGM for the
 * {@link org.hibernate.service.spi.SessionFactoryServiceRegistry}
 *
 * @see OgmSessionFactoryServiceRegistryImpl
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class OgmSessionFactoryServiceInitiators {

	public static List<SessionFactoryServiceInitiator<?>> LIST = Collections.unmodifiableList( Arrays.<SessionFactoryServiceInitiator<?>>asList(
			TypeTranslatorInitiator.INSTANCE,
			DatastoreServicesInitiator.INSTANCE,
			DatastoreProviderInitiator.INSTANCE,
			GridDialectFactoryInitiator.INSTANCE,
			QueryParserServicesInitiator.INSTANCE
	) );

}
