/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.utils;

import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.runner.RunWith;

/**
 * Helper class allowing you to run all or any specified subset of test available on the classpath.
 * This method is for example useful to run all or parts of the <i>backendtck</i>.
 *
 * @author Victor Kadachigov
 */
@RunWith(ClasspathSuite.class)
@ClasspathSuite.ClassnameFilters({

	// todo
//	"org.hibernate.ogm.backendtck.associations.collection.unidirectional.CollectionUnidirectionalTest",
//	"org.hibernate.ogm.backendtck.associations.collection.manytomany.ManyToManyExtraTest",
//	"org.hibernate.ogm.backendtck.compensation.CompensationSpiTest",					// -appliedOperationsPassedToErrorHandlerAreSeparatedByTransaction

	// failed

//	"org.hibernate.ogm.backendtck.associations.collection.unidirectional.*",
//	"org.hibernate.ogm.backendtck.loader.LoaderFromTupleTest"
//	"org.hibernate.ogm.backendtck.queries.CompositeIdQueriesTest",
//	"org.hibernate.ogm.backendtck.queries.QueriesWithEmbeddedTest",
//	"org.hibernate.ogm.backendtck.queries.SimpleQueriesTest",   // e:12, f:1
//	"org.hibernate.ogm.backendtck.loader.LoaderFromTupleTest",
//	"org.hibernate.ogm.backendtck.type.converter.JpaAttributeConverterTest"
//	"org.hibernate.ogm.backendtck.compensation.CompensationSpiTest",					// -appliedOperationsPassedToErrorHandlerAreSeparatedByTransaction

	// passed

//	"org.hibernate.ogm.backendtck.id.*",
//	"org.hibernate.ogm.backendtck.simpleentity.*",
//	"org.hibernate.ogm.backendtck.associations.collection.manytomany.ManyToManyTest",
//	"org.hibernate.ogm.backendtck.associations.manytoone.*",
//	"org.hibernate.ogm.backendtck.associations.onetoone.*",
//	"org.hibernate.ogm.backendtck.associations.compositeid.*",
//	"org.hibernate.ogm.backendtck.associations.collection.types.*",						// -MapContentsStoredInSeparateDocumentTest
//	"org.hibernate.ogm.backendtck.batchfetching.*",
//	"org.hibernate.ogm.backendtck.compensation.CompensationSpiJpaTest",
//	"org.hibernate.ogm.backendtck.embeddable.*",
//	"org.hibernate.ogm.backendtck.hibernatecore.*",
//	"org.hibernate.ogm.backendtck.hsearch.*"
//	"org.hibernate.ogm.backendtck.inheritance.*",
//	"org.hibernate.ogm.backendtck.innertypes.*",
//	"org.hibernate.ogm.backendtck.jpa.*",
//	"org.hibernate.ogm.backendtck.queries.JpaQueriesTest",
//	"org.hibernate.ogm.backendtck.queries.QueryUpdateTest",
//	"org.hibernate.ogm.backendtck.queries.parameters.QueryWithParametersTest",
//	"org.hibernate.ogm.backendtck.type.BuiltInTypeTest",
//	"org.hibernate.ogm.backendtck.type.descriptor.CalendarTimeZoneDateTimeTypeDescriptorTest",

})
public class IgniteBackendTckHelper {
}
