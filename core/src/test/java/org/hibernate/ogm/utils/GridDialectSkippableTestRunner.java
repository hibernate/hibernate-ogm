/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

/**
 * A JUnit 4 test runner for which allows to skip tests for single grid dialects by annotating them with
 * {@link SkipByGridDialect}.
 *
 * @author Gunnar Morling
 */
public class GridDialectSkippableTestRunner extends BlockJUnit4ClassRunner {

	public GridDialectSkippableTestRunner(Class<?> klass) throws InitializationError {
		super( klass );
	}

	@Override
	protected void runChild(final FrameworkMethod method, RunNotifier notifier) {
		if ( isTestMethodSkipped( method ) ) {
			notifier.fireTestIgnored( describeChild( method ) );
		}
		else {
			super.runChild( method, notifier );
		}
	}

	protected boolean isTestClassSkipped() {
		SkipByGridDialect skipByGridDialect = getTestClass().getJavaClass().getAnnotation( SkipByGridDialect.class );
		return isSkipped( skipByGridDialect );
	}

	/**
	 * Whether the given method is to be skipped or not, by means of the {@link SkipByGridDialect} either given on the
	 * method itself or on the class of the test.
	 */
	protected boolean isTestMethodSkipped(final FrameworkMethod method) {
		SkipByGridDialect skipByGridDialect = method.getAnnotation( SkipByGridDialect.class );
		if ( skipByGridDialect != null ) {
			return isSkipped( skipByGridDialect );
		}

		return isTestClassSkipped();
	}

	protected boolean areAllTestMethodsSkipped() {
		for ( FrameworkMethod method : getChildren() ) {
			if ( !isTestMethodSkipped( method ) ) {
				return false;
			}
		}

		return true;
	}

	private boolean isSkipped(SkipByGridDialect skipByGridDialect) {
		if ( skipByGridDialect != null ) {
			for ( GridDialectType gridDialectType : skipByGridDialect.value() ) {
				if ( gridDialectType.equals( TestHelper.getCurrentDialectType() ) ) {
					return true;
				}
			}
		}

		return false;
	}
}
