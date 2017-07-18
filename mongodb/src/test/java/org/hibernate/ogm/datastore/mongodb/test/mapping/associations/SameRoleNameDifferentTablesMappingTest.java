/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.mapping.associations;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.datastore.mongodb.utils.MongoDBTestHelper.assertDocument;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * In this use case two different entities have the same association with a third entity.
 * <p>
 * This means that the role name is the same but the tables names are different.
 *
 * @author Davide D'Alto
 */
public class SameRoleNameDifferentTablesMappingTest extends OgmTestCase {

	final VideoGame dot = new VideoGame( "The Day of the Tentacle" );
	final PlayableCharacter[] playables = { new PlayableCharacter( "Bernard" ), new PlayableCharacter( "Laverne" ), new PlayableCharacter( "Hoagie" ) };
	final NonPlayableCharacter[] nonPlayables = { new NonPlayableCharacter( "Dr Fred Edison" ), new NonPlayableCharacter( "George Washington" ), new NonPlayableCharacter( "John Hancock" ) };

	@Before
	public void persistEntities() {
		try ( OgmSession session = openSession() ) {
			Transaction tx = session.beginTransaction();
			session.persist( dot );

			for ( NonPlayableCharacter character : nonPlayables ) {
				dot.addCharacter( character );
				session.persist( character );
			}

			for ( PlayableCharacter character : playables ) {
				dot.addCharacter( character );
				session.persist( character );
			}

			tx.commit();
		}
	}

	@After
	public void deleteEntities() {
		try ( OgmSession session = openSession() ) {
			Transaction tx = session.beginTransaction();

			for ( NonPlayableCharacter character : nonPlayables ) {
				session.delete( character );
			}

			for ( PlayableCharacter character : playables ) {
				session.delete( character );
			}

			session.delete( dot );

			tx.commit();
		}

		checkCleanCache();
	}

	@Test
	public void testDeleteAssociation() {
		// The character removed from the association
		PlayableCharacter notAssociated = playables[2];
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			VideoGame videoGame = session.get( VideoGame.class, dot.getTitle() );

			// Delete association
			PlayableCharacter character = findCharacter( notAssociated, videoGame );
			character.setVideoGame( null );
			assertThat( videoGame.getPlayableCharacters().remove( character ) ).isTrue();

			transaction.commit();
		}

		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			assertDocument(
					session.getSessionFactory(),
					// collection
					"VideoGame",
					// query
					"{ '_id' : '" + dot.getTitle() + "' }",
					// projection
					null,
					// expected
					"{ " +
							"'_id' : '" + dot.getTitle() + "', " +
							"'playableCharacters' : [" +
								"'" + playables[0].getId() + "', " +
								"'" + playables[1].getId() + "' " +
							"], " +
							"'nonPlayableCharacters' : [" +
								"'" + nonPlayables[0].getId() + "', " +
								"'" + nonPlayables[1].getId() + "', " +
								"'" + nonPlayables[2].getId() + "' " +
							"]" +
					"}"
			);

			for ( NonPlayableCharacter nonPlayableCharacter : nonPlayables ) {
				assertDocument(
						session.getSessionFactory(),
						// collection
						"NonPlayableCharacter",
						// query
						"{ '_id' : '" + nonPlayableCharacter.getId() + "' }",
						// projection
						null,
						// expected
						"{ " +
							"'_id' : '" + nonPlayableCharacter.getId() + "', " +
							"'name' : '" + nonPlayableCharacter.getName() + "', " +
							"'videoGame_title' : '" + nonPlayableCharacter.getVideoGame().getTitle() + "', " +
						"}"
				);
			}

			for ( PlayableCharacter playableCharacter : playables ) {
				String videoGameField = ", 'videoGame_title' : '" + dot.getTitle() + "', ";
				if ( playableCharacter.equals( notAssociated ) ) {
					videoGameField = "";
				}
				assertDocument(
						session.getSessionFactory(),
						// collection
						"PlayableCharacter",
						// query
						"{ '_id' : '" + playableCharacter.getId() + "' }",
						// projection
						null,
						// expected
						"{ " +
							"'_id' : '" + playableCharacter.getId() + "', " +
							"'name' : '" + playableCharacter.getName() + "'" +
							videoGameField +
						"}"
				);
			}

