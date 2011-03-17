package org.hibernate.ogm.test.utils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.rules.ExternalResource;
import org.junit.rules.MethodRule;

import static org.fest.assertions.Assertions.assertThat;

/**
 * test case useful when one want to write a test relying on an archive (like a JPA archive)
 *
 * @author Hardy Ferentschik
 */
public class PackagingRule extends ExternalResource implements MethodRule {
	protected static ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
	protected static ClassLoader bundleClassLoader;
	protected static File targetDir;

	public File getTargetDir() {
		return targetDir;
	}

	static {
		// get a URL reference to something we now is part of the classpath (us)
		URL myUrl = originalClassLoader.getResource(
				PackagingRule.class.getName().replace( '.', '/' ) + ".class"
		);
		File myPath = new File( myUrl.getFile() );
		// navigate back to '/target'
		targetDir = myPath
				.getParentFile()  // target/classes/org/hibernate/ogm/test/utils
				.getParentFile()  // target/classes/org/hibernate/ejb/test
				.getParentFile()  // target/classes/org/hibernate/ejb
				.getParentFile()  // target/classes/org/hibernate
				.getParentFile()  // target/classes/org
				.getParentFile()  // target/classes/
				.getParentFile(); // target
		File testPackagesDir = new File( targetDir, "bundles" );
		try {
			bundleClassLoader = new URLClassLoader( new URL[] { testPackagesDir.toURL() }, originalClassLoader );
		}
		catch ( MalformedURLException e ) {
			assertThat( true ).as( "Unable to build custom class loader" ).isFalse();
		}
		targetDir = new File( targetDir, "packages" );
		targetDir.mkdirs();
	}

	@Override
	public void before() {
		// add the bundle class loader in order for ShrinkWrap to build the test package
		Thread.currentThread().setContextClassLoader( bundleClassLoader );
	}

	@Override
	public void after() {
		// reset the classloader
		Thread.currentThread().setContextClassLoader( originalClassLoader );
	}

	public void addPackageToClasspath(File... files) throws MalformedURLException {
		List<URL> urlList = new ArrayList<URL>();
		for ( File file : files ) {
			urlList.add( file.toURL() );
		}
		URLClassLoader classLoader = new URLClassLoader(
				urlList.toArray( new URL[urlList.size()] ), originalClassLoader
		);
		Thread.currentThread().setContextClassLoader( classLoader );
	}

	public void addPackageToClasspath(URL... urls) throws MalformedURLException {
		List<URL> urlList = new ArrayList<URL>();
		urlList.addAll( Arrays.asList( urls ) );
		URLClassLoader classLoader = new URLClassLoader(
				urlList.toArray( new URL[urlList.size()] ), originalClassLoader
		);
		Thread.currentThread().setContextClassLoader( classLoader );
	}
}
