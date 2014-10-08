/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

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
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Sanne Grinovero
 */
public class PackagingRule extends TemporaryFolder {

	protected static ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

	private static final ArchivePath persistencePath = ArchivePaths.create( "persistence.xml" );
	private final JavaArchive archive;
	private final File testPackage;

	public PackagingRule(String persistenceConfResource, Class<?>... entities) {
		try {
			create();

			archive = ShrinkWrap.create( JavaArchive.class, "jtastandalone.jar" );
			archive.addClasses( entities );
			archive.addAsManifestResource( persistenceConfResource, persistencePath );

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
