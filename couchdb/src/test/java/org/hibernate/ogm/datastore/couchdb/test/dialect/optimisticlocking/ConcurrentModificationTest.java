/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.test.dialect.optimisticlocking;

import static org.fest.assertions.Assertions.assertThat;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.Transaction;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for concurrent updates of CouchDB entities with mapped revision property.
 *
 * @author Gunnar Morling
 */
public class ConcurrentModificationTest extends OgmTestCase {

	private Session session;

	@Before
	public void createSession() {
		session = openSession();
	}

	@After
	public void deleteTestDataAndCloseSession() {
		session.clear();
		if ( session.getTransaction().isActive() ) {
			session.getTransaction().rollback();
		}
		Transaction transaction = session.beginTransaction();

		Novel novel = (Novel) session.get( Novel.class, "novel-1" );
		if ( novel != null ) {
			session.delete( novel );
		}

		Animal animal = (Animal) session.get( Animal.class, "animal-1" );
		if ( animal != null ) {
			session.delete( animal );
		}

		animal = (Animal) session.get( Animal.class, "animal-2" );
		if ( animal != null ) {
			session.delete( animal );
		}

		Zoo zoo = (Zoo) session.get( Zoo.class, "zoo-1" );
		if ( zoo != null ) {
			session.delete( zoo );
		}

		Contributor contributor = (Contributor) session.get( Contributor.class, "contributor-1" );
		if ( contributor != null ) {
			session.delete( contributor );
		}

		contributor = (Contributor) session.get( Contributor.class, "contributor-2" );
		if ( contributor != null ) {
			session.delete( contributor );
		}

		Project project = (Project) session.get( Project.class, "project-1" );
		if ( project != null ) {
			session.delete( project );
		}

		project = (Project) session.get( Project.class, "project-2" );
		if ( project != null ) {
			session.delete( project );
		}

		project = (Project) session.get( Project.class, "project-3" );
		if ( project != null ) {
			session.delete( project );
		}

		ProjectGroup projectGroup = (ProjectGroup) session.get( ProjectGroup.class, "project-group-1" );
		if ( projectGroup != null ) {
			session.delete( projectGroup );
		}

		transaction.commit();
		session.close();
	}

	@Test(expected = StaleObjectStateException.class)
	public void concurrentModificationShouldCauseException() throws Exception {
		Novel novel = createAndPersistNovel();

		String newRevision = doConcurrentUpdateToNovel();
		assertThat( newRevision ).isNotEqualTo( novel.get_rev() );

		Transaction transaction = session.beginTransaction();
		novel.setDescription( "Description 2" );
		transaction.commit();
	}

	@Test(expected = StaleObjectStateException.class)
	public void mergeAfterConcurrentModificationShouldCauseException() throws Exception {
		Novel novel = createAndPersistNovel();
		session.clear();

		doConcurrentUpdateToNovel();

		session.beginTransaction();
		novel = (Novel) session.merge( novel );
	}

	@Test(expected = StaleObjectStateException.class)
	public void updateAfterConcurrentDeletionShouldCauseException() throws Exception {
		createAndPersistNovel();
		session.clear();

		Transaction transaction = session.beginTransaction();

		Novel novel = (Novel) session.get( Novel.class, "novel-1" );
		concurrentlyDeleteNovel();
		novel.setPosition( 2 );

		transaction.commit();
	}

	@Test(expected = StaleObjectStateException.class)
	public void deletionAfterConcurrentModificationShouldCauseException() throws Exception {
		Novel novel = createAndPersistNovel();

		doConcurrentUpdateToNovel();

		Transaction transaction = session.beginTransaction();
		session.delete( novel );
		transaction.commit();
	}

	private Novel createAndPersistNovel() {
		Transaction transaction = session.beginTransaction();

		Novel novel = createNovel();

		assertThat( novel.get_rev() ).isNull();
		session.persist( novel );
		transaction.commit();
		assertThat( novel.get_rev() ).isNotNull();
		return novel;
	}

	private Novel createNovel() {
		Novel novel = new Novel();
		novel.setId( "novel-1" );
		novel.setDescription( "Description 1" );
		novel.setPosition( 1 );

		return novel;
	}

	private String doConcurrentUpdateToNovel() throws Exception {
		return Executors.newSingleThreadExecutor().submit( new Callable<String>() {

			@Override
			public String call() throws Exception {
				Session session = openSession();

				Transaction transaction = session.beginTransaction();
				final Novel novel = (Novel) session.get( Novel.class, "novel-1" );
				novel.setDescription( "Description 2" );
				transaction.commit();

				return novel.get_rev();
			}
		} ).get();
	}

	private String concurrentlyDeleteNovel() throws Exception {
		return Executors.newSingleThreadExecutor().submit( new Callable<String>() {

			@Override
			public String call() throws Exception {
				Session session = openSession();

				Transaction transaction = session.beginTransaction();
				final Novel novel = (Novel) session.get( Novel.class, "novel-1" );
				session.delete( novel );
				transaction.commit();

				return novel.get_rev();
			}
		} ).get();
	}

