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
package org.hibernate.ogm.test.utils;

import java.util.Map;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.junit.rules.TemporaryFolder;

/**
 * This helper to manage the SessionFactory as a JUnit Rule is intended only for
 * annotated entities. Can be improved if need for other configurations arises.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class SessionFactoryRule extends TemporaryFolder {

	static {
		TestHelper.initializeHelpers();
	}

	private final Class<?>[] entities;
	private final OgmConfiguration cfg = new OgmConfiguration();
	private SessionFactory sessions;
	private Session session;

	public SessionFactoryRule(Class<?>... entities) {
		if ( entities == null || entities.length == 0 ) {
			throw new IllegalArgumentException( "Define at least a single annotated entity" );
		}
		this.entities = entities;
		cfg.setProperty( Environment.HBM2DDL_AUTO, "none" );
		// by default use the new id generator scheme...
		cfg.setProperty( Configuration.USE_NEW_ID_GENERATOR_MAPPINGS, "true" );
		// volatile indexes for Hibernate Search (if used)
		cfg.setProperty( "hibernate.search.default.directory_provider", "ram" );
		// disable warnings about unspecified Lucene version
		cfg.setProperty( "hibernate.search.lucene_version", "LUCENE_35" );
		for ( Map.Entry<String, String> entry : TestHelper.getEnvironmentProperties().entrySet() ) {
			cfg.setProperty( entry.getKey(), entry.getValue() );
		}
	}

	public SessionFactoryRule setProperty(String key, String value) {
		if ( sessions != null ) {
			throw new IllegalStateException( "SessionFactory already created" );
		}
		cfg.setProperty( key, value );
		return this;
	}

	@Override
	public void before() throws Throwable {
		super.before();
		// start the SessionFactory eagerly so that it's part of the test fixture
		getSessionFactory();
	}

	@Override
	public void after() {
		if ( session != null && session.isOpen() ) {
			session.close();
			System.err.println( "Had to close your lingering Session" );
		}
		if ( sessions != null ) {
			sessions.close();
		}
		super.after();
	}

	public Session openSession() {
		if ( session == null || !session.isOpen() ) {
			return getSessionFactory().openSession();
		}
		else {
			throw new IllegalStateException( "Previous session not closed! Manage your own if you need multiple sessions." );
		}
	}

	public SessionFactory getSessionFactory() {
		if ( sessions == null ) {
			for ( Class<?> annotatedClass : entities ) {
				cfg.addAnnotatedClass( annotatedClass );
			}
			sessions = cfg.buildSessionFactory();
		}
		return sessions;
	}

}
