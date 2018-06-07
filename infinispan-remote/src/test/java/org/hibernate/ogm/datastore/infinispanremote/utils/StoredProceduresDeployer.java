/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.infinispanremote.utils;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.ogm.backendtck.storedprocedures.Car;
import org.hibernate.ogm.datastore.infinispanremote.impl.InfinispanRemoteDatastoreProvider;
import org.hibernate.ogm.datastore.infinispanremote.test.storedprocedures.ExceptionalProcedure;
import org.hibernate.ogm.datastore.infinispanremote.test.storedprocedures.ResultSetProcedure;
import org.hibernate.ogm.datastore.infinispanremote.test.storedprocedures.SimpleValueProcedure;

import org.infinispan.client.hotrod.RemoteCache;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * @author The Viet Nguyen
 */
public class StoredProceduresDeployer {

	private static final String SERVER_TASK_META_INF_RESOURCE_DIRECTORY = "/storedprocedures/servertask";
	private static final String SERVER_TASK_SIMPLE_VALUE_PROCEDURE_META_INF = SERVER_TASK_META_INF_RESOURCE_DIRECTORY + "/simple-value-procedure";
	private static final String SERVER_TASK_RESULT_SET_PROCEDURE_META_INF = SERVER_TASK_META_INF_RESOURCE_DIRECTORY + "/result-set-procedure";
	private static final String SERVER_TASK_EXCEPTIONAL_PROCEDURE_META_INF = SERVER_TASK_META_INF_RESOURCE_DIRECTORY + "/exceptional-procedure";
	private static final String SERVER_TASK_META_INF_TARGET_FILE = "/META-INF/services/org.infinispan.tasks.ServerTask";
	private static final String INFINISPAN_DEPLOYMENTS_DIRECTORY = "target/infinispan-server/standalone/deployments";
	private static final String SERVER_TASK_SIMPLE_VALUE_PROCEDURE_JAR = INFINISPAN_DEPLOYMENTS_DIRECTORY + "/simple-value-procedure.jar";
	private static final String SERVER_TASK_RESULT_SET_PROCEDURE_JAR = INFINISPAN_DEPLOYMENTS_DIRECTORY + "/result-set-procedure.jar";
	private static final String SERVER_TASK_EXCEPTIONAL_PROCEDURE_JAR = INFINISPAN_DEPLOYMENTS_DIRECTORY + "/exceptional-procedure.jar";
	private static final int MAX_TEST_COUNT = 20;

	public static void deployJavaScripts(SessionFactory sessionFactory) {
		InfinispanRemoteDatastoreProvider provider = InfinispanRemoteTestHelper.getProvider( sessionFactory );
		RemoteCache<String, String> scriptCache = provider.getScriptCache();
		scriptCache.put( Car.SIMPLE_VALUE_PROC, getContent( "/storedprocedures/simpleValueProcedure.js" ) );
		scriptCache.put( Car.RESULT_SET_PROC, getContent( "/storedprocedures/resultSetProcedure.js" ) );
		scriptCache.put( "exceptionalProcedure", getContent( "/storedprocedures/exceptionalProcedure.js" ) );
	}

	public static void undeployJavaScripts(SessionFactory sessionFactory) {
		InfinispanRemoteDatastoreProvider provider = InfinispanRemoteTestHelper.getProvider( sessionFactory );
		RemoteCache<Object, Object> scriptCache = provider.getScriptCache();
		scriptCache.remove( Car.SIMPLE_VALUE_PROC );
		scriptCache.remove( Car.RESULT_SET_PROC );
		scriptCache.remove( "exceptionalProcedure" );
	}

	public static void deployJars() throws InterruptedException {
		ShrinkWrap.create( JavaArchive.class, "simple-value-procedure.jar" )
				.addClass( SimpleValueProcedure.class )
				.addAsResource( getResource( SERVER_TASK_SIMPLE_VALUE_PROCEDURE_META_INF ), SERVER_TASK_META_INF_TARGET_FILE )
				.as( ZipExporter.class )
				.exportTo( new File( SERVER_TASK_SIMPLE_VALUE_PROCEDURE_JAR ), true );
		ShrinkWrap.create( JavaArchive.class, "result-set-procedure.jar" )
				.addClass( Car.class )
				.addClass( ResultSetProcedure.class )
				.addAsResource( getResource( SERVER_TASK_RESULT_SET_PROCEDURE_META_INF ), SERVER_TASK_META_INF_TARGET_FILE )
				.as( ZipExporter.class )
				.exportTo( new File( SERVER_TASK_RESULT_SET_PROCEDURE_JAR ), true );
		ShrinkWrap.create( JavaArchive.class, "exceptional-procedure.jar" )
				.addClass( ExceptionalProcedure.class )
				.addAsResource( getResource( SERVER_TASK_EXCEPTIONAL_PROCEDURE_META_INF ), SERVER_TASK_META_INF_TARGET_FILE )
				.as( ZipExporter.class )
				.exportTo( new File( SERVER_TASK_EXCEPTIONAL_PROCEDURE_JAR ), true );
		waitForJavaStoredProcedureDeployments();
	}

	private static void waitForJavaStoredProcedureDeployments() throws InterruptedException {
		boolean deployed;
		int testNumber = 0;
		do {
			deployed = javaStoredProceduresDeployed();
			if ( !deployed ) {
				TimeUnit.SECONDS.sleep( 1 );
			}
		}
		while ( ++testNumber < MAX_TEST_COUNT && !deployed );
		if ( !deployed ) {
			throw new HibernateException( "Can not upload procedures during 20 seconds!" );
		}
	}

	private static boolean javaStoredProceduresDeployed() {
		return Files.exists( Paths.get( SERVER_TASK_SIMPLE_VALUE_PROCEDURE_JAR + ".deployed" ) )
				&& Files.exists( Paths.get( SERVER_TASK_RESULT_SET_PROCEDURE_JAR + ".deployed" ) )
				&& Files.exists( Paths.get( SERVER_TASK_EXCEPTIONAL_PROCEDURE_JAR + ".deployed" ) );
	}

	private static URL getResource(String resource) {
		return InfinispanRemoteTestHelper.class.getResource( resource );
	}

	private static String getContent(String path) {
		return getContent( InfinispanRemoteTestHelper.class.getResourceAsStream( path ) );
	}

	private static String getContent(InputStream is) {
		try ( Scanner scanner = new Scanner( is ) ) {
			return scanner.useDelimiter( "\\A" ).next();
		}
	}
}
