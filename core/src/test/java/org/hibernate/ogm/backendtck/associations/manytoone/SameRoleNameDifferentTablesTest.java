/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.manytoone;

import static org.fest.assertions.Assertions.assertThat;

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
public class SameRoleNameDifferentTablesTest extends OgmTestCase {

	final VideoGame dot = new VideoGame( "The Day of the Tentacle" );
	final PlayableCharacter[] playables = { new PlayableCharacter( "Bernard" ), new PlayableCharacter( "Laverne" ), new PlayableCharacter( "Hoagie" ) };
	final NonPlayableCharacter[] nonPlayables = { new NonPlayableCharacter( "Dr. Fred Edison" ), new NonPlayableCharacter( "George Washington" ), new NonPlayableCharacter( "John Hancock" ) };

	@Before
	public void before() {
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
	public void testPlayableCharacters() {
		try ( OgmSession session = openSession() ) {
			Transaction tx = session.beginTransaction();
			final VideoGame videoGame = session.load( VideoGame.class, dot.getTitle() );

			final Set<PlayableCharacter> characters = videoGame.getPlayableCharacters();
			assertThat( characters ).containsOnly( playables );

			tx.commit();
		}
	}

	@Test
	public void testNonPlayableCharacters() {
		try ( OgmSession session = openSession() ) {
			Transaction tx = session.beginTransaction();
			final VideoGame videoGame = session.load( VideoGame.class, dot.getTitle() );

			final Set<NonPlayableCharacter> characters = videoGame.getNonPlayableCharacters();
			assertThat( characters ).containsOnly( nonPlayables );

			tx.commit();
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
