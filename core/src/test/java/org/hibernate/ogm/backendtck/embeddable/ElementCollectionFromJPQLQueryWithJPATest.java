/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.embeddable;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.GridDialectType.INFINISPAN_REMOTE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.hibernate.search.annotations.Indexed;
import org.junit.Test;

@SkipByGridDialect(value = { INFINISPAN_REMOTE }, comment = "Embedded collection without primary key, not supported")
@TestForIssue(jiraKey = "OGM-1420")
public class ElementCollectionFromJPQLQueryWithJPATest extends OgmJpaTestCase {

	@Test
	public void testQueryWithoutResultType() {
		QtyContents content1 = new QtyContents( "average1", "quantity1" );
		QtyContents content2 = new QtyContents( "average2", "quantity2" );
		FoodsCosmeticsMedicines medicine = new FoodsCosmeticsMedicines( "medicine" );
		medicine.getQtyContentsList().add( content1 );
		medicine.getQtyContentsList().add( content2 );

		inTransaction( em -> {
			em.persist( medicine );
		} );

		inTransaction( em -> {
			@SuppressWarnings("unchecked")
			List<FoodsCosmeticsMedicines> loaded = em.createQuery( "FROM FoodsCosmeticsMedicines" ).getResultList();

			assertThat( loaded ).containsOnly( medicine );
			assertThat( loaded.get( 0 ).getQtyContentsList() ).containsOnly( content1, content2 );
		} );

		inTransaction( em -> {
			em.remove( em.find( FoodsCosmeticsMedicines.class, medicine.getName() ) );
		} );
	}

	@Test
	public void testQueryWithResultType() {
		QtyContents content1 = new QtyContents( "average1", "quantity1" );
		QtyContents content2 = new QtyContents( "average2", "quantity2" );
		FoodsCosmeticsMedicines medicine = new FoodsCosmeticsMedicines( "medicine" );
		medicine.getQtyContentsList().add( content1 );
		medicine.getQtyContentsList().add( content2 );

		inTransaction( em -> {
			em.persist( medicine );
		} );

		inTransaction( em -> {
			List<FoodsCosmeticsMedicines> loaded = em.createQuery( "FROM FoodsCosmeticsMedicines", FoodsCosmeticsMedicines.class ).getResultList();

			assertThat( loaded ).containsOnly( medicine );
			assertThat( loaded.get( 0 ).getQtyContentsList() ).containsOnly( content1, content2 );
		} );

		inTransaction( em -> {
			em.remove( em.find( FoodsCosmeticsMedicines.class, medicine.getName() ) );
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ FoodsCosmeticsMedicines.class };
	}

	@Entity(name = "FoodsCosmeticsMedicines")
	@Indexed
	@Table(name = "foodsCosmeticsMedicines")
	public static class FoodsCosmeticsMedicines implements Serializable {

		@Id
		private String name;

		@ElementCollection
		private List<QtyContents> qtyContentsList = new ArrayList<>();

		public FoodsCosmeticsMedicines() {
		}

		public FoodsCosmeticsMedicines(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public java.util.List<QtyContents> getQtyContentsList() {
			return qtyContentsList;
		}

		public void setQtyContentsList(java.util.List<QtyContents> qtyContentsList) {
			this.qtyContentsList = qtyContentsList;
		}

		@Override
		public String toString() {
			return "[" + name + " " + qtyContentsList + "]";
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
			FoodsCosmeticsMedicines other = (FoodsCosmeticsMedicines) obj;
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

	@Embeddable
	public static class QtyContents implements Serializable {

		private String avgMeasure;
		private String quantity;

		public QtyContents() {
		}

		public QtyContents(String average, String quantity) {
			this.avgMeasure = average;
			this.quantity = quantity;
		}

		public String getAvgMeasure() {
			return avgMeasure;
		}

		public void setAvgMeasure(String avgMeasure) {
			this.avgMeasure = avgMeasure;
		}

		public String getQuantity() {
			return quantity;
		}

		public void setQuantity(String quantity) {
			this.quantity = quantity;
		}

		@Override
		public String toString() {
			return "(" + avgMeasure + ", " + quantity + ")";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ( ( avgMeasure == null ) ? 0 : avgMeasure.hashCode() );
			result = prime * result + ( ( quantity == null ) ? 0 : quantity.hashCode() );
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
			QtyContents other = (QtyContents) obj;
			if ( avgMeasure == null ) {
				if ( other.avgMeasure != null ) {
					return false;
				}
			}
			else if ( !avgMeasure.equals( other.avgMeasure ) ) {
				return false;
			}
			if ( quantity == null ) {
				if ( other.quantity != null ) {
					return false;
				}
			}
			else if ( !quantity.equals( other.quantity ) ) {
				return false;
			}
			return true;
		}

	}
}
