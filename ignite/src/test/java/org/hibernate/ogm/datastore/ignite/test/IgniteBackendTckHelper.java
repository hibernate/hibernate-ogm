/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.test;

import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.runner.RunWith;

/**
 * Helper class allowing you to run all or any specified subset of test available on the classpath.
 *
 * This method is for example useful to run all or parts of the <i>backendtck</i>.
 *
 * @author Hardy Ferentschik
 */
@RunWith(ClasspathSuite.class)
@ClasspathSuite.ClassnameFilters({
	"org.hibernate.ogm.backendtck.associations.collection.types.ListTest", 				// passed -testOrderedListAndCompositeId
//	"org.hibernate.ogm.backendtck.associations.collection.types.MapContentsStoredInSeparateDocumentTest",
//	"org.hibernate.ogm.backendtck.associations.collection.types.MapTest",
//	"org.hibernate.ogm.backendtck.associations.collection.unidirectional.CollectionUnidirectionalTest",
	"org.hibernate.ogm.backendtck.associations.manytoone.ManyToOneTest",    			// passed -testDefaultBiDirManyToOneCompositeKeyTest
	"org.hibernate.ogm.backendtck.associations.manytoone.ManyToOneExtraTest", 			// passed
	"org.hibernate.ogm.backendtck.associations.onetoone.OneToOneTest",					// passed
//	"org.hibernate.ogm.backendtck.id.AutoIdGeneratorTest",
//	"org.hibernate.ogm.backendtck.id.AutoIdGeneratorWithSessionTest",
//	"org.hibernate.ogm.backendtck.id.CompositeIdTest", //vk: ManyToOne
//	"org.hibernate.ogm.backendtck.id.DuplicateIdDetectionTest",
//	"org.hibernate.ogm.backendtck.id.IdentityIdGeneratorTest",
//	"org.hibernate.ogm.backendtck.id.SequenceIdGeneratorTest",
//	"org.hibernate.ogm.backendtck.id.TableIdGeneratorTest",
//	"org.hibernate.ogm.datastore.ignite.test.IgniteTest",
//	"org.hibernate.ogm.backendtck.compensation.CompensationSpiJpaTest",
//	"org.hibernate.ogm.backendtck.compensation.CompensationSpiTest",
//	"org.hibernate.ogm.backendtck.embeddable.EmbeddableExtraTest",
//	"org.hibernate.ogm.backendtck.hibernatecore.HibernateCoreAPIWrappingTest",
//	"org.hibernate.ogm.backendtck.hibernatecore.JNDIReferenceTest",
//	"org.hibernate.ogm.backendtck.hsearch.HibernateSearchAtopOgmTest",
//	"org.hibernate.ogm.backendtck.hsearch.SearchOnStandaloneOGMTest",
//	"org.hibernate.ogm.backendtck.id.sharedpk.SharedPrimaryKeyTest",
//	"org.hibernate.ogm.backendtck.inheritance.JPAPolymorphicFindTest",
//	"org.hibernate.ogm.backendtck.inheritance.JPATablePerClassFindTest",
//	"org.hibernate.ogm.backendtck.innertypes.InnerClassFindTest",
//	"org.hibernate.ogm.backendtck.jpa.JPAAPIWrappingTest",
//	"org.hibernate.ogm.backendtck.jpa.JPAJTATest",
//	"org.hibernate.ogm.backendtck.jpa.JPAResourceLocalTest",
//	"org.hibernate.ogm.backendtck.jpa.JPAStandaloneORMAndOGMTest",
//	"org.hibernate.ogm.backendtck.loader.LoaderFromTupleTest",
//	"org.hibernate.ogm.backendtck.massindex.SimpleEntityMassIndexingTest",
//	"org.hibernate.ogm.backendtck.queries.JpaQueriesTest",
//	"org.hibernate.ogm.backendtck.queries.CompositeIdQueriesTest",
//	"org.hibernate.ogm.backendtck.queries.QueriesWithEmbeddedTest",
//	"org.hibernate.ogm.backendtck.queries.QueryUpdateTest",
//	"org.hibernate.ogm.backendtck.queries.SimpleQueriesTest",
//	"org.hibernate.ogm.backendtck.queries.parameters.QueryWithParametersTest",
//	"org.hibernate.ogm.backendtck.simpleentity.InheritanceTest",
//	"org.hibernate.ogm.backendtck.simpleentity.NullableFieldValueTest",
//	"org.hibernate.ogm.backendtck.simpleentity.CRUDTest",
//	"org.hibernate.ogm.backendtck.type.BuiltInTypeTest",
//	"org.hibernate.ogm.backendtck.type.descriptor.CalendarTimeZoneDateTimeTypeDescriptorTest",
//	"org.hibernate.ogm.backendtck.associations.collection.manytomany.ManyToManyTest"
})
public class IgniteBackendTckHelper {
}
