/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.hibernatecore.impl;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.SessionFactoryRegistry;

/**
 * Resolves {@link OgmSessionFactory} instances during <tt>JNDI<tt> look-ups as well as during deserialization
 *
 * @author Davide D'Alto
 */
public class OgmSessionFactoryObjectFactory implements ObjectFactory {

	@Override
	public Object getObjectInstance(Object reference, Name name, Context nameCtx, Hashtable<?, ?> environment)
			throws Exception {
		final String uuid = (String) ( (Reference) reference ).get( 0 ).getContent();
		//OgmSessionFactory does not have state so we can create a new instance each time instead of keeping a registry
		return new OgmSessionFactory( (SessionFactoryImplementor) SessionFactoryRegistry.INSTANCE.getSessionFactory( uuid ) );
	}

}