	@Test(expected = StaleObjectStateException.class)
	public void customColumnNameShouldBeUsableForRevisionProperty() throws Exception {
		Animal animal = createAndPersistAnimal();

		String newRevision = doConcurrentUpdateToAnimal();
		assertThat( newRevision ).isNotEqualTo( animal.getRevision() );

		Transaction transaction = session.beginTransaction();
		animal.setName( "Xavier" );
		transaction.commit();
	}

	@Test
	public void canUpdateObjectPropertyAfterUpdateOfAssociationStoredInEntity() {
		Animal animal1 = createAndPersistAnimal();
		Animal animal2 = createAndPersistAnotherAnimal();
		Zoo zoo = createAndPersistZoo( animal1 );

		Transaction transaction = session.beginTransaction();

		zoo.getAnimals().add( animal2 );
		zoo.setName( "Hilwelma" );

		transaction.commit();

		transaction = session.beginTransaction();

		zoo = (Zoo) session.get( Zoo.class, "zoo-1" );
		assertThat( zoo.getName() ).isEqualTo( "Hilwelma" );
		assertThat( zoo.getAnimals() ).onProperty( "name" ).containsOnly( "Bruno", "Berta" );

		transaction.commit();
	}

	@Test
	public void canUpdateEntityAfterUpdateOfAssociationStoredInEntityOnInverseSide() {
		Project project = createAndPersistProjectWithProjectGroup();

		Transaction transaction = session.beginTransaction();

		project.getProjectGroup().setName( "Fancy projects" );

		transaction.commit();

		transaction = session.beginTransaction();

		ProjectGroup projectGroup = (ProjectGroup) session.get( ProjectGroup.class, "project-group-1" );
		assertThat( projectGroup.getName() ).isEqualTo( "Fancy projects" );
		assertThat( projectGroup.getProjects() ).onProperty( "name" ).containsOnly( "Validator" );

		transaction.commit();
	}

	@Test
	public void canUpdateEntityAfterRemovalOfAssociationStoredInEntityOnInverseSide() {
		Project project = createAndPersistProjectWithProjectGroup();
		ProjectGroup projectGroup = project.getProjectGroup();

		Transaction transaction = session.beginTransaction();

		projectGroup.getProjects().remove( project );
		project.setProjectGroup( null );

		transaction.commit();

		transaction = session.beginTransaction();

		projectGroup.setName( "Fancy projects" );

		transaction.commit();

		transaction = session.beginTransaction();

		projectGroup = (ProjectGroup) session.get( ProjectGroup.class, "project-group-1" );
		assertThat( projectGroup.getName() ).isEqualTo( "Fancy projects" );
		assertThat( projectGroup.getProjects() ).isEmpty();

		transaction.commit();
	}

	@Test
	public void canUpdateEntityOnInverseSideOfManyToManyAssocationAfterUpdateToAssociation() throws Exception {
		// given
		Project search = createAndPersistProjectWithUser();
		Project ogm = createAndPersistProjectWithContributor();

		// when
		Transaction transaction = session.beginTransaction();
		User bob = search.getUsers().iterator().next();
		bob.getProjects().add( ogm );
		ogm.getUsers().add( bob );
		transaction.commit();

		transaction = session.beginTransaction();
		bob.setName( "Alice" );
		transaction.commit();
		session.clear();

		// then
		transaction = session.beginTransaction();
		bob = (User) session.get( User.class, "user-1" );
		assertThat( bob.getName() ).isEqualTo( "Alice" );
		assertThat( bob.getProjects() ).onProperty( "name" ).containsOnly( "OGM", "Search" );
		transaction.commit();
	}

	@Test(expected = StaleObjectStateException.class)
	public void concurrentUpdateToAssociationShouldCauseException() throws Exception {
		Animal animal = createAndPersistAnimal();
		Zoo zoo = createAndPersistZoo( animal );

		doConcurrentUpdateToTheZoosAnimals();

		Transaction transaction = session.beginTransaction();
		zoo.getAnimals().remove( zoo.getAnimals().iterator().next() );
		transaction.commit();
	}

	@Test(expected = StaleObjectStateException.class)
	public void concurrentUpdateToObjectShouldCauseExceptionWhenUpdatingAssociation() throws Exception {
		Animal animal = createAndPersistAnimal();
		Zoo zoo = createAndPersistZoo( animal );

		doConcurrentUpdateToZoo();

		Transaction transaction = session.beginTransaction();
		zoo.getAnimals().remove( zoo.getAnimals().iterator().next() );
		transaction.commit();
	}

	@Test
	public void concurrentUpdateToObjectShouldCauseNoExceptionWithAssociationExcludedFromOptimisticLocking() throws Exception {
		Project ogm = createAndPersistProjectWithContributor();

		doConcurrentUpdateToProject();

		Transaction transaction = session.beginTransaction();
		ogm.setName( "OGM!" );
		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();

		ogm = (Project) session.get( Project.class, "project-1" );
		assertThat( ogm.getName() ).isEqualTo( "OGM!" );
		assertThat( ogm.getMembers() ).onProperty( "name" ).containsOnly( "Davide", "Sanne" );

		transaction.commit();
	}

