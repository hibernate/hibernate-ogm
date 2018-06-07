/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.infinispanremote.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.ogm.backendtck.storedprocedures.Car;
import org.hibernate.ogm.datastore.infinispanremote.impl.InfinispanRemoteDatastoreProvider;
import org.hibernate.ogm.datastore.infinispanremote.test.storedprocedures.ExceptionalProcedure;
import org.hibernate.ogm.datastore.infinispanremote.test.storedprocedures.ResultSetProcedure;
import org.hibernate.ogm.datastore.infinispanremote.test.storedprocedures.SimpleValueProcedure;
import org.hibernate.ogm.util.impl.ResourceHelper;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.tasks.ServerTask;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * @author Davide D'Alto
 * @author The Viet Nguyen
 */
public class StoredProceduresDeployer {

	private static final String FOLDER_SEP = System.getProperty( "file.separator" );

	private static final String INFINISPAN_DEPLOYMENTS_DIRECTORY = "target/infinispan-server/standalone/deployments/".replaceAll( "//", FOLDER_SEP );

	// Constants for waiting for the stored procedures to be deployed
	private static final int MAX_WAIT_MILLISECONDS = 20 * 1000;
	private static final int STATE_REFRESH_MILLISECONDS = 500;
	private static final int MAX_STATE_REFRESH_ATTEMPTS =  MAX_WAIT_MILLISECONDS / STATE_REFRESH_MILLISECONDS;

	public static void deployJavaScripts(SessionFactory sessionFactory) {
		InfinispanRemoteDatastoreProvider provider = InfinispanRemoteTestHelper.getProvider( sessionFactory );
		RemoteCache<String, String> scriptCache = provider.getScriptCache();
		scriptCache.put( Car.SIMPLE_VALUE_PROC, script( "/storedprocedures/simpleValueProcedure.js" ) );
		scriptCache.put( Car.RESULT_SET_PROC, script( "/storedprocedures/resultSetProcedure.js" ) );
		scriptCache.put( "exceptionalProcedure", script( "/storedprocedures/exceptionalProcedure.js" ) );
	}

	public static void undeployJavaScripts(SessionFactory sessionFactory) {
		InfinispanRemoteDatastoreProvider provider = InfinispanRemoteTestHelper.getProvider( sessionFactory );
		RemoteCache<Object, Object> scriptCache = provider.getScriptCache();
		scriptCache.remove( Car.SIMPLE_VALUE_PROC );
		scriptCache.remove( Car.RESULT_SET_PROC );
		scriptCache.remove( "exceptionalProcedure" );
	}

	public static void deployJars() throws InterruptedException {
		JavaArchive simpleValueJar = ShrinkWrap.create( JavaArchive.class, "simple-value-procedure.jar" )
				.addClass( SimpleValueProcedure.class )
				.addAsServiceProvider( ServerTask.class, SimpleValueProcedure.class );

		JavaArchive resultSetJar = ShrinkWrap.create( JavaArchive.class, "result-set-procedure.jar" )
				.addClass( Car.class )
				.addClass( ResultSetProcedure.class )
				.addAsServiceProvider( ServerTask.class, ResultSetProcedure.class );

		JavaArchive exceptionalProcedureJar = ShrinkWrap.create( JavaArchive.class, "exceptional-procedure.jar" )
				.addClass( ExceptionalProcedure.class )
				.addAsServiceProvider( ServerTask.class, ExceptionalProcedure.class );

		deploy( simpleValueJar, exceptionalProcedureJar, resultSetJar );
	}

	private static void deploy(Archive<?>... archives) {
		for ( Archive<?> archive : archives ) {
			archive.as( ZipExporter.class )
			.exportTo( deploymentDirectory( archive.getName() ), true );
		}
		waitForArchivesDeployment( archives );
	}

	private static File deploymentDirectory(String name) {
		return new File( INFINISPAN_DEPLOYMENTS_DIRECTORY + name );
	}

	private static void waitForArchivesDeployment(Archive<?>... archives) {
		for ( int attempts = 0; attempts < MAX_STATE_REFRESH_ATTEMPTS; attempts++ ) {
			if ( hasDeployed( archives ) ) {
				return;
			}
			waitOrAbort();
		}
		throw new RuntimeException( "Stored procedures not deployed" );
	}

	private static void waitOrAbort() {
		try {
			Thread.sleep( STATE_REFRESH_MILLISECONDS );
		}
		catch (InterruptedException e) {
			throw new RuntimeException( "Interrupted while waiting for Hot Rod server to deploy the stored procedures" );
		}
	}

	private static boolean hasDeployed( Archive<?>... archives) {
		for ( Archive<?> archive : archives ) {
			if ( !Files.exists( Paths.get( INFINISPAN_DEPLOYMENTS_DIRECTORY + archive.getName() + ".deployed" ) ) ) {
				return false;
			}
		}
		return true;
	}

	private static String script(String path) {
		try {
			return ResourceHelper.readResource( StoredProceduresDeployer.class.getResource( path ) );
		}
		catch (IOException e) {
			throw new HibernateException( e );
		}
	}
}
