package org.hibernate.ogm.test.simpleentity;

import org.hibernate.Session;

/**
 * @author Emmanuel Bernard
 */
public class BootstrapTest extends OgmTestCase {

	public void testBootstrap() throws Exception {
		final Session session = openSession();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Hypothesis.class
		};
	}
}
