/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.hibernate.ogm.datastore.impl.DatastoreProviderType;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

/**
 * A JUnit 4 test runner for which allows to skip tests for single grid dialects or datastore providers by annotating
 * them with {@link SkipByGridDialect} or {@link SkipByDatastoreProvider}, respectively.
 *
 * @author Gunnar Morling
 */
public class SkippableTestRunner extends BMUnitRunner {

	public SkippableTestRunner(Class<?> klass) throws InitializationError {
		super( klass );
	}

	@Override
	public void run(RunNotifier notifier) {
		if ( isTestClassSkipped() || areAllTestMethodsSkipped() ) {
			skipTest( notifier );
		}
		else {
			super.run( notifier );
		}
	}

	@Override
	protected void runChild(final FrameworkMethod method, RunNotifier notifier) {
		if ( isTestMethodSkipped( method ) ) {
			skipTest( method, notifier );
		}
		else {
			super.runChild( method, notifier );
		}
	}

	/**
	 * Skip a whole test class.
	 */
	protected void skipTest(RunNotifier notifier) {
		notifier.fireTestIgnored( Description.createSuiteDescription( super.getTestClass().getJavaClass() ) );
	}

	/**
	 * Skip a test method.
	 */
	protected void skipTest(FrameworkMethod method, RunNotifier notifier) {
		notifier.fireTestIgnored( describeChild( method ) );
	}

	/**
	 * Whether the test class is skipped by {@link SkipByDatastoreProvider}.
	 */
	protected boolean isTestClassSkipped() {
		return isTestClassSkippedByDatastoreProvider() || isTestClassSkippedByGridDialect() || isTestClassSkippedByFiles();
	}

	private boolean isTestClassSkippedByDatastoreProvider() {
		SkipByDatastoreProvider skipByDatastoreProvider = getTestClass().getJavaClass().getAnnotation( SkipByDatastoreProvider.class );
		return skipByDatastoreProvider != null ? isSkipped( skipByDatastoreProvider ) : false;
	}

	private boolean isTestClassSkippedByGridDialect() {
		SkipByGridDialect skipByGridDialect = getTestClass().getJavaClass().getAnnotation( SkipByGridDialect.class );

		return skipByGridDialect != null ? isSkipped( skipByGridDialect ) : false;
	}

	private boolean isTestClassSkippedByFiles() {
		Class<?> testClass = getTestClass().getJavaClass();
		final Predicate<String> predicate = new ClassNameIsSkippable( testClass );
		return isSkippable( predicate );
	}

	private boolean isSkippable(final Predicate<String> predicate) {
		URL resourceAsStream = Thread.currentThread().getContextClassLoader().getResource( "skipTests" );
		if ( resourceAsStream != null ) {
			try ( Stream<String> stream = Files.lines( Paths.get( resourceAsStream.toURI() ) ) ) {
				return stream.anyMatch( predicate );
			}
			catch (Exception e) {
				throw new RuntimeException( e );
			}
		}
		return false;
	}

	private static class ClassNameIsSkippable implements Predicate<String> {

		private final Class<?> testClass;

		public ClassNameIsSkippable(Class<?> testClass) {
			this.testClass = testClass;
		}

		@Override
		public boolean test(String classNameToSkip) {
			return test( testClass, classNameToSkip );
		}

		private static boolean test(Class<?> testClass, String classNameToSkip) {
			if ( testClass.getName().equals( classNameToSkip ) ) {
				return true;
			}

			Class<?> superclass = testClass.getSuperclass();
			if ( superclass != null && superclass != Object.class ) {
				return test( superclass, classNameToSkip );
			}
			return false;
		}
	}

	/**
	 * Whether the given method is to be skipped or not, by means of the {@link SkipByDatastoreProvider} or {@link SkipByGridDialect} either given on the
	 * method itself or on the class of the test.
	 */
	protected boolean isTestMethodSkipped(final FrameworkMethod method) {
		if ( isTestMethodSkippedByDatastoreProvider( method ) || isTestMethodSkippedByGridDialect( method ) ) {
			return true;
		}

		if ( isTestMethodSkippedByFiles( method ) ) {
			return true;
		}
		return isTestClassSkipped();
	}

	private boolean isTestMethodSkippedByDatastoreProvider(FrameworkMethod method) {
		SkipByDatastoreProvider skipByDatastoreProvider = method.getAnnotation( SkipByDatastoreProvider.class );

		return skipByDatastoreProvider != null ? isSkipped( skipByDatastoreProvider ) : false;
	}

	private boolean isTestMethodSkippedByGridDialect(FrameworkMethod method) {
		SkipByGridDialect skipByGridDialect = method.getAnnotation( SkipByGridDialect.class );

		return skipByGridDialect != null ? isSkipped( skipByGridDialect ) : false;
	}

	private boolean isTestMethodSkippedByFiles(FrameworkMethod method) {
		final Predicate<String> predicate = new MethodIsSkippable( method );
		return isSkippable( predicate );
	}

	private static class MethodIsSkippable implements Predicate<String> {

		private final Class<?> testClass;
		private final String methodName;
		private final Predicate<String> classNameIsSkippable;

		public MethodIsSkippable(FrameworkMethod method) {
			this.testClass = method.getDeclaringClass();
			this.methodName = method.getName();
			this.classNameIsSkippable = new ClassNameIsSkippable( testClass );
		}

		@Override
		public boolean test(String fullMethodName) {
			int index = fullMethodName.indexOf( '#' );
			if ( index > 0 ) {
				String classToSkip = fullMethodName.substring( 0, index );
				if ( classNameIsSkippable.test( classToSkip ) ) {
					String skippableMethodName = fullMethodName.substring( index + 1 );
					if ( methodName.equals( skippableMethodName ) ) {
						return true;
					}
				}
			}
			return false;
		}
	}

	protected boolean areAllTestMethodsSkipped() {
		for ( FrameworkMethod method : getChildren() ) {
			if ( !isTestMethodSkipped( method ) ) {
				return false;
			}
		}

		return true;
	}

	private boolean isSkipped(SkipByDatastoreProvider skipByDatastoreProvider) {
		for ( DatastoreProviderType datastoreProvider : skipByDatastoreProvider.value() ) {
			if ( datastoreProvider == TestHelper.getCurrentDatastoreProviderType() ) {
				return true;
			}
		}

		return false;
	}

	private boolean isSkipped(SkipByGridDialect skipByGridDialect) {
		Class<? extends GridDialect> actualGridDialectClass = TestHelper.getCurrentGridDialectClass();

		for ( GridDialectType gridDialectType : skipByGridDialect.value() ) {
			Class<? extends GridDialect> gridDialectClass = gridDialectType.loadGridDialectClass();
			if ( isSkipped( actualGridDialectClass, gridDialectClass ) ) {
				return true;
			}
		}

		for ( Class<? extends GridDialect> gridDialectClass : skipByGridDialect.dialects() ) {
			if ( isSkipped( actualGridDialectClass, gridDialectClass ) ) {
				return true;
			}

		}
		return false;
	}

	private boolean isSkipped(Class<? extends GridDialect> actualGridDialectClass, Class<? extends GridDialect> gridDialectClass) {
		boolean skipTest = gridDialectClass != null && gridDialectClass.isAssignableFrom( actualGridDialectClass );
		return skipTest;
	}

}
