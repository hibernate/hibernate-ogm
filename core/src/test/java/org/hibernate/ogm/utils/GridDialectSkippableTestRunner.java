/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
