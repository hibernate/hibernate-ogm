/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

import java.lang.annotation.Annotation;

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
		SkipByGridDialect skipByGridDialect = getSkipByGridDialect( method );

		if ( skipByGridDialect != null ) {
			for ( GridDialectType gridDialectType : skipByGridDialect.value() ) {
				if ( gridDialectType.equals( TestHelper.getCurrentDialectType() ) ) {
					notifier.fireTestIgnored( describeChild( method ) );
					return;
				}
			}
		}

		super.runChild( method, notifier );
	}

	private SkipByGridDialect getSkipByGridDialect(final FrameworkMethod method) {
		//try method first
		SkipByGridDialect skipByGridDialect = method.getAnnotation( SkipByGridDialect.class );

		//then type
		if ( skipByGridDialect == null ) {
			for ( Annotation annotation : getTestClass().getAnnotations() ) {
				if ( annotation.annotationType() == SkipByGridDialect.class ) {
					skipByGridDialect = (SkipByGridDialect) annotation;
				}
			}
		}

		return skipByGridDialect;
	}
}
