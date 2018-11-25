package de.axelfaust.graal.truffle.instruments.internal;

import java.net.URI;

import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.nodes.ExecutableNode;
import com.oracle.truffle.api.nodes.LanguageInfo;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;

import de.axelfaust.graal.truffle.instruments.api.PolyglotCallstackFrame;

/**
 *
 * @author Axel Faust
 */
class PolyglotCallstackFrameImpl implements PolyglotCallstackFrame
{

    protected final URI sourceURI;

    protected final String sourceName;

    protected final String languageId;

    protected final String languageName;

    protected final String scopeName;

    protected final int lineNumber;

    protected final SourceSection sourceSection;

    protected final CharSequence code;

    protected PolyglotCallstackFrameImpl(final EventContext context)
    {
        final com.oracle.truffle.api.source.SourceSection sourceSection = context.getInstrumentedSourceSection();
        this.sourceURI = sourceSection.getSource().getURI();
        this.sourceName = sourceSection.getSource().getName();
        this.lineNumber = sourceSection.getStartLine();
        this.code = sourceSection.getCharacters();

        // TODO We need a polyglot source section - how to convert though?
        this.sourceSection = sourceSection;

        Node currentNode = context.getInstrumentedNode();
        while (!(currentNode instanceof RootNode))
        {
            currentNode = currentNode.getParent();
        }

        if (currentNode instanceof ExecutableNode)
        {
            final LanguageInfo languageInfo = ((ExecutableNode) currentNode).getLanguageInfo();
            this.languageId = languageInfo.getId();
            this.languageName = languageInfo.getName();
        }
        else
        {
            this.languageId = null;
            this.languageName = null;
        }
        this.scopeName = currentNode instanceof RootNode ? ((RootNode) currentNode).getName() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI getSourceURI()
    {
        return this.sourceURI;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSourceName()
    {
        return this.sourceName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLanguageId()
    {
        return this.languageId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLanguageName()
    {
        return this.languageName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getScopeName()
    {
        return this.scopeName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CharSequence getCode()
    {
        return this.code;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLineNumber()
    {
        return this.lineNumber;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SourceSection getSourceSection()
    {
        return this.sourceSection;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        final URI sourceURI = this.getSourceURI();
        final String sourceName = this.getSourceName();
        final String languageId = this.getLanguageId();
        final String scopeName = this.getScopeName();
        result = prime * result + ((sourceURI == null) ? 0 : sourceURI.hashCode());
        result = prime * result + ((sourceName == null) ? 0 : sourceName.hashCode());
        result = prime * result + ((languageId == null) ? 0 : languageId.hashCode());
        result = prime * result + ((scopeName == null) ? 0 : scopeName.hashCode());
        result = prime * result + this.getLineNumber();
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (this.getClass() != obj.getClass())
        {
            return false;
        }
        final PolyglotCallstackFrame other = (PolyglotCallstackFrame) obj;
        if (this.getSourceURI() == null)
        {
            if (other.getSourceURI() != null)
            {
                return false;
            }
        }
        else if (!this.getSourceURI().equals(other.getSourceURI()))
        {
            return false;
        }
        if (this.getSourceName() == null)
        {
            if (other.getSourceName() != null)
            {
                return false;
            }
        }
        else if (!this.getSourceName().equals(other.getSourceName()))
        {
            return false;
        }
        if (this.getLanguageName() == null)
        {
            if (other.getLanguageName() != null)
            {
                return false;
            }
        }
        else if (!this.getLanguageName().equals(other.getLanguageName()))
        {
            return false;
        }
        if (this.getScopeName() == null)
        {
            if (other.getScopeName() != null)
            {
                return false;
            }
        }
        else if (!this.getScopeName().equals(other.getScopeName()))
        {
            return false;
        }
        if (this.getLineNumber() != other.getLineNumber())
        {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        final StringBuilder builder = new StringBuilder();
        builder.append("PolyglotCallstackFrame [");
        final URI sourceURI = this.getSourceURI();
        if (sourceURI != null)
        {
            builder.append("sourceURI=");
            builder.append(sourceURI);
            builder.append(", ");
        }
        final String sourceName = this.getSourceName();
        if (sourceName != null)
        {
            builder.append("sourceName=");
            builder.append(sourceName);
            builder.append(", ");
        }
        final String languageId = this.getLanguageId();
        if (languageId != null)
        {
            builder.append("languageId=");
            builder.append(languageId);
            builder.append(", ");
        }
        final String scopeName = this.getScopeName();
        if (scopeName != null)
        {
            builder.append("scopeName=");
            builder.append(scopeName);
            builder.append(", ");
        }
        final CharSequence code = this.getCode();
        if (code != null)
        {
            builder.append("code=");
            builder.append(code);
            builder.append(", ");
        }
        builder.append("lineNumber=");
        builder.append(this.getLineNumber());
        builder.append("]");
        return builder.toString();
    }
}
