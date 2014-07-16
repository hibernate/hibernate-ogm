/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Use this test Rule to skip tests when the configured GridDialect doesn't
 * support transactions: if rollback capabilities are required or different
 * transactions should be isolated.
 *
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2011 Red Hat Inc.
 */
public class RequiresTransactionalCapabilitiesRule implements TestRule {

	private static final Log log = LoggerFactory.make();

	@Override
	public Statement apply(final Statement base, final Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				if ( TestHelper.backendSupportsTransactions() ) {
					base.evaluate();
				}
				else {
					log.infof( "Skipping test $s as the current GridDialect doesn't support transactions", description.getMethodName() );
				}
			}
		};
	}

}
