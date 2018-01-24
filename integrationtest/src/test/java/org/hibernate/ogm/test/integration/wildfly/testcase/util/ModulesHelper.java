/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.wildfly.testcase.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
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

	private static String hibernateOgmVersion = null;
	private static String hibernateOgmModuleSlot = null;

	private static synchronized String getModuleSlotString() {
		if ( hibernateOgmModuleSlot == null ) {
			//This variable is computed from the project version, as the Maven build plugin helper
			//is otherwise not available for when running integration tests from the IDE
			String versionHibernateSearch = getDependencyVersionHibernateOGM();
			String[] split = versionHibernateSearch.split( "\\." );
			hibernateOgmModuleSlot = split[0] + '.' + split[1];
		}
		return hibernateOgmModuleSlot;
	}

	private static synchronized String getDependencyVersionHibernateOGM() {
		if ( hibernateOgmVersion == null ) {
			hibernateOgmVersion = injectVariablesFromProperties( "${dependency.version.HibernateOgm}" );
		}
		return hibernateOgmVersion;
	}

	public static void addModulesDependencyDeclaration(Archive<?> archive, String dependencies) {
		archive.add( manifest( injectVariables( dependencies ) ), "META-INF/MANIFEST.MF" );
	}

	private static Asset manifest(String dependencies) {
		String manifest = Descriptors.create( ManifestDescriptor.class )
				.attribute( "Dependencies", dependencies )
				.exportAsString();
		return new StringAsset( manifest );
	}

	public static String injectVariables(String dependencies) {
		String variablesFromProperties = injectVariablesFromProperties( dependencies );
		//The OGM module slot is "hardcoded" as a special case:
		return applyPropertyReplacement( variablesFromProperties, "hibernate-ogm.module.slot", getModuleSlotString() );
	}

	private static String injectVariablesFromProperties(String dependencies) {
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
			dependencies = applyPropertyReplacement( dependencies, key, value );
		}
		return dependencies;
	}

	private static String applyPropertyReplacement(String template, String key, String value) {
		String replaced = template.replace( "${" + key + "}", value );
		reportVariableInjectionIfDifferent( template, replaced, key, value );
		return replaced;
	}

	private static void reportVariableInjectionIfDifferent(String originalString, String updatedString, String propertyKey, String propertyValue) {
		if ( ! originalString.equals( updatedString ) ) {
			System.out.println( "\n\t***\tDependency version injected: " + propertyKey + " = " + propertyValue );
		}
	}

	/**
	 * Loads a resource from classpath, interpret it as a String and replace all properties.
	 */
	public static String loadResourceInjectingVariables(String resourceName) {
		final InputStream resourceAsStream = ModulesHelper.class.getClassLoader().getResourceAsStream( resourceName );
		final String readString;
		try {
			readString = IOUtils.toString( resourceAsStream );
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
		return injectVariables( readString );
	}

}
