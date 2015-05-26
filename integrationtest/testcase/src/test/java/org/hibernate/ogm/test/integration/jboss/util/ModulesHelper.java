/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.jboss.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;

/**
 * Some utilities to deal with modules setup and module versions
 *
 * @author Sanne Grinovero
 */
public class ModulesHelper {

	public static void addModulesDependencyDeclaration(Archive<?> archive, String dependencies) {
		archive.add( manifest( injectVariables( dependencies ) ), "META-INF/MANIFEST.MF" );
	}

	private static Asset manifest(String dependencies) {
		String manifest = Descriptors.create( ManifestDescriptor.class )
				.attribute( "Dependencies", dependencies )
				.exportAsString();
		return new StringAsset( manifest );
	}

	private static String injectVariables(String dependencies) {
		Properties projectCompilationProperties = new Properties();
		final InputStream resourceAsStream = ModulesHelper.class.getClassLoader().getResourceAsStream( "module-versions.properties" );
		try {
			projectCompilationProperties.load( resourceAsStream );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
		finally {
			try {
				resourceAsStream.close();
			}
			catch (IOException e) {
				throw new RuntimeException( e );
			}
		}
		Set<Entry<Object,Object>> entrySet = projectCompilationProperties.entrySet();
		for ( Entry<Object,Object> entry : entrySet ) {
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			String original = dependencies;
			dependencies = dependencies.replace( "${" + key + "}", value );
			if ( ! original.equals( dependencies ) ) {
				System.out.println( "\n\n\t***\tDependency version injected: " + key + " = " + value + "\n" );
			}
		}
		return dependencies;
	}

}
