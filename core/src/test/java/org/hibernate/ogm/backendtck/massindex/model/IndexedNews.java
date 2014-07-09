/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.massindex.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.OneToMany;

import org.hibernate.ogm.backendtck.id.NewsID;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
@Entity
@Indexed
public class IndexedNews {

	@DocumentId
	@EmbeddedId
	@FieldBridge(impl = NewsIdFieldBridge.class)
	private NewsID newsId;

	private String content;

	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumns({ @JoinColumn(name = "news_topic_fk", referencedColumnName = "newsid.title", nullable = false),
			@JoinColumn(name = "news_author_fk", referencedColumnName = "newsid.author", nullable = false) })
	private List<IndexedLabel> labels;

	public IndexedNews() {
	}

	public IndexedNews(NewsID newId, String content) {
		this.newsId = newId;
		this.content = content;
	}

	public NewsID getNewsId() {
		return newsId;
	}

	public void setNewsId(NewsID newId) {
		this.newsId = newId;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public List<IndexedLabel> getLabels() {
		return labels;
	}

	public void setLabels(List<IndexedLabel> labels) {
		this.labels = labels;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( content == null ) ? 0 : content.hashCode() );
		result = prime * result + ( ( newsId == null ) ? 0 : newsId.hashCode() );
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
		IndexedNews other = (IndexedNews) obj;
		if ( content == null ) {
			if ( other.content != null ) {
				return false;
			}
		}
		else if ( !content.equals( other.content ) ) {
			return false;
		}
		if ( newsId == null ) {
			if ( other.newsId != null ) {
				return false;
			}
		}
		else if ( !newsId.equals( other.newsId ) ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append( "IndexedNews [newsId=" );
		builder.append( newsId );
		builder.append( ", content=" );
		builder.append( content );
		builder.append( ", labels=" );
		builder.append( labels );
		builder.append( "]" );
		return builder.toString();
	}

}
