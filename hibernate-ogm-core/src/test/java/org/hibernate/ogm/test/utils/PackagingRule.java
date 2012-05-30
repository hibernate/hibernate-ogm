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
package org.hibernate.ogm.test.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.rules.TemporaryFolder;

/**
 * test case useful when one want to write a test relying on an archive (like a JPA archive)
 *
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Sanne Grinovero
 */
public class PackagingRule extends TemporaryFolder {

	private static final ArchivePath persistencePath = ArchivePaths.create( "persistence.xml" );

	protected static ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

	private final JavaArchive archive;
	private final File testPackage;

	public PackagingRule(String persistenceConfResource, Class<?>... entities) {
		archive = ShrinkWrap.create( JavaArchive.class, "jtastandalone.jar" );
		archive.addClasses( entities );
		archive.addAsManifestResource( persistenceConfResource, persistencePath );
		try {
			testPackage = newFile();
		}
		catch ( IOException e ) {
			throw new RuntimeException( e );
		}
		archive.as( ZipExporter.class ).exportTo( testPackage, true );
	}

	@Override
	public void before() throws Throwable {
		super.before();
		URLClassLoader classLoader = new URLClassLoader( new URL[]{ testPackage.toURL() }, originalClassLoader );
		Thread.currentThread().setContextClassLoader( classLoader );
	}

	@Override
	public void after() {
		// reset the classloader
		Thread.currentThread().setContextClassLoader( originalClassLoader );
		super.after();
	}

}
