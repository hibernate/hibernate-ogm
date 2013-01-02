/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.test.integration.jbossas7.util;

import org.hibernate.ogm.test.integration.jbossas7.ModuleMemberRegistrationScenario;
import org.hibernate.ogm.test.integration.jbossas7.controller.MemberRegistration;
import org.hibernate.ogm.test.integration.jbossas7.model.Member;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;

public class ModuleMemberRegistrationDeployment {

	private ModuleMemberRegistrationDeployment() {
		super();
	}

	public static class Builder {
		private final WebArchive archive;

		public Builder(Class<?> clazz) {
			archive = ShrinkWrap
					.create( WebArchive.class, clazz.getSimpleName() + ".war" )
					.addClasses( clazz, Member.class, MemberRegistration.class, Resources.class, ModuleMemberRegistrationScenario.class )
					.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" );
		}

		public Builder persistenceXml(PersistenceDescriptor descriptor) {
			String persistenceXml = descriptor.exportAsString();
			archive.addAsResource( new StringAsset(persistenceXml), "META-INF/persistence.xml" );
			return this;
		}

		public Builder manifestDependencies(String dependencies) {
			archive.add( manifest(dependencies), "META-INF/MANIFEST.MF" );
			return this;
		}

		public WebArchive createDeployment() {
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