			transaction.commit();
		}
	}

	private PlayableCharacter findCharacter(PlayableCharacter notAssociated, VideoGame videoGame) {
		Set<PlayableCharacter> playableCharacters = videoGame.getPlayableCharacters();
		for ( PlayableCharacter playableCharacter : playableCharacters ) {
			if ( playableCharacter.equals( notAssociated ) ) {
				return playableCharacter;
			}
		}
		return null;
	}

	@Test
	public void testDocuments() {
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			assertDocument(
					session.getSessionFactory(),
					// collection
					"VideoGame",
					// query
					"{ '_id' : '" + dot.getTitle() + "' }",
					// projection
					null,
					// expected
					"{ " +
						"'_id' : '" + dot.getTitle() + "', " +
						"'playableCharacters' : [" +
						"'" + playables[0].getId() + "', " +
						"'" + playables[1].getId() + "', " +
						"'" + playables[2].getId() + "' " +
						"], " +
						"'nonPlayableCharacters' : [" +
						"'" + nonPlayables[0].getId() + "', " +
						"'" + nonPlayables[1].getId() + "', " +
						"'" + nonPlayables[2].getId() + "' " +
						"]" +
					"}"
			);

			for ( NonPlayableCharacter nonPlayableCharacter : nonPlayables ) {
				assertDocument(
						session.getSessionFactory(),
						// collection
						"NonPlayableCharacter",
						// query
						"{ '_id' : '" + nonPlayableCharacter.getId() + "' }",
						// projection
						null,
						// expected
						"{ " +
							"'_id' : '" + nonPlayableCharacter.getId() + "', " +
							"'name' : '" + nonPlayableCharacter.getName() + "', " +
							"'videoGame_title' : '" + nonPlayableCharacter.getVideoGame().getTitle() + "', " +
						"}"
				);
			}

			for ( PlayableCharacter playableCharacter : playables ) {
				assertDocument(
						session.getSessionFactory(),
						// collection
						"PlayableCharacter",
						// query
						"{ '_id' : '" + playableCharacter.getId() + "' }",
						// projection
						null,
						// expected
						"{ " +
							"'_id' : '" + playableCharacter.getId() + "', " +
							"'name' : '" + playableCharacter.getName() + "', " +
							"'videoGame_title' : '" + playableCharacter.getVideoGame().getTitle() + "', " +
						"}"
				);
			}

			transaction.commit();
		}
	}

	@Test
	public void testDeleteEntity() {
		PlayableCharacter deleted = playables[1];
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();
			session.delete( deleted );
			transaction.commit();
		}

		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			assertDocument(
					session.getSessionFactory(),
					// collection
					"VideoGame",
					// query
					"{ '_id' : '" + dot.getTitle() + "' }",
					// projection
					null,
					// expected
					"{ " +
						"'_id' : '" + dot.getTitle() + "', " +
						"'playableCharacters' : [" +
						"'" + playables[0].getId() + "', " +
						"'" + playables[2].getId() + "' " +
						"], " +
						"'nonPlayableCharacters' : [" +
						"'" + nonPlayables[0].getId() + "', " +
						"'" + nonPlayables[1].getId() + "', " +
						"'" + nonPlayables[2].getId() + "' " +
						"]" +
					"}"
			);

			for ( NonPlayableCharacter nonPlayableCharacter : nonPlayables ) {
				assertDocument(
						session.getSessionFactory(),
						// collection
						"NonPlayableCharacter",
						// query
						"{ '_id' : '" + nonPlayableCharacter.getId() + "' }",
						// projection
						null,
						// expected
						"{ " +
							"'_id' : '" + nonPlayableCharacter.getId() + "', " +
							"'name' : '" + nonPlayableCharacter.getName() + "', " +
							"'videoGame_title' : '" + nonPlayableCharacter.getVideoGame().getTitle() + "', " +
						"}"
				);
			}

			for ( PlayableCharacter playableCharacter : playables ) {
				try {
					assertDocument(
							session.getSessionFactory(),
							// collection
							"PlayableCharacter",
							// query
							"{ '_id' : '" + playableCharacter.getId() + "' }",
							// projection
							null,
							// expected
							"{ " +
								"'_id' : '" + playableCharacter.getId() + "', " +
								"'name' : '" + playableCharacter.getName() + "', " +
								"'videoGame_title' : '" + playableCharacter.getVideoGame().getTitle() + "', " +
							"}" );
				}
				catch (AssertionError e) {
					assertThat( playableCharacter ).isEqualTo( deleted );
				}
			}

			transaction.commit();
		}
	}

	@Entity
	@Table(name = "VideoGame")
	public static class VideoGame {

		@Id
		private String title;

		@OneToMany(mappedBy = "videoGame")
		private Set<PlayableCharacter> playableCharacters = new HashSet<>();

		@OneToMany(mappedBy = "videoGame")
		private Set<NonPlayableCharacter> nonPlayableCharacters = new HashSet<>();

		public VideoGame() {
		}

		public VideoGame(String title) {
			this.title = title;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String name) {
			this.title = name;
		}

		public void addCharacter(PlayableCharacter character) {
			this.playableCharacters.add( character );
			character.setVideoGame( this );
		}

		public void addCharacter(NonPlayableCharacter character) {
			this.nonPlayableCharacters.add( character );
			character.setVideoGame( this );
		}

		public Set<PlayableCharacter> getPlayableCharacters() {
			return playableCharacters;
		}

		public void setPlayableCharacters(Set<PlayableCharacter> playableCharacters) {
			this.playableCharacters = playableCharacters;
		}

		public Set<NonPlayableCharacter> getNonPlayableCharacters() {
			return nonPlayableCharacters;
		}

		public void setNonPlayableCharacters(Set<NonPlayableCharacter> nonPlayableCharacters) {
			this.nonPlayableCharacters = nonPlayableCharacters;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ( ( title == null ) ? 0 : title.hashCode() );
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( getClass() != obj.getClass() ) {
				return false;
			}
			VideoGame other = (VideoGame) obj;
			if ( title == null ) {
				if ( other.title != null ) {
					return false;
				}
			}
			else if ( !title.equals( other.title ) ) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return "[" + title + ", playableCharacters=" + playableCharacters + ", nonPlayableCharacters=" + nonPlayableCharacters + "]";
		}
	}

	private static String id(String name) {
		return name.toUpperCase().replace( " ", "" );
	}

	@Entity
	@Table(name = "PlayableCharacter")
	public static class PlayableCharacter {

		private String id;

		private String name;

		private VideoGame videoGame;

		public PlayableCharacter() {
		}

		public PlayableCharacter(String name) {
			this.id = id( name );
			this.name = name;
		}

		@Id
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@ManyToOne
		public VideoGame getVideoGame() {
			return videoGame;
		}

		public void setVideoGame(VideoGame videoGame) {
			this.videoGame = videoGame;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( getClass() != obj.getClass() ) {
				return false;
			}
			PlayableCharacter other = (PlayableCharacter) obj;
			if ( name == null ) {
				if ( other.name != null ) {
					return false;
				}
			}
			else if ( !name.equals( other.name ) ) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	@Entity
	@Table(name = "NonPlayableCharacter")
	public static class NonPlayableCharacter {

		@Id
		private String id;

		private String name;

		private VideoGame videoGame;

		public NonPlayableCharacter() {
		}

		public NonPlayableCharacter(String name) {
			this.id = id( name );
			this.name = name;
		}

		@Id
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@ManyToOne
		public VideoGame getVideoGame() {
			return videoGame;
		}

		public void setVideoGame(VideoGame videoGame) {
			this.videoGame = videoGame;
		}

		@Override
		public String toString() {
			return name;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( getClass() != obj.getClass() ) {
				return false;
			}
			NonPlayableCharacter other = (NonPlayableCharacter) obj;
			if ( name == null ) {
				if ( other.name != null ) {
					return false;
				}
			}
			else if ( !name.equals( other.name ) ) {
				return false;
			}
			return true;
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ PlayableCharacter.class, NonPlayableCharacter.class, VideoGame.class };
	}
}