	private Animal createAndPersistAnimal() {
		Animal animal = new Animal();
		animal.setId( "animal-1" );
		animal.setName( "Bruno" );

		Transaction transaction = session.beginTransaction();
		assertThat( animal.getRevision() ).isNull();
		session.persist( animal );
		transaction.commit();
		assertThat( animal.getRevision() ).isNotNull();

		return animal;
	}

	private Animal createAndPersistAnotherAnimal() {
		return createAndPersistAnotherAnimal( session );
	}

	private Animal createAndPersistAnotherAnimal(Session session) {
		Animal animal = new Animal();
		animal.setId( "animal-2" );
		animal.setName( "Berta" );

		Transaction transaction = session.beginTransaction();
		assertThat( animal.getRevision() ).isNull();
		session.persist( animal );
		transaction.commit();
		assertThat( animal.getRevision() ).isNotNull();

		return animal;
	}

	private Zoo createAndPersistZoo(Animal animal) {
		Zoo zoo = new Zoo();
		zoo.setId( "zoo-1" );
		zoo.setName( "Bagenhecks Tierpark" );
		zoo.getAnimals().add( animal );

		Transaction transaction = session.beginTransaction();
		session.persist( zoo );
		transaction.commit();

		return zoo;
	}

	private String doConcurrentUpdateToAnimal() throws Exception {
		return Executors.newSingleThreadExecutor().submit( new Callable<String>() {

			@Override
			public String call() throws Exception {
				Session session = openSession();

				Transaction transaction = session.beginTransaction();
				final Animal animal = (Animal) session.get( Animal.class, "animal-1" );
				animal.setName( "Xavier" );
				transaction.commit();

				return animal.getRevision();
			}
		} ).get();
	}

	private String doConcurrentUpdateToTheZoosAnimals() throws Exception {
		return Executors.newSingleThreadExecutor().submit( new Callable<String>() {

			@Override
			public String call() throws Exception {
				Session session = openSession();

				Animal berta = createAndPersistAnotherAnimal( session );

				Transaction transaction = session.beginTransaction();
				final Zoo zoo = (Zoo) session.get( Zoo.class, "zoo-1" );
				zoo.getAnimals().add( berta );
				transaction.commit();

				return zoo.getRevision();
			}
		} ).get();
	}

	private String doConcurrentUpdateToZoo() throws Exception {
		return Executors.newSingleThreadExecutor().submit( new Callable<String>() {

			@Override
			public String call() throws Exception {
				Session session = openSession();
				Transaction transaction = session.beginTransaction();

				final Zoo zoo = (Zoo) session.get( Zoo.class, "zoo-1" );
				zoo.setName( "Hilwema" );

				transaction.commit();
				return zoo.getRevision();
			}
		} ).get();
	}

	private Project createAndPersistProjectWithContributor() {
		Transaction transaction = session.beginTransaction();

		Project ogm = new Project();
		ogm.setId( "project-1" );
		ogm.setName( "OGM" );
		session.persist( ogm  );

		Contributor davide = new Contributor();
		davide.setId( "contributor-1" );
		davide.setName( "Davide" );
		session.persist( davide );

		ogm.getMembers().add( davide );
		davide.getProjects().add( ogm );

		transaction.commit();
		return ogm;
	}

	private Project createAndPersistProjectWithUser() {
		Transaction transaction = session.beginTransaction();

		Project ogm = new Project();
		ogm.setId( "project-2" );
		ogm.setName( "Search" );
		session.persist( ogm  );

		User bob = new User();
		bob.setId( "user-1" );
		bob.setName( "Bob" );
		session.persist( bob );

		ogm.getUsers().add( bob );
		bob.getProjects().add( ogm );

		transaction.commit();
		return ogm;
	}

	private Project createAndPersistProjectWithProjectGroup() {
		Transaction transaction = session.beginTransaction();

		Project validator = new Project();
		validator.setId( "project-3" );
		validator.setName( "Validator" );
		session.persist( validator  );

		ProjectGroup hibernateProjects = new ProjectGroup();
		hibernateProjects.setId( "project-group-1" );
		hibernateProjects.setName( "Hibernate" );
		session.persist( hibernateProjects );

		validator.setProjectGroup( hibernateProjects );
		hibernateProjects.getProjects().add( validator );

		transaction.commit();
		return validator;
	}

	private String doConcurrentUpdateToProject() throws Exception {
		return Executors.newSingleThreadExecutor().submit( new Callable<String>() {

			@Override
			public String call() throws Exception {
				Session session = openSession();

				Transaction transaction = session.beginTransaction();
				final Project project = (Project) session.get( Project.class, "project-1" );

				Contributor sanne = new Contributor();
				sanne.setId( "contributor-2" );
				sanne.setName( "Sanne" );

				sanne.getProjects().add( project );
				project.getMembers().add( sanne );

				session.persist( sanne );

				transaction.commit();

				return project.getRevision();
			}
		} ).get();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Novel.class, Animal.class, Zoo.class, Project.class, Contributor.class, User.class, ProjectGroup.class };
	}
}
