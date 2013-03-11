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
package org.hibernate.ogm.cfg;

import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.cfg.impl.OgmNamingStrategy;
import org.hibernate.ogm.hibernatecore.impl.OgmSessionFactory;
import org.hibernate.ogm.massindex.OgmMassIndexerFactory;
import org.hibernate.search.hcore.impl.MassIndexerFactoryIntegrator;

/**
 * An instance of {@link OgmConfiguration} allows the application
 * to specify properties and mapping documents to be used when
 * creating an {@link OgmSessionFactory}.
 *
 * @author Davide D'Alto
 */
public class OgmConfiguration extends Configuration {

	public static final String OGM_ON = "hibernate.ogm._activate";

	public OgmConfiguration() {
		super();
		resetOgm();
	}

	private void resetOgm() {
		super.setNamingStrategy( OgmNamingStrategy.INSTANCE );
		setProperty( OGM_ON, "true" );
		// This property bind the OgmMassIndexer with Hibernate Search. An application could use OGM without Hibernate
		// Search therefore we set property value and key using a String in case the dependency is not on the classpath.
		setProperty( "hibernate.search.massindexer.factoryclass", "org.hibernate.ogm.massindex.OgmMassIndexerFactory" );
	}

	@Override
	public SessionFactory buildSessionFactory() throws HibernateException {
		return new OgmSessionFactory( (SessionFactoryImplementor ) super.buildSessionFactory() );
	}

	@Override
	public Configuration setProperties(Properties properties) {
		super.setProperties( properties );
		//Unless the new configuration properties explicitly disable OGM,
		//assume there was no intention to disable it:
		if ( ! properties.containsKey( OGM_ON ) ) {
			setProperty( OGM_ON, "true" );
		}
		return this;
	}

}
