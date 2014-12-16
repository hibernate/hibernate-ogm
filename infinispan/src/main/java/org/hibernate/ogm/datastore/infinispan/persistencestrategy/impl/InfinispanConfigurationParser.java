/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.persistencestrategy.impl;

import java.io.InputStream;

import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;

/**
 *
 * @author Sanne Grinovero
 */
public class InfinispanConfigurationParser {

	private final ParserRegistry configurationParser;
	private final ClassLoader ispnClassLoadr;

	public InfinispanConfigurationParser() {
		ispnClassLoadr = ParserRegistry.class.getClassLoader();
		configurationParser = new ParserRegistry( ispnClassLoadr );
	}

	ConfigurationBuilderHolder parseFile(InputStream configuration) {
		//Infinispan requires the context ClassLoader to have full visibility on all
		//its components and eventual extension points even *during* configuration parsing.
		final Thread currentThread = Thread.currentThread();
		final ClassLoader originalContextClassLoader = currentThread.getContextClassLoader();
		try {
			currentThread.setContextClassLoader( ispnClassLoadr );
			ConfigurationBuilderHolder builderHolder = configurationParser.parse( configuration );
			//Workaround Infinispan's ClassLoader strategies to bend to our will:
			fixClassLoaders( builderHolder );
			return builderHolder;
		}
		finally {
			currentThread.setContextClassLoader( originalContextClassLoader );
		}
	}

	private void fixClassLoaders(ConfigurationBuilderHolder builderHolder) {
		//Global section:
		builderHolder.getGlobalConfigurationBuilder().classLoader( ispnClassLoadr );
	}
}
