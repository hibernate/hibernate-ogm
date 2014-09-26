/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.jboss.util;

import org.hibernate.ogm.test.integration.jboss.ModuleMemberRegistrationScenario;
import org.hibernate.ogm.test.integration.jboss.controller.MemberRegistration;
import org.hibernate.ogm.test.integration.jboss.controller.MemberRegistrationWithJta;
import org.hibernate.ogm.test.integration.jboss.controller.MemberRegistrationWithResourceLocal;
import org.hibernate.ogm.test.integration.jboss.controller.RegistrationExecutor;
import org.hibernate.ogm.test.integration.jboss.model.Address;
import org.hibernate.ogm.test.integration.jboss.model.Member;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceUnitTransactionType;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;

/**
 * Creates a commeon archive that can be customized and deployed when running integration tests.
 * <p>
 * Different classes are going to be used if the persistence unit transaction type is set to RESOURCE_LOCAL instead of
 * JTA.
 *
 * @author Davide D'Alto
 */
public class ModuleMemberRegistrationDeployment {

	private ModuleMemberRegistrationDeployment() {
		super();
	}

	public static class Builder {
		private final WebArchive archive;
		private boolean resourceLocal = true;

		public Builder(Class<?> clazz) {
			archive = ShrinkWrap
					.create( WebArchive.class, clazz.getSimpleName() + ".war" )
					.addClasses( clazz, MemberRegistration.class, RegistrationExecutor.class, Member.class, Address.class, ModuleMemberRegistrationScenario.class )
					.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" );
		}

		public Builder persistenceXml(PersistenceDescriptor descriptor) {
			resourceLocal = descriptor.getOrCreatePersistenceUnit().getTransactionType() == PersistenceUnitTransactionType._RESOURCE_LOCAL;
			String persistenceXml = descriptor.exportAsString();
			archive.addAsResource( new StringAsset( persistenceXml ), "META-INF/persistence.xml" );
			return this;
		}

		public Builder manifestDependencies(String dependencies) {
			archive.add( manifest( dependencies ), "META-INF/MANIFEST.MF" );
			return this;
		}

		public Builder addClasses(Class<?> clazz, Class<?>...furtherClasses) {
			archive.addClass( clazz );
			if ( furtherClasses != null ) {
				archive.addClasses( furtherClasses );
			}
			return this;
		}

		public Builder addAsWebInfResource(String resourceName) {
			archive.addAsWebInfResource( resourceName );
			return this;
		}

		public WebArchive createDeployment() {
			if (resourceLocal) {
				archive.addClasses( MemberRegistrationWithResourceLocal.class );
			}
			else {
				archive.addClasses( MemberRegistrationWithJta.class, JtaResources.class );
			}
			return archive;
		}

		private static Asset manifest(String dependencies) {
			String manifest = Descriptors.create( ManifestDescriptor.class )
					.attribute( "Dependencies", dependencies )
					.exportAsString();
			return new StringAsset( manifest );
		}
	}

}
